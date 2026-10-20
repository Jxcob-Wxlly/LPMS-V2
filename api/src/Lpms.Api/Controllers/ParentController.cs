using Lpms.Api.Data;
using Lpms.Api.Dtos;
using Lpms.Api.Models;
using Lpms.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace Lpms.Api.Controllers;

/// <summary>The parent / guardian view: only the children linked to the signed-in parent.</summary>
[Route("api/[controller]")]
[Authorize(Roles = Roles.Parent)]
public class ParentController : ApiControllerBase
{
    private readonly LpmsDbContext _db;

    public ParentController(LpmsDbContext db)
    {
        _db = db;
    }

    /// <summary>Lists the signed-in parent's children with average score, attendance rate and assessment count.</summary>
    /// <remarks>Use GET /api/learners/{id}/progress for one child's full report (allowed for linked children only).</remarks>
    [HttpGet("children")]
    public async Task<ActionResult<List<LearnerSummaryDto>>> Children(CancellationToken ct)
    {
        var userId = User.GetUserId();
        var rows = await _db.ParentLearners.AsNoTracking()
            .Where(pl => pl.UserId == userId)
            .OrderBy(pl => pl.Learner.LastName).ThenBy(pl => pl.Learner.FirstName)
            .Select(pl => new LearnerRow(pl.LearnerId, pl.Learner.FirstName, pl.Learner.LastName,
                pl.Learner.Grade, pl.Learner.SchoolId, pl.Learner.School.SchoolName))
            .ToListAsync(ct);

        return Ok(await LearnerSummaries.BuildAsync(_db, rows, ct));
    }
}
