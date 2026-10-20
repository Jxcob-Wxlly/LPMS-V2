using Lpms.Api.Data;
using Lpms.Api.Dtos;
using Lpms.Api.Models;
using Lpms.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.EntityFrameworkCore;

namespace Lpms.Api.Controllers;

/// <summary>Assessment scores (the Progress table). Teachers manage their own entries.</summary>
[Route("api/[controller]")]
[Authorize(Roles = Roles.Teacher)]
public class ProgressController : ApiControllerBase
{
    private readonly LpmsDbContext _db;
    private readonly IClock _clock;

    public ProgressController(LpmsDbContext db, IClock clock)
    {
        _db = db;
        _clock = clock;
    }

    /// <summary>Adds an assessment for a learner at the teacher's school.</summary>
    [HttpPost]
    public async Task<ActionResult<AssessmentDto>> Create([FromBody] CreateProgressRequest request, CancellationToken ct)
    {
        var teacherId = User.GetTeacherId();
        if (teacherId is null)
            return Forbid();

        var date = request.AssessmentDate!.Value;
        if (date > _clock.Today())
            return BadRequestProblem("The assessment date cannot be in the future.");

        var learner = await _db.Learners.AsNoTracking()
            .FirstOrDefaultAsync(l => l.LearnerId == request.LearnerId, ct);
        if (learner is null)
            return NotFoundProblem("Learner not found.");

        var teacher = await _db.Teachers.AsNoTracking().FirstOrDefaultAsync(t => t.TeacherId == teacherId, ct);
        if (teacher is null)
            return Forbid();
        if (teacher.SchoolId != learner.SchoolId)
            return Forbid();

        var record = new ProgressRecord
        {
            LearnerId = learner.LearnerId,
            TeacherId = teacher.TeacherId,
            AssessmentScore = request.AssessmentScore!.Value,
            AssessmentDate = date,
            Notes = string.IsNullOrWhiteSpace(request.Notes) ? null : request.Notes.Trim()
        };
        _db.ProgressRecords.Add(record);
        await _db.SaveChangesAsync(ct);

        var dto = ToDto(record, $"{teacher.FirstName} {teacher.LastName}");
        return Created($"/api/progress/{record.ProgressId}", dto);
    }

    /// <summary>Edits one of the signed-in teacher's own assessments.</summary>
    [HttpPut("{id:int}")]
    public async Task<ActionResult<AssessmentDto>> Update(int id, [FromBody] UpdateProgressRequest request, CancellationToken ct)
    {
        var record = await _db.ProgressRecords.Include(p => p.Teacher).FirstOrDefaultAsync(p => p.ProgressId == id, ct);
        if (record is null)
            return NotFoundProblem("Assessment not found.");
        if (record.TeacherId != User.GetTeacherId())
            return Forbid();

        var date = request.AssessmentDate!.Value;
        if (date > _clock.Today())
            return BadRequestProblem("The assessment date cannot be in the future.");

        record.AssessmentScore = request.AssessmentScore!.Value;
        record.AssessmentDate = date;
        record.Notes = string.IsNullOrWhiteSpace(request.Notes) ? null : request.Notes.Trim();
        await _db.SaveChangesAsync(ct);

        return Ok(ToDto(record, $"{record.Teacher.FirstName} {record.Teacher.LastName}"));
    }

    /// <summary>Deletes one of the signed-in teacher's own assessments.</summary>
    [HttpDelete("{id:int}")]
    public async Task<IActionResult> Delete(int id, CancellationToken ct)
    {
        var record = await _db.ProgressRecords.FirstOrDefaultAsync(p => p.ProgressId == id, ct);
        if (record is null)
            return NotFoundProblem("Assessment not found.");
        if (record.TeacherId != User.GetTeacherId())
            return Forbid();

        _db.ProgressRecords.Remove(record);
        await _db.SaveChangesAsync(ct);
        return NoContent();
    }

    private static AssessmentDto ToDto(ProgressRecord record, string recordedBy) =>
        new(record.ProgressId, record.AssessmentScore, record.AssessmentDate, record.Notes, recordedBy, true);
}
