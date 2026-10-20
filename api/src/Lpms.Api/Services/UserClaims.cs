using System.Security.Claims;

namespace Lpms.Api.Services;

/// <summary>Names of the custom claims placed in the JWT, and helpers to read them.</summary>
public static class UserClaims
{
    public const string FullName = "fullName";
    public const string TeacherId = "teacherId";
    public const string SchoolId = "schoolId";

    public static int? GetUserId(this ClaimsPrincipal user) => ReadInt(user, ClaimTypes.NameIdentifier);

    public static int? GetTeacherId(this ClaimsPrincipal user) => ReadInt(user, TeacherId);

    public static int? GetSchoolId(this ClaimsPrincipal user) => ReadInt(user, SchoolId);

    private static int? ReadInt(ClaimsPrincipal user, string claimType)
    {
        var value = user.FindFirst(claimType)?.Value;
        return int.TryParse(value, out var number) ? number : null;
    }
}
