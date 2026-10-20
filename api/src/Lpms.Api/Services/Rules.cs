namespace Lpms.Api.Services;

/// <summary>Attendance calculations shared by the dashboard, learner list and learner progress screens.</summary>
public static class AttendanceStats
{
    /// <summary>Percentage present (one decimal), or null when nothing has been recorded.</summary>
    public static double? Rate(int present, int total) =>
        total <= 0 ? null : Math.Round(present * 100.0 / total, 1);
}

/// <summary>Plain-language label for a learner's average assessment score.</summary>
public static class ProgressLabels
{
    public static string For(double? averageScore) => averageScore switch
    {
        null => "No assessments yet",
        >= 80 => "Excellent progress",
        >= 65 => "Good progress",
        >= 50 => "Making progress",
        _ => "Needs support"
    };
}

/// <summary>Password rules enforced when a password is changed.</summary>
public static class PasswordPolicy
{
    public const int MinLength = 8;
    public const int MaxLength = 100;

    /// <summary>Returns a user-friendly message describing the first broken rule, or null when the password is acceptable.</summary>
    public static string? Validate(string? password, string label = "new password")
    {
        if (string.IsNullOrEmpty(password) || password.Length < MinLength)
            return $"The {label} must be at least {MinLength} characters long.";
        if (password.Length > MaxLength)
            return $"The {label} must be {MaxLength} characters or fewer.";
        if (!password.Any(char.IsUpper))
            return $"The {label} must contain an uppercase letter.";
        if (!password.Any(char.IsLower))
            return $"The {label} must contain a lowercase letter.";
        if (!password.Any(char.IsDigit))
            return $"The {label} must contain a number.";
        return null;
    }
}
