using FirebaseAdmin;
using Google.Apis.Auth.OAuth2;

namespace Budgetify.Api.Services;

/// <summary>Sends push notifications to a phone's FCM token.</summary>
public interface IPushService
{
    bool IsConfigured { get; }
    Task<bool> SendAsync(string fcmToken, IDictionary<string, string> data);
}

/// <summary>
/// Firebase Cloud Messaging implementation. It needs a Firebase service-account key, supplied
/// either as JSON in configuration "Firebase:CredentialsJson" (env var Firebase__CredentialsJson)
/// or as a file path in "Firebase:CredentialsPath". Without it the API still runs; pushes are
/// simply skipped and IsConfigured is false.
/// </summary>
public class FcmPushService : IPushService
{
    private readonly ILogger<FcmPushService> _logger;

    public bool IsConfigured { get; }

    public FcmPushService(IConfiguration config, ILogger<FcmPushService> logger)
    {
        _logger = logger;
        try
        {
            if (FirebaseApp.DefaultInstance == null)
            {
                var json = config["Firebase:CredentialsJson"];
                var path = config["Firebase:CredentialsPath"];
                GoogleCredential? credential = null;

                if (!string.IsNullOrWhiteSpace(json)) credential = GoogleCredential.FromJson(json);
                else if (!string.IsNullOrWhiteSpace(path) && File.Exists(path)) credential = GoogleCredential.FromFile(path);

                if (credential != null) FirebaseApp.Create(new AppOptions { Credential = credential });
            }
            IsConfigured = FirebaseApp.DefaultInstance != null;
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Firebase push is not configured correctly");
            IsConfigured = false;
        }

        _logger.LogInformation("Push notifications configured: {Configured}", IsConfigured);
    }

    public async Task<bool> SendAsync(string fcmToken, IDictionary<string, string> data)
    {
        if (!IsConfigured) return false;
        try
        {
            var message = new FirebaseAdmin.Messaging.Message
            {
                Token = fcmToken,
                Data = new Dictionary<string, string>(data),
                Android = new FirebaseAdmin.Messaging.AndroidConfig
                {
                    Priority = FirebaseAdmin.Messaging.Priority.High
                }
            };
            var id = await FirebaseAdmin.Messaging.FirebaseMessaging.DefaultInstance.SendAsync(message);
            _logger.LogInformation("FCM message sent: {MessageId}", id);
            return true;
        }
        catch (Exception ex)
        {
            _logger.LogWarning(ex, "FCM send failed");
            return false;
        }
    }
}
