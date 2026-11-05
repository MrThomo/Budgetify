using System.Security.Claims;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;
using Budgetify.Api.Data;
using Budgetify.Api.DTOs;
using Budgetify.Api.Models;
using Budgetify.Api.Services;

namespace Budgetify.Api.Controllers;

[ApiController]
[Route("api/transactions")]
[Authorize]
public class TransactionsController : ControllerBase
{
    private readonly AppDbContext _db;
    private readonly BudgetAlertService _alerts;
    private readonly ILogger<TransactionsController> _logger;

    public TransactionsController(AppDbContext db, BudgetAlertService alerts, ILogger<TransactionsController> logger)
    {
        _db = db;
        _alerts = alerts;
        _logger = logger;
    }

    private int CurrentUserId => int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)
        ?? User.FindFirstValue("sub")!);

    private static TransactionDto ToDto(Transaction t) =>
        new(t.Id, t.LocalId, t.Amount, t.CategoryId, t.TransactionDate, t.Note, t.ReceiptUrl);

    // GET /api/transactions
    [HttpGet]
    public async Task<ActionResult<List<TransactionDto>>> GetAll()
    {
        var transactions = await _db.Transactions
            .Where(t => t.UserId == CurrentUserId)
            .OrderByDescending(t => t.TransactionDate)
            .ToListAsync();
        return Ok(transactions.Select(ToDto).ToList());
    }

    // POST /api/transactions
    [HttpPost]
    public async Task<ActionResult<TransactionDto>> Create(CreateTransactionRequest request)
    {
        if (request.Amount <= 0) return BadRequest(new { message = "Amount must be greater than zero." });
        var ownsCategory = await _db.Categories.AnyAsync(c => c.Id == request.CategoryId && c.UserId == CurrentUserId);
        if (!ownsCategory) return BadRequest(new { message = "Unknown category." });

        var entity = new Transaction
        {
            Amount = request.Amount,
            CategoryId = request.CategoryId,
            TransactionDate = request.TransactionDate,
            Note = request.Note ?? string.Empty,
            ReceiptUrl = request.ReceiptUrl,
            UserId = CurrentUserId
        };
        _db.Transactions.Add(entity);
        await _db.SaveChangesAsync();
        await _alerts.EvaluateAsync(CurrentUserId, new[] { entity });

        return Created($"/api/transactions/{entity.Id}", ToDto(entity));
    }

    // POST /api/transactions/sync
    // Accepts expenses queued offline in the Android app's RoomDB and returns anything newer the
    // server has, so one call reconciles both directions. Safe to retry: duplicates (same LocalId)
    // are acknowledged but not inserted twice.
    [HttpPost("sync")]
    public async Task<ActionResult<SyncResponse>> Sync(SyncRequest request)
    {
        var userId = CurrentUserId;
        var syncStarted = DateTime.UtcNow;

        var validCategoryIds = (await _db.Categories.Where(c => c.UserId == userId).Select(c => c.Id).ToListAsync())
            .ToHashSet();
        var knownLocalIds = (await _db.Transactions.Where(t => t.UserId == userId && t.LocalId != null)
                .Select(t => t.LocalId).ToListAsync())
            .OfType<string>().ToHashSet();

        var synced = new List<string>();
        var rejected = new List<string>();
        var added = new List<Transaction>();

        foreach (var pending in request.PendingTransactions ?? new List<PendingTransactionDto>())
        {
            if (string.IsNullOrWhiteSpace(pending.LocalId) || pending.Amount <= 0 ||
                !validCategoryIds.Contains(pending.CategoryId))
            {
                rejected.Add(pending.LocalId ?? string.Empty);
                continue;
            }
            if (knownLocalIds.Contains(pending.LocalId))
            {
                synced.Add(pending.LocalId); // already stored by an earlier attempt
                continue;
            }

            var entity = new Transaction
            {
                LocalId = pending.LocalId,
                Amount = pending.Amount,
                CategoryId = pending.CategoryId,
                TransactionDate = pending.TransactionDate,
                Note = pending.Note ?? string.Empty,
                ReceiptUrl = pending.ReceiptUrl,
                UserId = userId
            };
            _db.Transactions.Add(entity);
            added.Add(entity);
            knownLocalIds.Add(pending.LocalId);
            synced.Add(pending.LocalId);
        }

        await _db.SaveChangesAsync();
        await _alerts.EvaluateAsync(userId, added);

        var since = request.LastSyncTimestamp ?? DateTime.MinValue;
        var updates = await _db.Transactions
            .Where(t => t.UserId == userId && t.CreatedAt > since)
            .ToListAsync();

        _logger.LogInformation("Synced {Count} offline transactions for user {UserId} ({Rejected} rejected, {Pulled} returned)",
            added.Count, userId, rejected.Count, updates.Count);

        return Ok(new SyncResponse(syncStarted, synced, rejected, updates.Select(ToDto).ToList()));
    }
}
