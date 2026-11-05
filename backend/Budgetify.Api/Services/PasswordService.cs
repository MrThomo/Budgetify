namespace Budgetify.Api.Services;

/// <summary>
/// Wraps BCrypt so password hashing/verification logic lives in one place.
/// BCrypt automatically generates and stores a per-password salt inside the
/// hash string itself, which is why we don't need a separate Salt column.
/// </summary>
public class PasswordService
{
    public string Hash(string plainTextPassword)
    {
        return BCrypt.Net.BCrypt.HashPassword(plainTextPassword, workFactor: 12);
    }

    public bool Verify(string plainTextPassword, string storedHash)
    {
        return BCrypt.Net.BCrypt.Verify(plainTextPassword, storedHash);
    }
}
