using System.ComponentModel.DataAnnotations;

namespace Lpms.Api.Dtos;

/// <summary>
/// Status values: "Upcoming" (not finished yet), "Pending" (finished but attendance not recorded),
/// "Completed" (attendance recorded).
/// </summary>
public record SessionSummaryDto(
    int SessionId,
    int ProgrammeId,
    string ProgrammeName,
    int SchoolId,
    string SchoolName,
    DateOnly SessionDate,
    TimeOnly StartTime,
    TimeOnly EndTime,
    string Status,
    bool IsToday,
    bool AttendanceRecorded,
    int TotalLearners,
    int PresentCount,
    int AbsentCount);

/// <summary>One learner on a session's roster; Status is null until attendance has been recorded.</summary>
public record RosterEntryDto(int LearnerId, string FirstName, string LastName, string Grade, string? Status);

public record SessionDetailDto(SessionSummaryDto Session, List<RosterEntryDto> Learners);

public class AttendanceEntryDto
{
    [Range(1, int.MaxValue, ErrorMessage = "A valid learner id is required.")]
    public int LearnerId { get; set; }

    [Required(ErrorMessage = "Attendance status is required.")]
    [RegularExpression("^(Present|Absent)$", ErrorMessage = "Status must be Present or Absent.")]
    public string Status { get; set; } = string.Empty;
}

public class SaveAttendanceRequest
{
    [Required]
    [MinLength(1, ErrorMessage = "Send at least one attendance entry.")]
    public List<AttendanceEntryDto> Entries { get; set; } = new();
}

public class CreateSessionRequest
{
    [Required(ErrorMessage = "Choose a programme.")]
    public int? ProgrammeId { get; set; }

    /// <summary>Required for Administrators and Coordinators. Teachers always schedule at their own school.</summary>
    public int? SchoolId { get; set; }

    /// <summary>Required for Administrators and Coordinators. Teachers always schedule for themselves.</summary>
    public int? TeacherId { get; set; }

    [Required(ErrorMessage = "Choose the session date.")]
    public DateOnly? SessionDate { get; set; }

    /// <example>09:00:00</example>
    [Required(ErrorMessage = "Enter the start time.")]
    public TimeOnly? StartTime { get; set; }

    /// <example>10:00:00</example>
    [Required(ErrorMessage = "Enter the end time.")]
    public TimeOnly? EndTime { get; set; }
}
