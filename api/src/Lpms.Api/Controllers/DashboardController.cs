using Lpms.Api.Data;
using Lpms.Api.Dtos;
using Lpms.Api.Models;
using Lpms.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace Lpms.Api.Controllers;

/// <summary>Summary figures for the mobile app's home screen.</summary>
[Route("api/[controller]")]
public class DashboardController : ApiControllerBase
{
    private readonly LpmsDbContext _db;
    private readonly IClock _clock;

    public DashboardController(LpmsDbContext db, IClock clock)
    {
        _db = db;
        _clock = clock;
    }

    /// <summary>Counts and today's sessions for the signed-in teacher.</summary>
    [HttpGet("teacher")]
    [Authorize(Roles = Roles.Teacher)]
    public async Task<ActionResult<TeacherDashboardDto>> GetTeacherDashboard(CancellationToken ct)
    {
        var teacherId = User.GetTeacherId();
        if (teacherId is null)
            return Forbid();

        var teacher = await _db.Teachers.AsNoTracking()
            .Include(t => t.School)
            .FirstOrDefaultAsync(t => t.TeacherId == teacherId, ct);
        if (teacher is null)
            return Forbid();

        var today = _clock.Today();
        var timeNow = _clock.TimeOfDay();
        var daysSinceMonday = ((int)today.DayOfWeek + 6) % 7;
        var weekStart = today.AddDays(-daysSinceMonday);
        var weekEnd = weekStart.AddDays(6);

        var rows = await _db.Sessions.AsNoTracking()
            .Where(s => s.TeacherId == teacher.TeacherId)
            .ToRows()
            .ToListAsync(ct);

        var recorded = rows.Where(r => r.AttendanceRecorded).ToList();
        var present = recorded.Sum(r => r.PresentCount);
        var marked = recorded.Sum(r => r.PresentCount + r.AbsentCount);

        var todaySessions = rows
            .Where(r => r.SessionDate == today)
            .OrderBy(r => r.StartTime)
            .Select(r => r.ToSummary(today, timeNow))
            .ToList();

        var totalLearners = await _db.Learners.CountAsync(l => l.SchoolId == teacher.SchoolId, ct);

        return Ok(new TeacherDashboardDto(
            TeacherName: $"{teacher.FirstName} {teacher.LastName}",
            SchoolName: teacher.School.SchoolName,
            SessionsThisWeek: rows.Count(r => r.SessionDate >= weekStart && r.SessionDate <= weekEnd),
            SessionsToday: todaySessions.Count,
            TotalLearners: totalLearners,
            AttendanceRate: AttendanceStats.Rate(present, marked),
            PendingAttendance: rows.Count(r => r.StatusOf(today, timeNow) == SessionProjection.StatusPending),
            TodaySessions: todaySessions));
    }
}
