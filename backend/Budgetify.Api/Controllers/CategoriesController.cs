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
[Route("api/categories")]
[Authorize]
public class CategoriesController : ControllerBase
{
    private readonly AppDbContext _db;
    private readonly ILogger<CategoriesController> _logger;

    public CategoriesController(AppDbContext db, ILogger<CategoriesController> logger)
    {
        _db = db;
        _logger = logger;
    }

    private int CurrentUserId => int.Parse(User.FindFirstValue(ClaimTypes.NameIdentifier)
        ?? User.FindFirstValue("sub")!);

    // GET /api/categories  (also seeds defaults for users who registered before categories existed)
    [HttpGet]
    public async Task<ActionResult<List<CategoryDto>>> GetAll()
    {
        await CategorySeeder.EnsureDefaultsAsync(_db, CurrentUserId);
        var list = await _db.Categories
            .Where(c => c.UserId == CurrentUserId)
            .OrderBy(c => c.Name)
            .ToListAsync();
        return Ok(list.Select(c => new CategoryDto(c.Id, c.Name, c.MonthlyLimit)).ToList());
    }

    // POST /api/categories
    [HttpPost]
    public async Task<ActionResult<CategoryDto>> Create(CategoryRequest request)
    {
        var name = request.Name?.Trim() ?? string.Empty;
        if (name.Length == 0 || name.Length > 30 || request.MonthlyLimit <= 0)
        {
            return BadRequest(new { message = "A name (max 30 characters) and a positive monthly limit are required." });
        }
        if (await _db.Categories.AnyAsync(c => c.UserId == CurrentUserId && c.Name == name))
        {
            return Conflict(new { message = "You already have a category with this name." });
        }

        var category = new Category { Name = name, MonthlyLimit = request.MonthlyLimit, UserId = CurrentUserId };
        _db.Categories.Add(category);
        await _db.SaveChangesAsync();
        _logger.LogInformation("User {UserId} created category {CategoryId}", CurrentUserId, category.Id);
        return Created($"/api/categories/{category.Id}", new CategoryDto(category.Id, category.Name, category.MonthlyLimit));
    }

    // PUT /api/categories/{id}
    [HttpPut("{id:int}")]
    public async Task<ActionResult<CategoryDto>> Update(int id, CategoryRequest request)
    {
        var category = await _db.Categories.FirstOrDefaultAsync(c => c.Id == id && c.UserId == CurrentUserId);
        if (category == null) return NotFound();
        if (request.MonthlyLimit <= 0) return BadRequest(new { message = "The monthly limit must be positive." });

        category.MonthlyLimit = request.MonthlyLimit;
        if (!string.IsNullOrWhiteSpace(request.Name) && request.Name.Trim().Length <= 30)
        {
            category.Name = request.Name.Trim();
        }
        await _db.SaveChangesAsync();
        _logger.LogInformation("User {UserId} updated category {CategoryId}", CurrentUserId, id);
        return Ok(new CategoryDto(category.Id, category.Name, category.MonthlyLimit));
    }
}
