using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Budgetify.Api.Data;
using Budgetify.Api.DTOs;

namespace Budgetify.Api.Controllers;

[ApiController]
[Route("api/settings")]
[Authorize] // requires a valid JWT from /api/auth/login or /api/auth/register
public class SettingsController : ControllerBase
{
    private readonly AppDbContext _db;
    private readonly ILogger<SettingsController> _logger;

    public SettingsController(AppDbContext db, ILogger<SettingsController> logger)
    {
        _db = db;
        _logger = logger;
    }

    private int CurrentUserId => int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)
        ?? User.FindFirstValue("sub")!);

    // GET /api/settings
    [HttpGet]
    public async Task<ActionResult<SettingsResponse>> Get()
    {
        var user = await _db.Users.FindAsync(CurrentUserId);
        if (user == null) return NotFound();

        return Ok(new SettingsResponse(user.Currency, user.Theme, user.Language, user.NotificationsEnabled));
    }

    // PUT /api/settings
    [HttpPut]
    public async Task<ActionResult<SettingsResponse>> Update(UpdateSettingsRequest request)
    {
        var user = await _db.Users.FindAsync(CurrentUserId);
        if (user == null) return NotFound();

        if (request.Currency != null) user.Currency = request.Currency;
        if (request.Theme != null) user.Theme = request.Theme;
        if (request.Language != null) user.Language = request.Language;
        if (request.NotificationsEnabled.HasValue) user.NotificationsEnabled = request.NotificationsEnabled.Value;

        await _db.SaveChangesAsync();
        _logger.LogInformation("User {UserId} updated settings", CurrentUserId);

        return Ok(new SettingsResponse(user.Currency, user.Theme, user.Language, user.NotificationsEnabled));
    }
}
