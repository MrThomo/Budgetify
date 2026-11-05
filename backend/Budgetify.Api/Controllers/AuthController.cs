using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Budgetify.Api.Data;
using Budgetify.Api.DTOs;
using Budgetify.Api.Models;
using Budgetify.Api.Services;

namespace Budgetify.Api.Controllers;

[ApiController]
[Route("api/auth")]
public class AuthController : ControllerBase
{
    private readonly AppDbContext _db;
    private readonly PasswordService _passwordService;
    private readonly TokenService _tokenService;
    private readonly IGoogleTokenValidator _google;
    private readonly ILogger<AuthController> _logger;

    public AuthController(AppDbContext db, PasswordService passwordService, TokenService tokenService,
        IGoogleTokenValidator google, ILogger<AuthController> logger)
    {
        _db = db;
        _passwordService = passwordService;
        _tokenService = tokenService;
        _google = google;
        _logger = logger;
    }

    private static string NormaliseEmail(string email) => email.Trim().ToLowerInvariant();

    // POST /api/auth/register
    [HttpPost("register")]
    public async Task<ActionResult<AuthResponse>> Register(RegisterRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.FullName) ||
            string.IsNullOrWhiteSpace(request.Email) || !request.Email.Contains('@') ||
            string.IsNullOrEmpty(request.Password) || request.Password.Length < 8)
        {
            return BadRequest(new { message = "Name, a valid email and a password of at least 8 characters are required." });
        }

        var email = NormaliseEmail(request.Email);
        if (await _db.Users.AnyAsync(u => u.Email == email))
        {
            _logger.LogWarning("Registration attempt with duplicate email");
            return Conflict(new { message = "An account with this email already exists." });
        }

        var user = new User
        {
            FullName = request.FullName.Trim(),
            Email = email,
            PasswordHash = _passwordService.Hash(request.Password)   // BCrypt - plain text is never stored
        };

        _db.Users.Add(user);
        await _db.SaveChangesAsync();
        await CategorySeeder.EnsureDefaultsAsync(_db, user.Id);

        _logger.LogInformation("New user registered: {UserId}", user.Id);

        var token = _tokenService.GenerateToken(user);
        return Created($"/api/settings", new AuthResponse(user.Id, user.FullName, user.Email, token));
    }

    // POST /api/auth/login
    [HttpPost("login")]
    public async Task<ActionResult<AuthResponse>> Login(LoginRequest request)
    {
        var email = NormaliseEmail(request.Email ?? string.Empty);
        var user = await _db.Users.FirstOrDefaultAsync(u => u.Email == email);

        if (user == null || string.IsNullOrEmpty(request.Password) ||
            !_passwordService.Verify(request.Password, user.PasswordHash))
        {
            _logger.LogWarning("Failed login attempt");
            return Unauthorized(new { message = "Invalid email or password." });
        }

        var token = _tokenService.GenerateToken(user);
        _logger.LogInformation("User {UserId} logged in", user.Id);
        return Ok(new AuthResponse(user.Id, user.FullName, user.Email, token));
    }

    // POST /api/auth/google  (single sign-on)
    [HttpPost("google")]
    public async Task<ActionResult<AuthResponse>> GoogleLogin(GoogleLoginRequest request)
    {
        var identity = string.IsNullOrWhiteSpace(request.IdToken) ? null : await _google.ValidateAsync(request.IdToken);
        if (identity == null)
        {
            return Unauthorized(new { message = "Google sign-in could not be verified." });
        }

        var email = NormaliseEmail(identity.Email);
        var user = await _db.Users.FirstOrDefaultAsync(u => u.Email == email);
        if (user == null)
        {
            user = new User
            {
                FullName = string.IsNullOrWhiteSpace(identity.Name) ? email : identity.Name!,
                Email = email,
                // Random hash: SSO accounts cannot be logged into with a password.
                PasswordHash = _passwordService.Hash(Guid.NewGuid().ToString("N"))
            };
            _db.Users.Add(user);
            await _db.SaveChangesAsync();
            await CategorySeeder.EnsureDefaultsAsync(_db, user.Id);
            _logger.LogInformation("New user created via Google SSO: {UserId}", user.Id);
        }
        else
        {
            _logger.LogInformation("User {UserId} signed in via Google SSO", user.Id);
        }

        return Ok(new AuthResponse(user.Id, user.FullName, user.Email, _tokenService.GenerateToken(user)));
    }
}
