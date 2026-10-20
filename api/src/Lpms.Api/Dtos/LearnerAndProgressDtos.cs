using System.ComponentModel.DataAnnotations;

namespace Lpms.Api.Dtos;

public record LearnerSummaryDto(
    int LearnerId,
    string FirstName,
    string LastName,
    string Grade,
    int SchoolId,
    string SchoolName,
    double? AverageScore,
    double? AttendanceRate,
    int AssessmentCount);

public record AssessmentDto(
    int ProgressId,
    decimal AssessmentScore,
    DateOnly AssessmentDate,
    string? Notes,
    string RecordedBy,
    bool CanEdit);

public record LearnerAttendanceDto(int SessionId, DateOnly SessionDate, string ProgrammeName, string Status);

public record LearnerProgressDto(
    LearnerSummaryDto Learner,
    string ProgressLabel,
    List<AssessmentDto> Assessments,
    List<LearnerAttendanceDto> Attendance);

/// <summary>Fields of an assessment that can be edited (PUT /api/progress/{id}).</summary>
public class UpdateProgressRequest
{
    [Required(ErrorMessage = "Enter an assessment score.")]
    [Range(typeof(decimal), "0", "100", ErrorMessage = "The score must be between 0 and 100.")]
    public decimal? AssessmentScore { get; set; }

    [Required(ErrorMessage = "Choose the assessment date.")]
    public DateOnly? AssessmentDate { get; set; }

    [StringLength(255, ErrorMessage = "Notes can be at most 255 characters.")]
    public string? Notes { get; set; }
}

/// <summary>A new assessment (POST /api/progress). The teacher is taken from the sign-in token.</summary>
public class CreateProgressRequest : UpdateProgressRequest
{
    [Required(ErrorMessage = "Choose a learner.")]
    [Range(1, int.MaxValue, ErrorMessage = "A valid learner id is required.")]
    public int? LearnerId { get; set; }
}
