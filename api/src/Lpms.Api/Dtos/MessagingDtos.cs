using System.ComponentModel.DataAnnotations;

namespace Lpms.Api.Dtos;

/// <summary>
/// One conversation: a parent and the staff at a learner's school, about that learner.
/// LastMessage and LastSentAt are null until the first message is sent.
/// </summary>
public record ConversationSummaryDto(
    int LearnerId,
    string LearnerName,
    string Grade,
    int ParentUserId,
    string ParentName,
    string SchoolName,
    string? LastMessage,
    DateTime? LastSentAt,
    string? LastSenderName,
    int MessageCount);

public record MessageDto(
    int MessageId,
    int SenderUserId,
    string SenderName,
    string SenderRole,
    string Body,
    DateTime SentAt,
    bool IsMine);

public class SendMessageRequest
{
    [Required(ErrorMessage = "Type a message first.")]
    [StringLength(1000, ErrorMessage = "A message can be at most 1000 characters.")]
    public string Body { get; set; } = string.Empty;
}

public record AnnouncementDto(
    int AnnouncementId,
    string Title,
    string Body,
    string Audience,
    int? SchoolId,
    string? SchoolName,
    string AuthorName,
    DateTime CreatedAt);

public class CreateAnnouncementRequest
{
    [Required(ErrorMessage = "Enter a title.")]
    [StringLength(100, ErrorMessage = "The title can be at most 100 characters.")]
    public string Title { get; set; } = string.Empty;

    [Required(ErrorMessage = "Write the announcement.")]
    [StringLength(1000, ErrorMessage = "The announcement can be at most 1000 characters.")]
    public string Body { get; set; } = string.Empty;

    [Required(ErrorMessage = "Choose who should see it.")]
    [RegularExpression("^(Everyone|Teachers|Parents)$", ErrorMessage = "Audience must be Everyone, Teachers or Parents.")]
    public string Audience { get; set; } = string.Empty;

    /// <summary>Leave empty to send to every school.</summary>
    public int? SchoolId { get; set; }
}
