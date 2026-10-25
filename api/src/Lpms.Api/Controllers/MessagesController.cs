using Lpms.Api.Data;
using Lpms.Api.Dtos;
using Lpms.Api.Models;
using Lpms.Api.Services;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace Lpms.Api.Controllers;

/// <summary>
/// Conversations between parents and the staff at their child's school, one conversation per (learner, parent).
/// Parents see their own children's conversations; teachers see the learners at their school;
/// coordinators and administrators see everything.
/// </summary>
[Route("api/messages")]
public class MessagesController : ApiControllerBase
{
    private readonly LpmsDbContext _db;
    private readonly IClock _clock;

    public MessagesController(LpmsDbContext db, IClock clock)
    {
        _db = db;
        _clock = clock;
    }

    /// <summary>Lists the signed-in user's conversations, most recent first. Pairs with no messages yet come last.</summary>
    [HttpGet("conversations")]
    public async Task<ActionResult<List<ConversationSummaryDto>>> Conversations(CancellationToken ct)
    {
        var userId = User.GetUserId();
        if (userId is null)
            return Forbid();

        var links = _db.ParentLearners.AsNoTracking().AsQueryable();
        if (User.IsInRole(Roles.Parent))
            links = links.Where(pl => pl.UserId == userId);
        else if (User.IsInRole(Roles.Teacher))
        {
            var schoolId = User.GetSchoolId();
            if (schoolId is null)
                return Forbid();
            links = links.Where(pl => pl.Learner.SchoolId == schoolId);
        }

        var pairs = await links
            .Select(pl => new
            {
                pl.LearnerId,
                LearnerFirst = pl.Learner.FirstName,
                LearnerLast = pl.Learner.LastName,
                pl.Learner.Grade,
                ParentUserId = pl.UserId,
                ParentName = pl.User.FullName,
                SchoolName = pl.Learner.School.SchoolName
            })
            .ToListAsync(ct);

        var learnerIds = pairs.Select(p => p.LearnerId).Distinct().ToList();
        var messages = await _db.Messages.AsNoTracking()
            .Where(m => learnerIds.Contains(m.LearnerId))
            .Select(m => new { m.LearnerId, m.ParentUserId, m.Body, m.SentAt, m.MessageId, SenderName = m.SenderUser.FullName })
            .ToListAsync(ct);

        var result = pairs.Select(p =>
        {
            var thread = messages
                .Where(m => m.LearnerId == p.LearnerId && m.ParentUserId == p.ParentUserId)
                .OrderBy(m => m.SentAt).ThenBy(m => m.MessageId)
                .ToList();
            var last = thread.LastOrDefault();
            return new ConversationSummaryDto(
                p.LearnerId, $"{p.LearnerFirst} {p.LearnerLast}", p.Grade,
                p.ParentUserId, p.ParentName, p.SchoolName,
                last?.Body, last?.SentAt, last?.SenderName, thread.Count);
        })
        .OrderByDescending(c => c.LastSentAt.HasValue)
        .ThenByDescending(c => c.LastSentAt)
        .ThenBy(c => c.LearnerName)
        .ToList();

        return Ok(result);
    }

    /// <summary>Returns the messages of one conversation, oldest first.</summary>
    [HttpGet("conversations/{learnerId:int}/{parentUserId:int}")]
    public async Task<ActionResult<List<MessageDto>>> Thread(int learnerId, int parentUserId, CancellationToken ct)
    {
        var access = await CheckAccessAsync(learnerId, parentUserId, ct);
        if (access != null)
            return access;

        var me = User.GetUserId();
        var rows = await _db.Messages.AsNoTracking()
            .Where(m => m.LearnerId == learnerId && m.ParentUserId == parentUserId)
            .OrderBy(m => m.SentAt).ThenBy(m => m.MessageId)
            .Select(m => new { m.MessageId, m.SenderUserId, m.SenderUser.FullName, m.SenderUser.Role, m.Body, m.SentAt })
            .ToListAsync(ct);

        return Ok(rows.Select(m => new MessageDto(
            m.MessageId, m.SenderUserId, m.FullName, m.Role, m.Body, m.SentAt, m.SenderUserId == me)).ToList());
    }

    /// <summary>Sends a message into a conversation.</summary>
    [HttpPost("conversations/{learnerId:int}/{parentUserId:int}")]
    public async Task<ActionResult<MessageDto>> Send(
        int learnerId, int parentUserId, [FromBody] SendMessageRequest request, CancellationToken ct)
    {
        var access = await CheckAccessAsync(learnerId, parentUserId, ct);
        if (access != null)
            return access;

        var body = request.Body.Trim();
        if (body.Length == 0)
            return BadRequestProblem("Type a message first.");

        var userId = User.GetUserId()!.Value;
        var sender = await _db.Users.AsNoTracking().FirstAsync(u => u.UserId == userId, ct);

        var message = new ConversationMessage
        {
            LearnerId = learnerId,
            ParentUserId = parentUserId,
            SenderUserId = userId,
            Body = body,
            SentAt = _clock.Now
        };
        _db.Messages.Add(message);
        await _db.SaveChangesAsync(ct);

        return StatusCode(StatusCodes.Status201Created,
            new MessageDto(message.MessageId, userId, sender.FullName, sender.Role, message.Body, message.SentAt, true));
    }

    /// <summary>Null when the caller may use this conversation; otherwise the response to return.</summary>
    private async Task<ActionResult?> CheckAccessAsync(int learnerId, int parentUserId, CancellationToken ct)
    {
        var userId = User.GetUserId();
        if (userId is null)
            return Forbid();

        var link = await _db.ParentLearners.AsNoTracking()
            .Where(pl => pl.LearnerId == learnerId && pl.UserId == parentUserId)
            .Select(pl => new { pl.Learner.SchoolId })
            .FirstOrDefaultAsync(ct);
        if (link is null)
            return NotFoundProblem("There is no conversation for that learner and parent.");

        if (User.IsInRole(Roles.Parent))
            return parentUserId == userId ? null : Forbid();

        if (User.IsInRole(Roles.Teacher))
            return User.GetSchoolId() == link.SchoolId ? null : Forbid();

        return null;   // coordinators and administrators
    }
}
