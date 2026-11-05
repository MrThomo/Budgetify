using System.Text;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.IdentityModel.Tokens;
using Budgetify.Api.Data;
using Budgetify.Api.Services;

var builder = WebApplication.CreateBuilder(args);

// --- Logging ---
builder.Logging.ClearProviders();
builder.Logging.AddConsole();

// --- Database ---
// SQLite by default (local dev, CI, simple hosting). If the connection string looks like a
// SQL Server / Azure SQL string ("Server=...") the SQL Server provider is used instead.
// The options lambda resolves IConfiguration lazily so tests and hosts can override it.
builder.Services.AddDbContext<AppDbContext>((sp, options) =>
{
    var config = sp.GetRequiredService<IConfiguration>();
    var connection = config.GetConnectionString("DefaultConnection") ?? "Data Source=budgetify.db";
    if (connection.Contains("Server=", StringComparison.OrdinalIgnoreCase))
        options.UseSqlServer(connection);
    else
        options.UseSqlite(connection);
});

// --- App services ---
builder.Services.AddScoped<PasswordService>();
builder.Services.AddScoped<TokenService>();
builder.Services.AddScoped<BudgetAlertService>();
builder.Services.AddSingleton<IPushService, FcmPushService>();
builder.Services.AddSingleton<IGoogleTokenValidator, GoogleTokenValidator>();

// --- JWT authentication ---
var jwtKey = builder.Configuration["Jwt:Key"] ?? "DEV_ONLY_CHANGE_ME_BUDGETIFY_SECRET_KEY_2026";
builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidateAudience = true,
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            ValidIssuer = builder.Configuration["Jwt:Issuer"] ?? "Budgetify.Api",
            ValidAudience = builder.Configuration["Jwt:Audience"] ?? "Budgetify.App",
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwtKey))
        };
    });
builder.Services.AddAuthorization();

builder.Services.AddControllers();
builder.Services.AddEndpointsApiExplorer();
builder.Services.AddSwaggerGen();

builder.Services.AddCors(options =>
{
    options.AddDefaultPolicy(policy => policy.AllowAnyOrigin().AllowAnyMethod().AllowAnyHeader());
});

var app = builder.Build();

// Create the database tables on first start (fine for this project; production would use EF migrations).
using (var scope = app.Services.CreateScope())
{
    var db = scope.ServiceProvider.GetRequiredService<AppDbContext>();
    db.Database.EnsureCreated();
}

// Swagger UI is on in Development, or anywhere when Swagger:Enabled=true (handy for the demo video).
if (app.Environment.IsDevelopment() || app.Configuration.GetValue<bool>("Swagger:Enabled"))
{
    app.UseSwagger();
    app.UseSwaggerUI();
}

app.UseCors();
app.UseAuthentication();
app.UseAuthorization();
app.MapControllers();

app.MapGet("/", () => Results.Ok(new { status = "Budgetify API running" }));

app.Run();

// Exposed for WebApplicationFactory-based integration tests.
public partial class Program { }
