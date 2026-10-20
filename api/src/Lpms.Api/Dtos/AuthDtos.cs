using System.ComponentModel.DataAnnotations;

namespace Lpms.Api.Dtos;

public class LoginRequest
{
    /// <summary>The user's email address or username.</summary>
    [Required(ErrorMessage = "Enter your email address or username.")]
    [StringLength(100, ErrorMessage = "The email address or username is too long.")]
    public string Identifier { get; set; } = string.Empty;

    [Required(ErrorMessage = "Enter your password.")]
    [StringLength(100, ErrorMessage = "The password is too long.")]
    public string Password { get; set; } = string.Empty;
}

public record UserProfileDto(
    int UserId,
    string Username,
    string FullName,
    string Email,
    string Role,
    int? TeacherId,
    int? SchoolId,
    string? SchoolName);

public record LoginResponse(string Token, DateTime ExpiresAtUtc, UserProfileDto User);

public class ChangePasswordRequest
{
    [Required(ErrorMessage = "Enter your current password.")]
    [StringLength(100)]
    public string CurrentPassword { get; set; } = string.Empty;

    [Required(ErrorMessage = "Enter a new password.")]
    [StringLength(100)]
    public string NewPassword { get; set; } = string.Empty;
}

public class RegisterRequest
{
    [Required(ErrorMessage = "Enter your name.")]
    [StringLength(100, ErrorMessage = "The name is too long.")]
    public string FullName { get; set; } = string.Empty;

    [Required(ErrorMessage = "Enter your email address.")]
    [StringLength(100, ErrorMessage = "The email address is too long.")]
    public string Email { get; set; } = string.Empty;

    [Required(ErrorMessage = "Choose your school.")]
    public int? SchoolId { get; set; }

    [Required(ErrorMessage = "Enter a password.")]
    [StringLength(100, ErrorMessage = "The password is too long.")]
    public string Password { get; set; } = string.Empty;
}

/// <summary>Minimal school info shown on the Create account screen before sign-in.</summary>
public record SchoolOptionDto(int SchoolId, string SchoolName);
