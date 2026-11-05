namespace Budgetify.Api.Models;

public class Category
{
    public int Id { get; set; }
    public string Name { get; set; } = string.Empty;      // e.g. "Groceries", "Transport"
    public decimal MonthlyLimit { get; set; }
    public int UserId { get; set; }
    public User? User { get; set; }
}
