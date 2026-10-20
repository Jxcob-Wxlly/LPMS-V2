namespace Lpms.Api.Options;

/// <summary>Settings for the JSON Web Tokens issued by POST /api/auth/login (section "Jwt").</summary>
public class JwtOptions
{
    public const string SectionName = "Jwt";

    public string Issuer { get; set; } = "lpms-api";
    public string Audience { get; set; } = "lpms-teacher-app";

    /// <summary>Signing secret, at least 32 characters. Set it as an environment variable / App Service setting (Jwt__Key); never commit a real key.</summary>
    public string Key { get; set; } = string.Empty;

    public int ExpiryMinutes { get; set; } = 480;
}
