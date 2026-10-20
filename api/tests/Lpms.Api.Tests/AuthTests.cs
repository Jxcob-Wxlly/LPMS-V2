using System.Net;
using System.Net.Http.Json;
using Lpms.Api.Dtos;

namespace Lpms.Api.Tests;

/// <summary>Sign-in, current user and change-password endpoints.</summary>
public class AuthTests : IClassFixture<LpmsApiFactory>
{
    private readonly LpmsApiFactory _factory;

    public AuthTests(LpmsApiFactory factory)
    {
        _factory = factory;
    }

    private static LoginRequest Login(string identifier, string password) =>
        new() { Identifier = identifier, Password = password };

    [Fact]
    public async Task Login_with_username_returns_token_and_profile()
    {
        var client = _factory.CreateClient();

        var response = await client.PostAsJsonAsync("/api/auth/login", Login("teacher1", LpmsApiFactory.Password));

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var body = await response.Content.ReadFromJsonAsync<LoginResponse>();
        Assert.False(string.IsNullOrWhiteSpace(body!.Token));
        Assert.Equal("Teacher", body.User.Role);
        Assert.Equal(1, body.User.TeacherId);
        Assert.Equal(1, body.User.SchoolId);
        Assert.Equal("Soweto Primary", body.User.SchoolName);
        Assert.True(body.ExpiresAtUtc > DateTime.UtcNow);
    }

    [Fact]
    public async Task Login_with_email_ignores_letter_case()
    {
        var client = _factory.CreateClient();

        var response = await client.PostAsJsonAsync("/api/auth/login", Login("NOMSA@read.org.za", LpmsApiFactory.Password));

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }

    [Fact]
    public async Task Login_with_wrong_password_is_unauthorized()
    {
        var client = _factory.CreateClient();

        var response = await client.PostAsJsonAsync("/api/auth/login", Login("teacher1", "not-the-password"));

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task Login_with_unknown_user_is_unauthorized()
    {
        var client = _factory.CreateClient();

        var response = await client.PostAsJsonAsync("/api/auth/login", Login("nobody", LpmsApiFactory.Password));

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task Login_with_empty_password_is_bad_request()
    {
        var client = _factory.CreateClient();

        var response = await client.PostAsJsonAsync("/api/auth/login", Login("teacher1", ""));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task Login_for_inactive_account_is_forbidden()
    {
        var client = _factory.CreateClient();

        var response = await client.PostAsJsonAsync("/api/auth/login", Login("inactive1", LpmsApiFactory.Password));

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task Me_without_token_is_unauthorized()
    {
        var client = _factory.CreateClient();

        var response = await client.GetAsync("/api/auth/me");

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task Me_with_garbage_token_is_unauthorized()
    {
        var client = _factory.CreateClient();
        client.DefaultRequestHeaders.Authorization =
            new System.Net.Http.Headers.AuthenticationHeaderValue("Bearer", "this.is.not-a-token");

        var response = await client.GetAsync("/api/auth/me");

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task Me_returns_the_signed_in_profile()
    {
        var client = await _factory.CreateClientForAsync("teacher2");

        var profile = await client.GetFromJsonAsync<UserProfileDto>("/api/auth/me");

        Assert.Equal("teacher2", profile!.Username);
        Assert.Equal("Lindiwe Dube", profile.FullName);
        Assert.Equal(2, profile.TeacherId);
    }

    [Fact]
    public async Task ChangePassword_rejects_a_wrong_current_password()
    {
        var client = await _factory.CreateClientForAsync("admin1");

        var response = await client.PostAsJsonAsync("/api/auth/change-password",
            new ChangePasswordRequest { CurrentPassword = "wrong", NewPassword = "Brand-New1" });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Theory]
    [InlineData("short1A")]
    [InlineData("alllowercase1")]
    [InlineData("ALLUPPERCASE1")]
    [InlineData("NoDigitsHere")]
    public async Task ChangePassword_rejects_weak_new_passwords(string weakPassword)
    {
        var client = await _factory.CreateClientForAsync("admin1");

        var response = await client.PostAsJsonAsync("/api/auth/change-password",
            new ChangePasswordRequest { CurrentPassword = LpmsApiFactory.Password, NewPassword = weakPassword });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task ChangePassword_with_valid_input_replaces_the_password()
    {
        var client = await _factory.CreateClientForAsync("pwchange");

        var change = await client.PostAsJsonAsync("/api/auth/change-password",
            new ChangePasswordRequest { CurrentPassword = LpmsApiFactory.Password, NewPassword = "Brand-New1" });
        Assert.Equal(HttpStatusCode.NoContent, change.StatusCode);

        var anonymous = _factory.CreateClient();
        var oldLogin = await anonymous.PostAsJsonAsync("/api/auth/login", Login("pwchange", LpmsApiFactory.Password));
        var newLogin = await anonymous.PostAsJsonAsync("/api/auth/login", Login("pwchange", "Brand-New1"));

        Assert.Equal(HttpStatusCode.Unauthorized, oldLogin.StatusCode);
        Assert.Equal(HttpStatusCode.OK, newLogin.StatusCode);
    }
}
