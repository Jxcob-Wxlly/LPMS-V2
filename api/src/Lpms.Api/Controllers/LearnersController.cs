using Lpms.Api.Data;
using Lpms.Api.Dtos;
using Lpms.Api.Models;
using Lpms.Api.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace Lpms.Api.Controllers;

/// <summary>Learners and their progress.</summary>
[Route("api/[controller]")]
public class LearnersController : ApiControllerBase
{
    private readonly LpmsDbContext _db;

    public LearnersController(LpmsDbContext db)
    {
        _db = db;
    }

    /// <summary>Lists learners (max 200). Teachers only ever see learners from their own school.</summary>
    /// <param name="search">Optional text matched against first and last name.</param>
    /// <param name="schoolId">Optional school filter (ignored for teachers).</param>
    [HttpGet]
    public async Task<ActionResult<List<LearnerSummaryDto>>> GetAll(
        [FromQuery] string? search, [FromQuery] int? schoolId, CancellationToken ct)
    {
        if (User.IsInRole(Roles.Parent))
            return Forbid();     // parents use /api/parent/children

        var effectiveSchoolId = schoolId;
        if (User.IsInRole(Roles.Teacher))
        {
            effectiveSchoolId = User.GetSchoolId();
            if (effectiveSchoolId is null)
                return Forbid();
        }

        var query = _db.Learners.AsNoTracking();
        if (effectiveSchoolId is int filterSchoolId)
            query = query.Where(l => l.SchoolId == filterSchoolId);
        if (!string.IsNullOrWhiteSpace(search))
        {
            var term = search.Trim();
            query = query.Where(l => l.FirstName.Contains(term) || l.LastName.Contains(term));
        }

        var rows = await query
            .OrderBy(l => l.LastName).ThenBy(l => l.FirstName)
            .Take(200)
            .Select(l => new LearnerRow(l.LearnerId, l.FirstName, l.LastName, l.Grade, l.SchoolId, l.School.SchoolName))
            .ToListAsync(ct);

        return Ok(await LearnerSummaries.BuildAsync(_db, rows, ct));
    }

    /// <summary>Gets a learner's average score, assessments and attendance history.</summary>
    [HttpGet("{id:int}/progress")]
    public async Task<ActionResult<LearnerProgressDto>> GetProgress(int id, CancellationToken ct)
    {
        var row = await _db.Learners.AsNoTracking()
            .Where(l => l.LearnerId == id)
            .Select(l => new LearnerRow(l.LearnerId, l.FirstName, l.LastName, l.Grade, l.SchoolId, l.School.SchoolName))
            .FirstOrDefaultAsync(ct);

        if (row is null)
            return NotFoundProblem("Learner not found.");
        if (User.IsInRole(Roles.Teacher) && User.GetSchoolId() != row.SchoolId)
            return Forbid();
        if (User.IsInRole(Roles.Parent))
        {
            var userId = User.GetUserId();
            var linked = await _db.ParentLearners.AsNoTracking()
                .AnyAsync(pl => pl.UserId == userId && pl.LearnerId == id, ct);
            if (!linked)
                return Forbid();     // only your own children
        }

        var summary = (await LearnerSummaries.BuildAsync(_db, new List<LearnerRow> { row }, ct)).Single();
        var currentTeacherId = User.GetTeacherId();

        var assessmentRows = await _db.ProgressRecords.AsNoTracking()
            .Where(p => p.LearnerId == id)
            .OrderByDescending(p => p.AssessmentDate).ThenByDescending(p => p.ProgressId)
            .Select(p => new
            {
                p.ProgressId,
                p.AssessmentScore,
                p.AssessmentDate,
                p.Notes,
                p.TeacherId,
                TeacherName = p.Teacher.FirstName + " " + p.Teacher.LastName
            })
            .ToListAsync(ct);
        var assessments = assessmentRows
            .Select(p => new AssessmentDto(
                p.ProgressId, p.AssessmentScore, p.AssessmentDate, p.Notes, p.TeacherName,
                currentTeacherId is not null && currentTeacherId == p.TeacherId))
            .ToList();

        var attendanceRows = await _db.AttendanceRecords.AsNoTracking()
            .Where(a => a.LearnerId == id)
            .OrderByDescending(a => a.Session.SessionDate).ThenByDescending(a => a.SessionId)
            .Select(a => new
            {
                a.SessionId,
                a.Session.SessionDate,
                a.Session.Programme.ProgrammeName,
                a.AttendanceStatus
            })
            .Take(50)
            .ToListAsync(ct);
        var attendance = attendanceRows
            .Select(a => new LearnerAttendanceDto(a.SessionId, a.SessionDate, a.ProgrammeName, a.AttendanceStatus))
            .ToList();

        return Ok(new LearnerProgressDto(summary, ProgressLabels.For(summary.AverageScore), assessments, attendance));
    }
}
