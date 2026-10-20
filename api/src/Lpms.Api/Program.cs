using System.Security.Claims;
using System.Text;
using System.Threading.RateLimiting;
using Lpms.Api.Data;
using Lpms.Api.Options;
using Lpms.Api.Services;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.Cors.Infrastructure;
using Microsoft.AspNetCore.HttpOverrides;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;
using Microsoft.OpenApi.Models;

var builder = WebApplication.CreateBuilder(args);
var services = builder.Services;

// ---- Configuration (read lazily through IOptions so tests and App Service settings can override it) ----
services.AddOptions<JwtOptions>()
    .Bind(builder.Configuration.GetSection(JwtOptions.SectionName))
    .Validate(o => !string.IsNullOrWhiteSpace(o.Key) && o.Key.Length >= 32,
        "Jwt:Key must be set to a random secret of at least 32 characters (use the Jwt__Key setting).")
    .ValidateOnStart();

// ---- Database: Azure SQL / SQL Server through Entity Framework Core ----
services.AddDbContext<LpmsDbContext>((serviceProvider, options) =>
{
    var connectionString = serviceProvider.GetRequiredService<IConfiguration>().GetConnectionString("LpmsDb");
    options.UseSqlServer(connectionString, sql =>
    {
        // Azure SQL serverless can take a while to wake up, so retry transient failures.
        sql.EnableRetryOnFailure(maxRetryCount: 3);
        sql.CommandTimeout(60);
    });
});

services.AddSingleton<IClock, SastClock>();
services.AddSingleton<ITokenService, TokenService>();

// ---- Authentication: JWT bearer tokens issued by POST /api/auth/login ----
services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme).AddJwtBearer();
services.AddOptions<JwtBearerOptions>(JwtBearerDefaults.AuthenticationScheme)
    .Configure<IOptions<JwtOptions>>((bearer, jwtAccessor) =>
    {
        var jwt = jwtAccessor.Value;
        bearer.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidIssuer = jwt.Issuer,
            ValidateAudience = true,
            ValidAudience = jwt.Audience,
            ValidateIssuerSigningKey = true,
            IssuerSigningKey = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(jwt.Key)),
            ValidateLifetime = true,
            ClockSkew = TimeSpan.FromMinutes(1),
            NameClaimType = ClaimTypes.Name,
            RoleClaimType = ClaimTypes.Role
        };
    });
services.AddAuthorization();

// ---- MVC, Swagger ----
services.AddControllers();
services.AddEndpointsApiExplorer();
services.AddSwaggerGen(options =>
{
    options.SwaggerDoc("v1", new OpenApiInfo
    {
        Title = "LPMS API",
        Version = "v1",
        Description = "REST API for the Literacy Programme Management System (READ Educational Trust). " +
                      "Sign in with POST /api/auth/login, then click Authorize and paste the token."
    });
    options.AddSecurityDefinition("Bearer", new OpenApiSecurityScheme
    {
        Name = "Authorization",
        Type = SecuritySchemeType.Http,
        Scheme = "bearer",
        BearerFormat = "JWT",
        In = ParameterLocation.Header,
        Description = "Paste the JWT returned by /api/auth/login (without the word Bearer)."
    });
    options.AddSecurityRequirement(new OpenApiSecurityRequirement
    {
        {
            new OpenApiSecurityScheme
            {
                Reference = new OpenApiReference { Type = ReferenceType.SecurityScheme, Id = "Bearer" }
            },
            Array.Empty<string>()
        }
    });

    var xmlFile = Path.Combine(AppContext.BaseDirectory, $"{typeof(Program).Assembly.GetName().Name}.xml");
    if (File.Exists(xmlFile))
        options.IncludeXmlComments(xmlFile);
});

// ---- CORS (only needed for the website; the Android app is not a browser) ----
services.AddCors();
services.AddOptions<CorsOptions>().Configure<IConfiguration>((cors, configuration) =>
{
    var origins = configuration.GetSection("Cors:AllowedOrigins").Get<string[]>() ?? Array.Empty<string>();
    cors.AddDefaultPolicy(policy =>
    {
        if (origins.Length > 0)
            policy.WithOrigins(origins).AllowAnyHeader().AllowAnyMethod();
    });
});

// ---- Rate limiting: slows down password guessing on the login endpoint ----
services.AddRateLimiter(options =>
{
    options.RejectionStatusCode = StatusCodes.Status429TooManyRequests;
    options.AddPolicy("login", httpContext =>
    {
        var configuration = httpContext.RequestServices.GetRequiredService<IConfiguration>();
        var permitLimit = configuration.GetValue("RateLimiting:LoginPermitLimit", 10);
        return RateLimitPartition.GetFixedWindowLimiter(
            httpContext.Connection.RemoteIpAddress?.ToString() ?? "unknown",
            _ => new FixedWindowRateLimiterOptions
            {
                PermitLimit = permitLimit,
                Window = TimeSpan.FromMinutes(1),
                QueueLimit = 0
            });
    });
});

// ---- Behind the Azure App Service front end we need the real client IP / scheme ----
services.Configure<ForwardedHeadersOptions>(options =>
{
    options.ForwardedHeaders = ForwardedHeaders.XForwardedFor | ForwardedHeaders.XForwardedProto;
    options.KnownNetworks.Clear();
    options.KnownProxies.Clear();
});

var app = builder.Build();

app.UseForwardedHeaders();

// Any unhandled exception becomes a clean JSON 500 (details are logged, never sent to the client).
app.UseExceptionHandler(errorApp => errorApp.Run(async context =>
{
    context.Response.StatusCode = StatusCodes.Status500InternalServerError;
    await context.Response.WriteAsJsonAsync(new ProblemDetails
    {
        Status = StatusCodes.Status500InternalServerError,
        Title = "Unexpected server error",
        Detail = "Something went wrong on the server. Please try again."
    });
}));

// Swagger UI stays on in every environment: it is part of the demonstration.
app.UseSwagger();
app.UseSwaggerUI(options =>
{
    options.SwaggerEndpoint("/swagger/v1/swagger.json", "LPMS API v1");
    options.DocumentTitle = "LPMS API";
});

// No UseHttpsRedirection(): Azure App Service enforces HTTPS in front of the app, and the phone
// talks to a local http:// address while developing.
app.UseCors();
app.UseRateLimiter();
app.UseAuthentication();
app.UseAuthorization();

app.MapGet("/", () => Results.Redirect("/swagger")).ExcludeFromDescription();
app.MapControllers();

app.Run();

// Makes the entry point visible to the integration tests (WebApplicationFactory<Program>).
public partial class Program
{
}
