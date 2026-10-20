namespace Lpms.Api.Services;

/// <summary>BCrypt password hashing. Plain-text passwords are never stored or logged.</summary>
public static class PasswordService
{
    public const int DefaultWorkFactor = 11;

    // A valid BCrypt hash of a random throw-away password. It is verified when the user does not
    // exist so that "unknown user" and "wrong password" take about the same time.
    private const string DummyHash = "$2a$11$MpcOARMR53.6UhqA7XA/8OCwKxGNsAzzLHqr8EWCnw2ut61AD.cU2";

    public static string Hash(string password, int workFactor = DefaultWorkFactor) =>
        BCrypt.Net.BCrypt.HashPassword(password, workFactor);

    /// <summary>Returns false (instead of throwing) when the stored hash is malformed.</summary>
    public static bool Verify(string password, string? hash)
    {
        if (string.IsNullOrEmpty(hash))
            return false;
        try
        {
            return BCrypt.Net.BCrypt.Verify(password, hash);
        }
        catch (Exception)
        {
            return false;
        }
    }

    public static void BurnTime(string password) => Verify(password, DummyHash);
}
