using System.Net;
using System.Net.Http.Headers;
using System.Net.Http.Json;
using Budgetify.Api.DTOs;
using Microsoft.AspNetCore.Mvc.Testing;
using Xunit;

namespace Budgetify.Api.Tests;

/// <summary>
/// End-to-end tests of the REST API running in-memory against a throw-away SQLite file:
/// register, login, protected settings, categories and the offline-sync endpoint.
/// </summary>
public class ApiIntegrationTests : IClassFixture<WebApplicationFactory<Program>>
{
    private readonly WebApplicationFactory<Program> _factory;

    public ApiIntegrationTests(WebApplicationFactory<Program> factory)
    {
        var dbPath = Path.Combine(Path.GetTempPath(), $"budgetify-test-{Guid.NewGuid():N}.db");
        _factory = factory.WithWebHostBuilder(b =>
            b.UseSetting("ConnectionStrings:DefaultConnection", $"Data Source={dbPath}"));
    }

    private static string NewEmail() => $"user-{Guid.NewGuid():N}@example.com";

    private async Task<(HttpClient Client, AuthResponse Auth)> RegisterAsync(string? email = null)
    {
        var client = _factory.CreateClient();
        var response = await client.PostAsJsonAsync("/api/auth/register",
            new RegisterRequest("Test User", email ?? NewEmail(), "Secret123"));
        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
        var auth = (await response.Content.ReadFromJsonAsync<AuthResponse>())!;
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", auth.Token);
        return (client, auth);
    }

    [Fact]
    public async Task Register_ReturnsToken_AndDuplicateEmailIsRejected()
    {
        var email = NewEmail();
        var (_, auth) = await RegisterAsync(email);
        Assert.False(string.IsNullOrEmpty(auth.Token));

        var again = await _factory.CreateClient().PostAsJsonAsync("/api/auth/register",
            new RegisterRequest("Other", email, "Secret123"));
        Assert.Equal(HttpStatusCode.Conflict, again.StatusCode);
    }

    [Fact]
    public async Task Register_RejectsWeakOrMissingInput()
    {
        var response = await _factory.CreateClient().PostAsJsonAsync("/api/auth/register",
            new RegisterRequest("", "not-an-email", "short"));
        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task Login_WorksWithCorrectPassword_AndFailsWithWrongOne()
    {
        var email = NewEmail();
        await RegisterAsync(email);
        var client = _factory.CreateClient();

        var ok = await client.PostAsJsonAsync("/api/auth/login", new LoginRequest(email, "Secret123"));
        Assert.Equal(HttpStatusCode.OK, ok.StatusCode);

        var bad = await client.PostAsJsonAsync("/api/auth/login", new LoginRequest(email, "WrongPass1"));
        Assert.Equal(HttpStatusCode.Unauthorized, bad.StatusCode);
    }

    [Fact]
    public async Task Settings_RequireAuthentication()
    {
        var response = await _factory.CreateClient().GetAsync("/api/settings");
        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task Settings_CanBeUpdatedAndReadBack()
    {
        var (client, _) = await RegisterAsync();

        var put = await client.PutAsJsonAsync("/api/settings",
            new UpdateSettingsRequest("USD", "dark", "zu", false));
        Assert.Equal(HttpStatusCode.OK, put.StatusCode);

        var settings = await client.GetFromJsonAsync<SettingsResponse>("/api/settings");
        Assert.Equal("USD", settings!.Currency);
        Assert.Equal("dark", settings.Theme);
        Assert.Equal("zu", settings.Language);
        Assert.False(settings.NotificationsEnabled);
    }

    [Fact]
    public async Task NewUser_GetsDefaultCategories()
    {
        var (client, _) = await RegisterAsync();
        var categories = await client.GetFromJsonAsync<List<CategoryDto>>("/api/categories");
        Assert.NotNull(categories);
        Assert.Contains(categories!, c => c.Name == "Groceries");
        Assert.True(categories!.Count >= 6);
    }

    [Fact]
    public async Task Sync_StoresOfflineExpense_AndIsIdempotent()
    {
        var (client, _) = await RegisterAsync();
        var categories = (await client.GetFromJsonAsync<List<CategoryDto>>("/api/categories"))!;
        var localId = Guid.NewGuid().ToString();
        var pending = new PendingTransactionDto(localId, 99.50m, categories[0].Id,
            new DateTime(2026, 11, 4), "Lunch", null);
        var request = new SyncRequest(null, new List<PendingTransactionDto> { pending });

        var first = await (await client.PostAsJsonAsync("/api/transactions/sync", request))
            .Content.ReadFromJsonAsync<SyncResponse>();
        Assert.Contains(localId, first!.SyncedLocalIds);
        Assert.Single(first.ServerUpdates);

        // Retrying the exact same request must not duplicate the expense.
        await client.PostAsJsonAsync("/api/transactions/sync", request);
        var all = await client.GetFromJsonAsync<List<TransactionDto>>("/api/transactions");
        Assert.Single(all!);
        Assert.Equal(99.50m, all![0].Amount);
    }

    [Fact]
    public async Task Sync_RejectsUnknownCategoryAndNegativeAmount()
    {
        var (client, _) = await RegisterAsync();
        var bad1 = new PendingTransactionDto(Guid.NewGuid().ToString(), 10m, 999999, DateTime.UtcNow, null, null);
        var bad2 = new PendingTransactionDto(Guid.NewGuid().ToString(), -5m, 1, DateTime.UtcNow, null, null);

        var response = await (await client.PostAsJsonAsync("/api/transactions/sync",
                new SyncRequest(null, new List<PendingTransactionDto> { bad1, bad2 })))
            .Content.ReadFromJsonAsync<SyncResponse>();

        Assert.Equal(2, response!.RejectedLocalIds.Count);
        Assert.Empty(response.SyncedLocalIds);
    }

    [Fact]
    public async Task TestNotification_Returns503WhenPushNotConfigured()
    {
        var (client, _) = await RegisterAsync();
        var response = await client.PostAsync("/api/notifications/test", null);
        Assert.Equal(HttpStatusCode.ServiceUnavailable, response.StatusCode);
    }
}
