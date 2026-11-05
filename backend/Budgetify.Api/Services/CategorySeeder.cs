using Microsoft.EntityFrameworkCore;
using Budgetify.Api.Data;
using Budgetify.Api.Models;

namespace Budgetify.Api.Services;

/// <summary>Gives every user a sensible set of starter categories (ZAR monthly limits).</summary>
public static class CategorySeeder
{
    private static readonly (string Name, decimal Limit)[] Defaults =
    {
        ("Groceries", 2000m),
        ("Transport", 800m),
        ("Eating Out", 600m),
        ("Utilities", 1200m),
        ("Entertainment", 500m),
        ("Other", 400m)
    };

    public static async Task EnsureDefaultsAsync(AppDbContext db, int userId)
    {
        if (await db.Categories.AnyAsync(c => c.UserId == userId)) return;

        foreach (var (name, limit) in Defaults)
        {
            db.Categories.Add(new Category { Name = name, MonthlyLimit = limit, UserId = userId });
        }
        await db.SaveChangesAsync();
    }
}
