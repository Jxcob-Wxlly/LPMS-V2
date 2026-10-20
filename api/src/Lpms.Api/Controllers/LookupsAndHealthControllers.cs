using Lpms.Api.Data;
using Lpms.Api.Dtos;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace Lpms.Api.Controllers;

/// <summary>Reference lists (schools, programmes) for any signed-in user.</summary>
[Route("api")]
public class LookupsController : ApiControllerBase
{
    private readonly LpmsDbContext _db;

    public LookupsController(LpmsDbContext db)
    {
        _db = db;
    }

    /// <summary>Lists all schools.</summary>
    [HttpGet("schools")]
    public async Task<ActionResult<List<SchoolDto>>> GetSchools(CancellationToken ct) =>
        Ok(await _db.Schools.AsNoTracking()
            .OrderBy(s => s.SchoolName)
            .Select(s => new SchoolDto(s.SchoolId, s.SchoolName, s.Address, s.PrincipalName, s.ContactNumber, s.ContactEmail))
            .ToListAsync(ct));

    /// <summary>Lists all programmes.</summary>
    [HttpGet("programmes")]
    public async Task<ActionResult<List<ProgrammeDto>>> GetProgrammes(CancellationToken ct) =>
        Ok(await _db.Programmes.AsNoTracking()
            .OrderBy(p => p.ProgrammeName)
            .Select(p => new ProgrammeDto(p.ProgrammeId, p.ProgrammeName, p.Description, p.StartDate, p.EndDate))
            .ToListAsync(ct));
}

/// <summary>Anonymous health check used by monitoring (the "Monitor" stage of the DevOps lifecycle).</summary>
[Route("api/health")]
[AllowAnonymous]
public class HealthController : ApiControllerBase
{
    private readonly LpmsDbContext _db;

    private readonly ILogger<HealthController> _logger;

    public HealthController(LpmsDbContext db, ILogger<HealthController> logger)
    {
        _db = db;
        _logger = logger;
    }

    /// <summary>Returns 200 when the API can reach the database, otherwise 503.</summary>
    [HttpGet]
    public async Task<ActionResult<HealthDto>> Get(CancellationToken ct)
    {
        bool canConnect;
        try
        {
            canConnect = await _db.Database.CanConnectAsync(ct);
        }
        catch (Exception ex)
        {
            _logger.LogError(ex, "Health check could not reach the database.");
            canConnect = false;
        }

        var version = typeof(HealthController).Assembly.GetName().Version?.ToString() ?? "1.0.0";
        var dto = new HealthDto(
            canConnect ? "Healthy" : "Unhealthy",
            canConnect ? "Connected" : "Unavailable",
            DateTime.UtcNow,
            version);

        return canConnect ? Ok(dto) : StatusCode(StatusCodes.Status503ServiceUnavailable, dto);
    }
}
