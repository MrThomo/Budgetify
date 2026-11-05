namespace Budgetify.Api.Models;

public class Transaction
{
    public int Id { get; set; }

    // Client-generated UUID created on the phone while the record only exists offline in RoomDB.
    // It makes sync idempotent: re-sending the same offline expense never creates a duplicate.
    public string? LocalId { get; set; }

    public decimal Amount { get; set; }
    public string Note { get; set; } = string.Empty;
    public DateTime TransactionDate { get; set; }

    // Download URL of the receipt photo stored in Firebase Storage (blob storage).
    public string? ReceiptUrl { get; set; }

    public int CategoryId { get; set; }
    public Category? Category { get; set; }

    public int UserId { get; set; }
    public User? User { get; set; }

    public DateTime CreatedAt { get; set; } = DateTime.UtcNow;
}
