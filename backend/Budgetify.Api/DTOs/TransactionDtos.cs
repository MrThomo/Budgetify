namespace Budgetify.Api.DTOs;

public record CategoryDto(int Id, string Name, decimal MonthlyLimit);

public record CategoryRequest(string Name, decimal MonthlyLimit);

public record TransactionDto(
    int Id,
    string? LocalId,
    decimal Amount,
    int CategoryId,
    DateTime TransactionDate,
    string? Note,
    string? ReceiptUrl);

public record CreateTransactionRequest(
    decimal Amount,
    int CategoryId,
    DateTime TransactionDate,
    string? Note,
    string? ReceiptUrl);

public record PendingTransactionDto(
    string LocalId,
    decimal Amount,
    int CategoryId,
    DateTime TransactionDate,
    string? Note,
    string? ReceiptUrl);

public record SyncRequest(DateTime? LastSyncTimestamp, List<PendingTransactionDto>? PendingTransactions);

public record SyncResponse(
    DateTime SyncTimestamp,
    List<string> SyncedLocalIds,
    List<string> RejectedLocalIds,
    List<TransactionDto> ServerUpdates);
