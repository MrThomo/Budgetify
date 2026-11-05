namespace Budgetify.Api.Services;

/// <summary>
/// Budget threshold rule shared (in spirit) with the Android app's BudgetMath.crossedThreshold,
/// so the push alert the server sends always agrees with the local alert on the phone.
/// </summary>
public static class BudgetRules
{
    /// <summary>Returns 100 (limit exceeded), 80 (warning line crossed) or null (nothing newly crossed).</summary>
    public static int? CrossedThreshold(decimal before, decimal after, decimal limit)
    {
        if (limit <= 0m) return null;
        if (before <= limit && after > limit) return 100;
        if (before < limit * 0.8m && after >= limit * 0.8m) return 80;
        return null;
    }
}
