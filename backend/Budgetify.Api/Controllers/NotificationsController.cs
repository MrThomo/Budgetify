using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Budgetify.Api.Data;
using Budgetify.Api.DTOs;
using Budgetify.Api.Services;

namespace Budgetify.Api.Controllers;

[ApiController]
[Route("api/notifications")]
[Authorize]
public class NotificationsController : ControllerBase
{
    private readonly AppDbContext _db;
    private readonly IPushService _push;
    private readonly ILogger<NotificationsController> _logger;

    public NotificationsController(AppDbContext db, IPushService push, ILogger<NotificationsController> logger)
    {
        _db = db;
        _push = push;
        _logger = logger;
    }

    private int CurrentUserId => int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)
        ?? User.FindFirstValue("sub")!);

    // POST /api/notifications/token - the phone registers its FCM token
    [HttpPost("token")]
    public async Task<IActionResult> RegisterToken(DeviceTokenRequest request)
    {
        if (string.IsNullOrWhiteSpace(request.Token) || request.Token.Length > 4096)
        {
            return BadRequest(new { message = "A device token is required." });
        }
        var user = await _db.Users.FindAsync(CurrentUserId);
        if (user == null) return NotFound();

        user.FcmToken = request.Token;
        await _db.SaveChangesAsync();
        _logger.LogInformation("User {UserId} registered an FCM token", user.Id);
        return Ok(new { registered = true });
    }

    // POST /api/notifications/test - pushes a test message to the caller's phone
    [HttpPost("test")]
    public async Task<IActionResult> SendTest()
    {
        if (!_push.IsConfigured)
        {
            return StatusCode(503, new { message = "Push notifications are not configured on the server." });
        }
        var user = await _db.Users.FindAsync(CurrentUserId);
        if (user == null) return NotFound();
        if (string.IsNullOrEmpty(user.FcmToken))
        {
            return BadRequest(new { message = "No device is registered for this user yet." });
        }

        var sent = await _push.SendAsync(user.FcmToken, new Dictionary<string, string> { ["type"] = "test" });
        return sent ? Ok(new { sent = true }) : StatusCode(502, new { message = "Push delivery failed." });
    }
}
