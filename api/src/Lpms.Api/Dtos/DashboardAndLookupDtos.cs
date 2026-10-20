namespace Lpms.Api.Dtos;

public record TeacherDashboardDto(
    string TeacherName,
    string SchoolName,
    int SessionsThisWeek,
    int SessionsToday,
    int TotalLearners,
    double? AttendanceRate,
    int PendingAttendance,
    List<SessionSummaryDto> TodaySessions);

public record SchoolDto(
    int SchoolId,
    string SchoolName,
    string Address,
    string PrincipalName,
    string ContactNumber,
    string? ContactEmail);

public record ProgrammeDto(
    int ProgrammeId,
    string ProgrammeName,
    string? Description,
    DateOnly StartDate,
    DateOnly EndDate);

public record HealthDto(string Status, string Database, DateTime TimeUtc, string Version);
