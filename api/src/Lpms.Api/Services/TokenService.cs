using System.Globalization;
using System.IdentityModel.Tokens.Jwt;
using System.Security.Claims;
using System.Text;
using Lpms.Api.Models;
using Lpms.Api.Options;
using Microsoft.Extensions.Options;
using Microsoft.IdentityModel.Tokens;

namespace Lpms.Api.Services;

public interface ITokenService
{
    (string Token, DateTime ExpiresAtUtc) CreateToken(UserAccount user);
}

/// <summary>Creates signed JWTs (HS256) for authenticated users.</summary>
public sealed class TokenService : ITokenService
{
    private readonly JwtOptions _options;

    public TokenService(IOptions<JwtOptions> options)
    {
        _options = options.Value;
    }

    public (string Token, DateTime ExpiresAtUtc) CreateToken(UserAccount user)
    {
        var now = DateTime.UtcNow;
        var expires = now.AddMinutes(_options.ExpiryMinutes);

        var claims = new List<Claim>
        {
            new(ClaimTypes.NameIdentifier, user.UserId.ToString(CultureInfo.InvariantCulture)),
            new(ClaimTypes.Name, user.Username),
            new(ClaimTypes.Role, user.Role),
            new(UserClaims.FullName, user.FullName)
        };
        if (user.TeacherId is int teacherId)
            claims.Add(new Claim(UserClaims.TeacherId, teacherId.ToString(CultureInfo.InvariantCulture)));
        if (user.Teacher is not null)
            claims.Add(new Claim(UserClaims.SchoolId, user.Teacher.SchoolId.ToString(CultureInfo.InvariantCulture)));

        var key = new SymmetricSecurityKey(Encoding.UTF8.GetBytes(_options.Key));
        var credentials = new SigningCredentials(key, SecurityAlgorithms.HmacSha256);
        var token = new JwtSecurityToken(
            issuer: _options.Issuer,
            audience: _options.Audience,
            claims: claims,
            notBefore: now,
            expires: expires,
            signingCredentials: credentials);

        return (new JwtSecurityTokenHandler().WriteToken(token), expires);
    }
}
