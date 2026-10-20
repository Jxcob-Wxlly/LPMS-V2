using Lpms.Api.Data;
using Lpms.Api.Dtos;
using Lpms.Api.Models;
using Lpms.Api.Services;
using Microsoft.AspNetCore.Authorization;
using Microsoft.AspNetCore.Mvc;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.EntityFrameworkCore;

namespace Lpms.Api.Controllers;

/// <summary>Sign-in, current user and password change.</summary>
[Route("api/[controller]")]
public class AuthController : ApiControllerBase
{
    private readonly LpmsDbContext _db;
    private readonly ITokenService _tokens;

    public AuthController(LpmsDbContext db, ITokenService tokens)
    {
        _db = db;
        _tokens = tokens;
    }

    /// <summary>Signs in with an email address or username and returns a JWT.</summary>
    /// <remarks>Passwords are checked against the BCrypt hash stored in the User table. Attempts are rate limited.</remarks>
    [HttpPost("login")]
    [AllowAnonymous]
    [EnableRateLimiting("login")]
    public async Task<ActionResult<LoginResponse>> Login([FromBody] LoginRequest request, CancellationToken ct)
    {
        var identifier = request.Identifier.Trim().ToLower();

        var user = await _db.Users
            .Include(u => u.Teacher)
            .FirstOrDefaultAsync(u => u.Email.ToLower() == identifier || u.Username.ToLower() == identifier, ct);

        if (user is null)
        {
            PasswordService.BurnTime(request.Password);
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "Sign-in failed",
                detail: "Incorrect email/username or password.");
        }

        if (!PasswordService.Verify(request.Password, user.PasswordHash))
        {
            return Problem(statusCode: StatusCodes.Status401Unauthorized, title: "Sign-in failed",
                detail: "Incorrect email/username or password.");
        }

        if (!user.IsActive)
        {
            return Problem(statusCode: StatusCodes.Status403Forbidden, title: "Account inactive",
                detail: "This account is inactive. Please contact your administrator.");
        }

        var (token, expiresAtUtc) = _tokens.CreateToken(user);
        return Ok(new LoginResponse(token, expiresAtUtc, await ToProfileAsync(user, ct)));
    }

    /// <summary>Lists schools so a new teacher can pick theirs on the Create account screen.</summary>
    [HttpGet("schools")]
    [AllowAnonymous]
    public async Task<ActionResult<List<SchoolOptionDto>>> Schools(CancellationToken ct) =>
        Ok(await _db.Schools.AsNoTracking()
            .OrderBy(s => s.SchoolName)
            .Select(s => new SchoolOptionDto(s.SchoolId, s.SchoolName))
            .ToListAsync(ct));

    /// <summary>Creates a teacher account and signs the new teacher in.</summary>
    [HttpPost("register")]
    [AllowAnonymous]
    [EnableRateLimiting("login")]
    public async Task<ActionResult<LoginResponse>> Register([FromBody] RegisterRequest request, CancellationToken ct)
    {
        var parts = request.FullName.Split(' ', StringSplitOptions.RemoveEmptyEntries | StringSplitOptions.TrimEntries);
        if (parts.Length < 2)
            return BadRequestProblem("Enter your first and last name.");

        var email = request.Email.Trim();
        if (!System.Text.RegularExpressions.Regex.IsMatch(email, @"^[^@\s]+@[^@\s]+\.[^@\s]+$"))
            return BadRequestProblem("Enter a valid email address.");

        var problem = PasswordPolicy.Validate(request.Password, "password");
        if (problem is not null)
            return BadRequestProblem(problem);

        var school = await _db.Schools.AsNoTracking()
            .FirstOrDefaultAsync(s => s.SchoolId == request.SchoolId, ct);
        if (school is null)
            return BadRequestProblem("Choose your school.");

        var lower = email.ToLower();
        var exists = await _db.Users.AnyAsync(u => u.Email.ToLower() == lower, ct)
                     || await _db.Teachers.AnyAsync(t => t.ContactEmail.ToLower() == lower, ct);
        if (exists)
            return Problem(statusCode: StatusCodes.Status409Conflict, title: "Conflict",
                detail: "An account with this email address already exists. Try signing in instead.");

        // Username comes from the email's local part; add a number if it is taken.
        var baseName = lower.Split('@')[0];
        if (baseName.Length > 44) baseName = baseName[..44];
        var username = baseName;
        for (var n = 2; await _db.Users.AnyAsync(u => u.Username.ToLower() == username, ct); n++)
            username = $"{baseName}{n}";

        var teacher = new Teacher
        {
            FirstName = parts[0],
            LastName = string.Join(' ', parts.Skip(1)),
            SchoolId = school.SchoolId,
            ContactEmail = email
        };
        var user = new UserAccount
        {
            Username = username,
            PasswordHash = PasswordService.Hash(request.Password),
            Role = Roles.Teacher,
            FullName = string.Join(' ', parts),
            Email = email,
            IsActive = true,
            Teacher = teacher
        };
        _db.Users.Add(user);
        await _db.SaveChangesAsync(ct);

        var (token, expiresAtUtc) = _tokens.CreateToken(user);
        return StatusCode(StatusCodes.Status201Created,
            new LoginResponse(token, expiresAtUtc, await ToProfileAsync(user, ct)));
    }

    /// <summary>Returns the profile of the signed-in user.</summary>
    [HttpGet("me")]
    public async Task<ActionResult<UserProfileDto>> Me(CancellationToken ct)
    {
        var userId = User.GetUserId();
        var user = await _db.Users.AsNoTracking()
            .Include(u => u.Teacher)
            .FirstOrDefaultAsync(u => u.UserId == userId, ct);

        if (user is null || !user.IsActive)
            return Unauthorized();

        return Ok(await ToProfileAsync(user, ct));
    }

    /// <summary>Changes the signed-in user's password.</summary>
    [HttpPost("change-password")]
    public async Task<IActionResult> ChangePassword([FromBody] ChangePasswordRequest request, CancellationToken ct)
    {
        var userId = User.GetUserId();
        var user = await _db.Users.FirstOrDefaultAsync(u => u.UserId == userId, ct);
        if (user is null || !user.IsActive)
            return Unauthorized();

        // 400 (not 401) so the mobile app does not mistake this for an expired session.
        if (!PasswordService.Verify(request.CurrentPassword, user.PasswordHash))
            return BadRequestProblem("Your current password is incorrect.");

        var problem = PasswordPolicy.Validate(request.NewPassword);
        if (problem is not null)
            return BadRequestProblem(problem);

        if (request.NewPassword == request.CurrentPassword)
            return BadRequestProblem("The new password must be different from your current password.");

        user.PasswordHash = PasswordService.Hash(request.NewPassword);
        await _db.SaveChangesAsync(ct);
        return NoContent();
    }

    private async Task<UserProfileDto> ToProfileAsync(UserAccount user, CancellationToken ct)
    {
        int? schoolId = user.Teacher?.SchoolId;
        string? schoolName = null;
        if (schoolId is int id)
        {
            schoolName = await _db.Schools.AsNoTracking()
                .Where(s => s.SchoolId == id)
                .Select(s => s.SchoolName)
                .FirstOrDefaultAsync(ct);
        }

        return new UserProfileDto(user.UserId, user.Username, user.FullName, user.Email, user.Role,
            user.TeacherId, schoolId, schoolName);
    }
}
