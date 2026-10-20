using Lpms.Api.Data;
using Lpms.Api.Dtos;
using Lpms.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace Lpms.Api.Services;

/// <summary>A session joined with its programme, school and attendance counts (one SQL query).</summary>
public class SessionRow
{
    public int SessionId { get; set; }
    public int ProgrammeId { get; set; }
    public string ProgrammeName { get; set; } = string.Empty;
    public int SchoolId { get; set; }
    public string SchoolName { get; set; } = string.Empty;
    public int TeacherId { get; set; }
    public DateOnly SessionDate { get; set; }
    public TimeOnly StartTime { get; set; }
    public TimeOnly EndTime { get; set; }
    public int TotalLearners { get; set; }
    public int PresentCount { get; set; }
    public int AbsentCount { get; set; }

    public bool AttendanceRecorded => PresentCount + AbsentCount > 0;
}

public static class SessionProjection
{
    public const string StatusUpcoming = "Upcoming";
    public const string StatusPending = "Pending";
    public const string StatusCompleted = "Completed";

    public static IQueryable<SessionRow> ToRows(this IQueryable<Session> sessions) =>
        sessions.Select(s => new SessionRow
        {
            SessionId = s.SessionId,
            ProgrammeId = s.ProgrammeId,
            ProgrammeName = s.Programme.ProgrammeName,
            SchoolId = s.SchoolId,
            SchoolName = s.School.SchoolName,
            TeacherId = s.TeacherId,
            SessionDate = s.SessionDate,
            StartTime = s.StartTime,
            EndTime = s.EndTime,
            TotalLearners = s.School.Learners.Count,
            PresentCount = s.AttendanceRecords.Count(a => a.AttendanceStatus == AttendanceStatuses.Present),
            AbsentCount = s.AttendanceRecords.Count(a => a.AttendanceStatus == AttendanceStatuses.Absent)
        });

    /// <summary>
    /// Completed = attendance recorded. Pending = the session has finished but attendance is missing.
    /// Upcoming = anything else (not started yet, or still running).
    /// </summary>
    public static string StatusOf(this SessionRow row, DateOnly today, TimeOnly timeNow)
    {
        if (row.AttendanceRecorded)
            return StatusCompleted;
        var finished = row.SessionDate < today || (row.SessionDate == today && row.EndTime < timeNow);
        return finished ? StatusPending : StatusUpcoming;
    }

    public static SessionSummaryDto ToSummary(this SessionRow row, DateOnly today, TimeOnly timeNow) =>
        new(
            row.SessionId,
            row.ProgrammeId,
            row.ProgrammeName,
            row.SchoolId,
            row.SchoolName,
            row.SessionDate,
            row.StartTime,
            row.EndTime,
            row.StatusOf(today, timeNow),
            row.SessionDate == today,
            row.AttendanceRecorded,
            row.TotalLearners,
            row.PresentCount,
            row.AbsentCount);
}

/// <summary>A learner with the school name, before averages are added.</summary>
public record LearnerRow(int LearnerId, string FirstName, string LastName, string Grade, int SchoolId, string SchoolName);

public static class LearnerSummaries
{
    /// <summary>
    /// Adds average score, attendance rate and assessment count to each learner. Aggregation is done in memory
    /// (a school has at most a few hundred learners) so the same code works on SQL Server and SQLite.
    /// </summary>
    public static async Task<List<LearnerSummaryDto>> BuildAsync(
        LpmsDbContext db, List<LearnerRow> rows, CancellationToken ct)
    {
        var ids = rows.Select(r => r.LearnerId).ToList();

        var scores = await db.ProgressRecords.AsNoTracking()
            .Where(p => ids.Contains(p.LearnerId))
            .Select(p => new { p.LearnerId, p.AssessmentScore })
            .ToListAsync(ct);
        var attendance = await db.AttendanceRecords.AsNoTracking()
            .Where(a => ids.Contains(a.LearnerId))
            .Select(a => new { a.LearnerId, a.AttendanceStatus })
            .ToListAsync(ct);

        var scoreLookup = scores
            .GroupBy(s => s.LearnerId)
            .ToDictionary(g => g.Key, g => g.Select(x => x.AssessmentScore).ToList());
        var attendanceLookup = attendance
            .GroupBy(a => a.LearnerId)
            .ToDictionary(g => g.Key, g => g.Select(x => x.AttendanceStatus).ToList());

        var result = new List<LearnerSummaryDto>();
        foreach (var r in rows)
        {
            scoreLookup.TryGetValue(r.LearnerId, out var learnerScores);
            attendanceLookup.TryGetValue(r.LearnerId, out var learnerAttendance);

            double? average = learnerScores is { Count: > 0 }
                ? Math.Round((double)learnerScores.Average(), 1)
                : null;
            var present = learnerAttendance?.Count(s => s == AttendanceStatuses.Present) ?? 0;
            var total = learnerAttendance?.Count ?? 0;

            result.Add(new LearnerSummaryDto(
                r.LearnerId, r.FirstName, r.LastName, r.Grade, r.SchoolId, r.SchoolName,
                average, AttendanceStats.Rate(present, total), learnerScores?.Count ?? 0));
        }

        return result;
    }
}
