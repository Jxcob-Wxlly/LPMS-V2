using Lpms.Api.Data;
using Lpms.Api.Dtos;
using Lpms.Api.Models;
using Lpms.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace Lpms.Api.Controllers;

/// <summary>Notices from coordinators and administrators to teachers and/or parents.</summary>
[Route("api/announcements")]
public class AnnouncementsController : ApiControllerBase
{
    private readonly LpmsDbContext _db;
    private readonly IClock _clock;

    public AnnouncementsController(LpmsDbContext db, IClock clock)
    {
        _db = db;
        _clock = clock;
    }

    /// <summary>
    /// Announcements the signed-in user may read, newest first (at most 50).
    /// Teachers see "Everyone" and "Teachers" notices for their school or for all schools;
    /// parents see "Everyone" and "Parents" notices for their children's schools or for all schools;
    /// coordinators and administrators see everything.
    /// </summary>
    [HttpGet]
    public async Task<ActionResult<List<AnnouncementDto>>> List(CancellationToken ct)
    {
        var userId = User.GetUserId();
        if (userId is null)
            return Forbid();

        var query = _db.Announcements.AsNoTracking().AsQueryable();

        if (User.IsInRole(Roles.Teacher))
        {
            var schoolId = User.GetSchoolId();
            if (schoolId is null)
                return Forbid();
            query = query.Where(a => a.Audience != AnnouncementAudiences.Parents
                                     && (a.SchoolId == null || a.SchoolId == schoolId));
        }
        else if (User.IsInRole(Roles.Parent))
        {
            var schoolIds = await _db.ParentLearners.AsNoTracking()
                .Where(pl => pl.UserId == userId)
                .Select(pl => pl.Learner.SchoolId)
                .Distinct()
                .ToListAsync(ct);
            query = query.Where(a => a.Audience != AnnouncementAudiences.Teachers
                                     && (a.SchoolId == null || schoolIds.Contains(a.SchoolId.Value)));
        }

        var rows = await query
            .OrderByDescending(a => a.CreatedAt).ThenByDescending(a => a.AnnouncementId)
            .Take(50)
            .Select(a => new AnnouncementDto(
                a.AnnouncementId, a.Title, a.Body, a.Audience, a.SchoolId,
                a.School == null ? null : a.School.SchoolName,
                a.AuthorUser.FullName, a.CreatedAt))
            .ToListAsync(ct);

        return Ok(rows);
    }

    /// <summary>Posts an announcement. Coordinators and administrators only.</summary>
    [HttpPost]
    [Authorize(Roles = Roles.AdministratorOrCoordinator)]
    public async Task<ActionResult<AnnouncementDto>> Create([FromBody] CreateAnnouncementRequest request, CancellationToken ct)
    {
        var title = request.Title.Trim();
        var body = request.Body.Trim();
        if (title.Length == 0)
            return BadRequestProblem("Enter a title.");
        if (body.Length == 0)
            return BadRequestProblem("Write the announcement.");

        string? schoolName = null;
        if (request.SchoolId is not null)
        {
            schoolName = await _db.Schools.AsNoTracking()
                .Where(s => s.SchoolId == request.SchoolId)
                .Select(s => s.SchoolName)
                .FirstOrDefaultAsync(ct);
            if (schoolName is null)
                return BadRequestProblem("That school does not exist.");
        }

        var userId = User.GetUserId()!.Value;
        var author = await _db.Users.AsNoTracking().FirstAsync(u => u.UserId == userId, ct);

        var announcement = new Announcement
        {
            AuthorUserId = userId,
            Title = title,
            Body = body,
            Audience = request.Audience,
            SchoolId = request.SchoolId,
            CreatedAt = _clock.Now
        };
        _db.Announcements.Add(announcement);
        await _db.SaveChangesAsync(ct);

        return StatusCode(StatusCodes.Status201Created, new AnnouncementDto(
            announcement.AnnouncementId, title, body, announcement.Audience,
            announcement.SchoolId, schoolName, author.FullName, announcement.CreatedAt));
    }
}
