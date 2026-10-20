using Lpms.Api.Data;
using Lpms.Api.Dtos;
using Lpms.Api.Models;
using Lpms.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace Lpms.Api.Controllers;

/// <summary>Programme sessions and attendance.</summary>
[Route("api/[controller]")]
public class SessionsController : ApiControllerBase
{
    private readonly LpmsDbContext _db;
    private readonly IClock _clock;

    public SessionsController(LpmsDbContext db, IClock clock)
    {
        _db = db;
        _clock = clock;
    }

    /// <summary>Lists the signed-in teacher's sessions.</summary>
    /// <param name="scope">"upcoming" (today onwards, soonest first), "past" (before today, newest first) or "all".</param>
    [HttpGet("mine")]
    [Authorize(Roles = Roles.Teacher)]
    public async Task<ActionResult<List<SessionSummaryDto>>> GetMine([FromQuery] string scope = "upcoming", CancellationToken ct = default)
    {
        var teacherId = User.GetTeacherId();
        if (teacherId is null)
            return Forbid();

        var today = _clock.Today();
        var query = _db.Sessions.AsNoTracking().Where(s => s.TeacherId == teacherId);

        IQueryable<Session> ordered;
        switch (scope.Trim().ToLowerInvariant())
        {
            case "upcoming":
                ordered = query.Where(s => s.SessionDate >= today)
                    .OrderBy(s => s.SessionDate).ThenBy(s => s.StartTime);
                break;
            case "past":
                ordered = query.Where(s => s.SessionDate < today)
                    .OrderByDescending(s => s.SessionDate).ThenByDescending(s => s.StartTime);
                break;
            case "all":
                ordered = query.OrderBy(s => s.SessionDate).ThenBy(s => s.StartTime);
                break;
            default:
                return BadRequestProblem("scope must be 'upcoming', 'past' or 'all'.");
        }

        var rows = await ordered.ToRows().ToListAsync(ct);
        var timeNow = _clock.TimeOfDay();
        return Ok(rows.Select(r => r.ToSummary(today, timeNow)).ToList());
    }

    /// <summary>Gets one session with its learner roster and any attendance already recorded.</summary>
    [HttpGet("{id:int}")]
    public async Task<ActionResult<SessionDetailDto>> GetById(int id, CancellationToken ct)
    {
        var row = await _db.Sessions.AsNoTracking()
            .Where(s => s.SessionId == id)
            .ToRows()
            .FirstOrDefaultAsync(ct);

        if (User.IsInRole(Roles.Parent))
            return Forbid();     // rosters list other children, so parents never see them
        if (row is null)
            return NotFoundProblem("Session not found.");
        if (User.IsInRole(Roles.Teacher) && User.GetTeacherId() != row.TeacherId)
            return Forbid();

        var learners = await _db.Learners.AsNoTracking()
            .Where(l => l.SchoolId == row.SchoolId)
            .OrderBy(l => l.LastName).ThenBy(l => l.FirstName)
            .Select(l => new { l.LearnerId, l.FirstName, l.LastName, l.Grade })
            .ToListAsync(ct);
        var statuses = await _db.AttendanceRecords.AsNoTracking()
            .Where(a => a.SessionId == id)
            .ToDictionaryAsync(a => a.LearnerId, a => a.AttendanceStatus, ct);

        var roster = learners
            .Select(l => new RosterEntryDto(
                l.LearnerId, l.FirstName, l.LastName, l.Grade,
                statuses.TryGetValue(l.LearnerId, out var status) ? status : null))
            .ToList();

        return Ok(new SessionDetailDto(row.ToSummary(_clock.Today(), _clock.TimeOfDay()), roster));
    }

    /// <summary>Records (or corrects) attendance for a session. Only the session's own teacher may do this.</summary>
    [HttpPut("{id:int}/attendance")]
    [Authorize(Roles = Roles.Teacher)]
    public async Task<ActionResult<SessionDetailDto>> SaveAttendance(
        int id, [FromBody] SaveAttendanceRequest request, CancellationToken ct)
    {
        var session = await _db.Sessions.FirstOrDefaultAsync(s => s.SessionId == id, ct);
        if (session is null)
            return NotFoundProblem("Session not found.");
        if (session.TeacherId != User.GetTeacherId())
            return Forbid();
        if (session.SessionDate > _clock.Today())
            return BadRequestProblem("Attendance cannot be recorded for a session that has not happened yet.");

        var learnerIds = request.Entries.Select(e => e.LearnerId).ToList();
        if (learnerIds.Distinct().Count() != learnerIds.Count)
            return BadRequestProblem("Each learner can only appear once in the attendance list.");

        var validIds = await _db.Learners.AsNoTracking()
            .Where(l => l.SchoolId == session.SchoolId && learnerIds.Contains(l.LearnerId))
            .Select(l => l.LearnerId)
            .ToListAsync(ct);
        var unknown = learnerIds.Except(validIds).ToList();
        if (unknown.Count > 0)
            return BadRequestProblem($"Learner(s) {string.Join(", ", unknown)} do not belong to this session's school.");

        var existing = await _db.AttendanceRecords.Where(a => a.SessionId == id).ToListAsync(ct);
        foreach (var entry in request.Entries)
        {
            var record = existing.FirstOrDefault(a => a.LearnerId == entry.LearnerId);
            if (record is null)
            {
                _db.AttendanceRecords.Add(new AttendanceRecord
                {
                    SessionId = id,
                    LearnerId = entry.LearnerId,
                    AttendanceStatus = entry.Status
                });
            }
            else
            {
                record.AttendanceStatus = entry.Status;
            }
        }

        try
        {
            await _db.SaveChangesAsync(ct);
        }
        catch (DbUpdateException)
        {
            return Problem(statusCode: StatusCodes.Status409Conflict, title: "Conflict",
                detail: "Attendance for this session was changed at the same time. Reload and try again.");
        }

        return await GetById(id, ct);
    }

    /// <summary>Schedules a new session. Teachers schedule for themselves; Administrators and Coordinators choose the teacher.</summary>
    [HttpPost]
    [Authorize(Roles = "Administrator,Coordinator,Teacher")]
    public async Task<ActionResult<SessionSummaryDto>> Create([FromBody] CreateSessionRequest request, CancellationToken ct)
    {
        var date = request.SessionDate!.Value;
        var start = request.StartTime!.Value;
        var end = request.EndTime!.Value;
        if (end <= start)
            return BadRequestProblem("The end time must be after the start time.");

        var programme = await _db.Programmes.AsNoTracking()
            .FirstOrDefaultAsync(p => p.ProgrammeId == request.ProgrammeId, ct);
        if (programme is null)
            return BadRequestProblem("The programme does not exist.");
        if (date < programme.StartDate || date > programme.EndDate)
            return BadRequestProblem("The session date must fall within the programme's start and end dates.");

        var schoolId = request.SchoolId;
        var teacherId = request.TeacherId;
        if (User.IsInRole(Roles.Teacher))
        {
            var ownTeacherId = User.GetTeacherId();
            var ownSchoolId = User.GetSchoolId();
            if ((teacherId is not null && teacherId != ownTeacherId) ||
                (schoolId is not null && schoolId != ownSchoolId))
                return Forbid();
            teacherId = ownTeacherId;
            schoolId = ownSchoolId;
        }
        else
        {
            if (schoolId is null) return BadRequestProblem("Choose a school.");
            if (teacherId is null) return BadRequestProblem("Choose a teacher.");
        }

        var school = await _db.Schools.AsNoTracking()
            .FirstOrDefaultAsync(s => s.SchoolId == schoolId, ct);
        if (school is null)
            return BadRequestProblem("The school does not exist.");

        var teacher = await _db.Teachers.AsNoTracking()
            .FirstOrDefaultAsync(t => t.TeacherId == teacherId, ct);
        if (teacher is null)
            return BadRequestProblem("The teacher does not exist.");
        if (teacher.SchoolId != school.SchoolId)
            return BadRequestProblem("The teacher does not work at the selected school.");

        var clash = await _db.Sessions.AnyAsync(s =>
            s.TeacherId == teacher.TeacherId && s.SessionDate == date && s.StartTime < end && start < s.EndTime, ct);
        if (clash)
            return Problem(statusCode: StatusCodes.Status409Conflict, title: "Conflict",
                detail: "The teacher already has a session that overlaps this time.");

        var session = new Session
        {
            ProgrammeId = programme.ProgrammeId,
            SchoolId = school.SchoolId,
            TeacherId = teacher.TeacherId,
            SessionDate = date,
            StartTime = start,
            EndTime = end
        };
        _db.Sessions.Add(session);
        await _db.SaveChangesAsync(ct);

        var row = await _db.Sessions.AsNoTracking()
            .Where(s => s.SessionId == session.SessionId)
            .ToRows()
            .FirstAsync(ct);
        var summary = row.ToSummary(_clock.Today(), _clock.TimeOfDay());
        return CreatedAtAction(nameof(GetById), new { id = session.SessionId }, summary);
    }
}
