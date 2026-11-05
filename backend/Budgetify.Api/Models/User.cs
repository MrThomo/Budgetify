namespace Budgetify.Api.Models;

/// <summary>
/// A registered Budgetify user. Passwords are never stored in plain text - see
/// Services/PasswordService.cs, which hashes and salts every password with BCrypt.
/// Users who sign in with Google (SSO) get a random, unusable password hash.
/// </summary>
public class User
{
    public int Id { get; set; }
    public string FullName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;

    // BCrypt hash (includes its own random salt), never the raw password.
    public string PasswordHash { get; set; } = string.Empty;

    public string Currency { get; set; } = "ZAR";
    public string Theme { get; set; } = "light";       // "light" | "dark"
    public string Language { get; set; } = "en";        // "en" | "zu" | "st"
    public bool NotificationsEnabled { get; set; } = true;

    // Firebase Cloud Messaging registration token of the user's phone (for push notifications).
    public string? FcmToken { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;

    public List<Transaction> Transactions { get; set; } = new();
}
