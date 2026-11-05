using Budgetify.Api.Services;
using Xunit;

namespace Budgetify.Api.Tests;

public class PasswordServiceTests
{
    private readonly PasswordService _service = new();

    [Fact]
    public void Hash_IsNotThePlainTextPassword()
    {
        var hash = _service.Hash("Secret123");
        Assert.NotEqual("Secret123", hash);
        Assert.StartsWith("$2", hash); // BCrypt format
    }

    [Fact]
    public void Verify_AcceptsCorrectAndRejectsWrongPassword()
    {
        var hash = _service.Hash("Secret123");
        Assert.True(_service.Verify("Secret123", hash));
        Assert.False(_service.Verify("secret123", hash));
    }

    [Fact]
    public void Hash_IsSaltedSoTwoHashesDiffer()
    {
        Assert.NotEqual(_service.Hash("Secret123"), _service.Hash("Secret123"));
    }
}

public class BudgetRulesTests
{
    [Theory]
    [InlineData(70, 85, 100, 80)]
    [InlineData(90, 101, 100, 100)]
    [InlineData(10, 150, 100, 100)]
    public void CrossedThreshold_DetectsNewlyCrossedLines(int before, int after, int limit, int expected)
    {
        Assert.Equal(expected, BudgetRules.CrossedThreshold(before, after, limit));
    }

    [Theory]
    [InlineData(10, 20, 100)]
    [InlineData(85, 90, 100)]
    [InlineData(110, 120, 100)]
    [InlineData(0, 50, 0)]
    public void CrossedThreshold_ReturnsNullWhenNothingNew(int before, int after, int limit)
    {
        Assert.Null(BudgetRules.CrossedThreshold(before, after, limit));
    }
}
