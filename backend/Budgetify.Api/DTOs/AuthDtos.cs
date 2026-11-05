namespace Budgetify.Api.DTOs;

public record RegisterRequest(string FullName, string Email, string Password);

public record LoginRequest(string Email, string Password);

public record GoogleLoginRequest(string IdToken);

public record AuthResponse(int UserId, string FullName, string Email, string Token);

public record UpdateSettingsRequest(string? Currency, string? Theme, string? Language, bool? NotificationsEnabled);

public record SettingsResponse(string Currency, string Theme, string Language, bool NotificationsEnabled);

public record DeviceTokenRequest(string Token);
