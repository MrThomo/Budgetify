using Google.Apis.Auth;

namespace Budgetify.Api.Services;

public record GoogleIdentity(string Email, string? Name);

/// <summary>Verifies a Google ID token (single sign-on) and returns the account it belongs to.</summary>
public interface IGoogleTokenValidator
{
    Task<GoogleIdentity?> ValidateAsync(string idToken);
}

public class GoogleTokenValidator : IGoogleTokenValidator
{
    private readonly IConfiguration _config;
    private readonly ILogger<GoogleTokenValidator> _logger;

    public GoogleTokenValidator(IConfiguration config, ILogger<GoogleTokenValidator> logger)
    {
        _config = config;
        _logger = logger;
    }

    public async Task<GoogleIdentity?> ValidateAsync(string idToken)
    {
        var clientId = _config["Google:WebClientId"];
        if (string.IsNullOrWhiteSpace(clientId))
        {
            _logger.LogWarning("Google SSO attempted but Google:WebClientId is not configured");
            return null;
        }

        try
        {
            var payload = await GoogleJsonWebSignature.ValidateAsync(idToken,
                new GoogleJsonWebSignature.ValidationSettings { Audience = new[] { clientId } });

            if (string.IsNullOrWhiteSpace(payload.Email) || !payload.EmailVerified) return null;
            return new GoogleIdentity(payload.Email, payload.Name);
        }
        catch (InvalidJwtException ex)
        {
            _logger.LogWarning("Rejected invalid Google ID token: {Message}", ex.Message);
            return null;
        }
    }
}
