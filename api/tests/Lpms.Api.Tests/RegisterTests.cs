using System.Net;
using System.Net.Http.Json;
using Lpms.Api.Dtos;

namespace Lpms.Api.Tests;

/// <summary>Create account (teacher self-registration) and the public school list.</summary>
public class RegisterTests : IClassFixture<LpmsApiFactory>
{
    private readonly LpmsApiFactory _factory;

    public RegisterTests(LpmsApiFactory factory)
    {
        _factory = factory;
    }

    private static RegisterRequest Request(string email = "new.teacher@read.org.za", string name = "New Teacher",
        int schoolId = 1, string password = "Passw0rd!") =>
        new() { FullName = name, Email = email, SchoolId = schoolId, Password = password };

    [Fact]
    public async Task Schools_are_listed_without_signing_in()
    {
        var client = _factory.CreateClient();

        var response = await client.GetAsync("/api/auth/schools");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var schools = await response.Content.ReadFromJsonAsync<List<SchoolOptionDto>>();
        Assert.Equal(2, schools!.Count);
        Assert.Equal("Soweto Primary", schools[0].SchoolName);
    }

    [Fact]
    public async Task Register_creates_a_teacher_and_signs_them_in()
    {
        var client = _factory.CreateClient();

        var response = await client.PostAsJsonAsync("/api/auth/register", Request("sipho.mokoena@read.org.za", "Sipho Mokoena"));

        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
        var body = await response.Content.ReadFromJsonAsync<LoginResponse>();
        Assert.False(string.IsNullOrWhiteSpace(body!.Token));
        Assert.Equal("Teacher", body.User.Role);
        Assert.Equal("Sipho Mokoena", body.User.FullName);
        Assert.Equal("sipho.mokoena", body.User.Username);
        Assert.Equal("Soweto Primary", body.User.SchoolName);
        Assert.NotNull(body.User.TeacherId);

        // The new password works for a normal sign-in.
        var login = await client.PostAsJsonAsync("/api/auth/login",
            new LoginRequest { Identifier = "sipho.mokoena@read.org.za", Password = "Passw0rd!" });
        Assert.Equal(HttpStatusCode.OK, login.StatusCode);
    }

    [Fact]
    public async Task Register_rejects_an_email_that_is_already_used()
    {
        var client = _factory.CreateClient();

        var response = await client.PostAsJsonAsync("/api/auth/register", Request("nomsa.zulu@read.org.za"));

        Assert.Equal(HttpStatusCode.Conflict, response.StatusCode);
    }

    [Fact]
    public async Task Register_rejects_a_weak_password()
    {
        var client = _factory.CreateClient();

        var response = await client.PostAsJsonAsync("/api/auth/register", Request("weak@read.org.za", password: "password"));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
        Assert.Contains("password must", await response.Content.ReadAsStringAsync());
    }

    [Fact]
    public async Task Register_needs_a_first_and_last_name()
    {
        var client = _factory.CreateClient();

        var response = await client.PostAsJsonAsync("/api/auth/register", Request("one@read.org.za", name: "Sipho"));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task Register_rejects_an_invalid_email()
    {
        var client = _factory.CreateClient();

        var response = await client.PostAsJsonAsync("/api/auth/register", Request("not-an-email"));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task Register_rejects_an_unknown_school()
    {
        var client = _factory.CreateClient();

        var response = await client.PostAsJsonAsync("/api/auth/register", Request("ghost@read.org.za", schoolId: 99));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }
}
