using Lpms.Api.Services;

namespace Lpms.Api.Tests;

/// <summary>Pure unit tests (no web host, no database).</summary>
public class RulesUnitTests
{
    // ---------------------------------------------------------------- attendance rate

    [Theory]
    [InlineData(0, 0, null)]
    [InlineData(0, 4, 0.0)]
    [InlineData(4, 4, 100.0)]
    [InlineData(2, 3, 66.7)]
    [InlineData(23, 25, 92.0)]
    public void AttendanceRate_is_a_percentage_with_one_decimal(int present, int total, double? expected)
    {
        Assert.Equal(expected, AttendanceStats.Rate(present, total));
    }

    // ---------------------------------------------------------------- progress label

    [Theory]
    [InlineData(null, "No assessments yet")]
    [InlineData(95.0, "Excellent progress")]
    [InlineData(80.0, "Excellent progress")]
    [InlineData(79.9, "Good progress")]
    [InlineData(65.0, "Good progress")]
    [InlineData(64.9, "Making progress")]
    [InlineData(50.0, "Making progress")]
    [InlineData(49.9, "Needs support")]
    [InlineData(0.0, "Needs support")]
    public void ProgressLabel_matches_the_score_bands(double? average, string expected)
    {
        Assert.Equal(expected, ProgressLabels.For(average));
    }

    // ---------------------------------------------------------------- password policy

    [Theory]
    [InlineData("Abcdef12")]
    [InlineData("Lpms@2026!")]
    [InlineData("A-very-long-Passphrase-with-1-digit")]
    public void PasswordPolicy_accepts_strong_passwords(string password)
    {
        Assert.Null(PasswordPolicy.Validate(password));
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("Abc1")]
    [InlineData("abcdefg1")]
    [InlineData("ABCDEFG1")]
    [InlineData("Abcdefgh")]
    public void PasswordPolicy_rejects_weak_passwords(string? password)
    {
        Assert.NotNull(PasswordPolicy.Validate(password));
    }

    [Fact]
    public void PasswordPolicy_rejects_passwords_longer_than_100_characters()
    {
        Assert.NotNull(PasswordPolicy.Validate("Aa1" + new string('x', 100)));
    }

    // ---------------------------------------------------------------- password hashing

    [Fact]
    public void PasswordService_verifies_the_right_password_and_rejects_the_wrong_one()
    {
        var hash = PasswordService.Hash("Secret123", workFactor: 4);

        Assert.StartsWith("$2", hash);
        Assert.NotEqual("Secret123", hash);
        Assert.True(PasswordService.Verify("Secret123", hash));
        Assert.False(PasswordService.Verify("secret123", hash));
    }

    [Fact]
    public void PasswordService_gives_a_different_hash_each_time()
    {
        Assert.NotEqual(PasswordService.Hash("Secret123", 4), PasswordService.Hash("Secret123", 4));
    }

    [Theory]
    [InlineData(null)]
    [InlineData("")]
    [InlineData("not-a-bcrypt-hash")]
    public void PasswordService_treats_a_malformed_stored_hash_as_a_failed_check(string? storedHash)
    {
        Assert.False(PasswordService.Verify("Secret123", storedHash));
    }

    // ---------------------------------------------------------------- session status

    private static SessionRow Row(DateOnly date, TimeOnly end, int present = 0, int absent = 0) => new()
    {
        SessionId = 1,
        SessionDate = date,
        StartTime = end.AddHours(-1),
        EndTime = end,
        PresentCount = present,
        AbsentCount = absent
    };

    [Fact]
    public void SessionStatus_is_completed_when_attendance_was_recorded()
    {
        var today = new DateOnly(2026, 10, 16);

        Assert.Equal("Completed", Row(today.AddDays(-3), new TimeOnly(10, 0), present: 3).StatusOf(today, new TimeOnly(9, 0)));
    }

    [Fact]
    public void SessionStatus_is_pending_for_a_finished_session_without_attendance()
    {
        var today = new DateOnly(2026, 10, 16);

        Assert.Equal("Pending", Row(today.AddDays(-1), new TimeOnly(10, 0)).StatusOf(today, new TimeOnly(9, 0)));
        Assert.Equal("Pending", Row(today, new TimeOnly(9, 0)).StatusOf(today, new TimeOnly(10, 0)));
    }

    [Fact]
    public void SessionStatus_is_upcoming_until_the_session_has_ended()
    {
        var today = new DateOnly(2026, 10, 16);

        Assert.Equal("Upcoming", Row(today, new TimeOnly(15, 0)).StatusOf(today, new TimeOnly(14, 30)));
        Assert.Equal("Upcoming", Row(today.AddDays(2), new TimeOnly(9, 0)).StatusOf(today, new TimeOnly(23, 0)));
    }
}
