using Microsoft.EntityFrameworkCore;
using Budgetify.Api.Data;
using Budgetify.Api.Models;

namespace Budgetify.Api.Services;

/// <summary>
/// After new expenses arrive, checks whether any category crossed 80% / 100% of its monthly
/// limit and, if so, pushes a real-time alert to the user's phone through Firebase Cloud Messaging.
/// The phone turns the data message into a notification in the user's chosen language.
/// </summary>
public class BudgetAlertService
{
    private readonly AppDbContext _db;
    private readonly IPushService _push;
    private readonly ILogger<BudgetAlertService> _logger;

    public BudgetAlertService(AppDbContext db, IPushService push, ILogger<BudgetAlertService> logger)
    {
        _db = db;
        _push = push;
        _logger = logger;
    }

    public async Task EvaluateAsync(int userId, IReadOnlyCollection<Transaction> added)
    {
        if (added.Count == 0 || !_push.IsConfigured) return;

        var user = await _db.Users.FindAsync(userId);
        if (user == null || !user.NotificationsEnabled || string.IsNullOrEmpty(user.FcmToken)) return;

        var groups = added.GroupBy(t => new { t.CategoryId, t.TransactionDate.Year, t.TransactionDate.Month });
        foreach (var group in groups)
        {
            var category = await _db.Categories.FindAsync(group.Key.CategoryId);
            if (category == null) continue;

            var monthStart = new DateTime(group.Key.Year, group.Key.Month, 1);
            var monthEnd = monthStart.AddMonths(1);

            // Sum in memory: SQLite cannot aggregate decimal columns in SQL.
            var amounts = await _db.Transactions
                .Where(t => t.UserId == userId && t.CategoryId == category.Id
                            && t.TransactionDate >= monthStart && t.TransactionDate < monthEnd)
                .Select(t => t.Amount)
                .ToListAsync();

            var after = amounts.Sum();
            var before = after - group.Sum(t => t.Amount);
            var crossed = BudgetRules.CrossedThreshold(before, after, category.MonthlyLimit);
            if (crossed == null) continue;

            _logger.LogInformation("Budget alert for user {UserId}, category {Category}: {Percent}%",
                userId, category.Name, crossed);

            await _push.SendAsync(user.FcmToken!, new Dictionary<string, string>
            {
                ["type"] = "budget_alert",
                ["categoryId"] = category.Id.ToString(),
                ["categoryName"] = category.Name,
                ["percent"] = crossed.Value.ToString()
            });
        }
    }
}
