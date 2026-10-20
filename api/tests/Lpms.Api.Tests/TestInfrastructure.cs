using System.Net.Http.Headers;
using System.Net.Http.Json;
using Lpms.Api.Data;
using Lpms.Api.Dtos;
using Lpms.Api.Models;
using Lpms.Api.Services;
using Microsoft.AspNetCore.Hosting;
using Microsoft.AspNetCore.Mvc.Testing;
using Microsoft.Data.Sqlite;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.DependencyInjection.Extensions;
using Microsoft.Extensions.Hosting;

namespace Lpms.Api.Tests;

/// <summary>A clock the tests can control: "now" is Friday 16 October 2026, 10:00.</summary>
public sealed class FakeClock : IClock
{
    public DateTime Now { get; set; } = new DateTime(2026, 10, 16, 10, 0, 0);
}

/// <summary>
/// Starts the real API in memory, but on a SQLite in-memory database filled with a small, known data set,
/// so the tests exercise the real controllers, validation, authentication and authorisation.
/// </summary>
public sealed class LpmsApiFactory : WebApplicationFactory<Program>
{
    public const string Password = "Passw0rd!";

    private readonly SqliteConnection _connection = new("DataSource=:memory:");

    public FakeClock Clock { get; } = new();

    protected override void ConfigureWebHost(IWebHostBuilder builder)
    {
        builder.UseEnvironment("Development");

        builder.ConfigureAppConfiguration((_, configuration) =>
        {
            configuration.AddInMemoryCollection(new Dictionary<string, string?>
            {
                ["Jwt:Key"] = "unit-test-signing-key-which-is-long-enough-123456",
                ["Jwt:Issuer"] = "lpms-tests",
                ["Jwt:Audience"] = "lpms-tests",
                ["RateLimiting:LoginPermitLimit"] = "10000",
                ["ConnectionStrings:LpmsDb"] = "unused-in-tests"
            });
        });

        builder.ConfigureServices(services =>
        {
            // Swap SQL Server for the in-memory SQLite database.
            services.RemoveAll<DbContextOptions<LpmsDbContext>>();
            _connection.Open();
            services.AddDbContext<LpmsDbContext>(options => options.UseSqlite(_connection));

            // Swap the real clock for the fake one.
            services.RemoveAll<IClock>();
            services.AddSingleton<IClock>(Clock);
        });
    }

    protected override IHost CreateHost(IHostBuilder builder)
    {
        var host = base.CreateHost(builder);
        using var scope = host.Services.CreateScope();
        var db = scope.ServiceProvider.GetRequiredService<LpmsDbContext>();
        db.Database.EnsureCreated();
        TestData.Seed(db);
        return host;
    }

    protected override void Dispose(bool disposing)
    {
        base.Dispose(disposing);
        if (disposing)
            _connection.Dispose();
    }

    /// <summary>Signs in through the real endpoint and returns a client that sends the token.</summary>
    public async Task<HttpClient> CreateClientForAsync(string username, string password = Password)
    {
        var client = CreateClient();
        var response = await client.PostAsJsonAsync("/api/auth/login",
            new LoginRequest { Identifier = username, Password = password });
        response.EnsureSuccessStatusCode();
        var login = await response.Content.ReadFromJsonAsync<LoginResponse>();
        client.DefaultRequestHeaders.Authorization = new AuthenticationHeaderValue("Bearer", login!.Token);
        return client;
    }
}

/// <summary>
/// Known data used by every test.
///   School 1: teachers 1 (teacher1) and 2 (teacher2), learners 1-3.   School 2: teacher 3 (teacher3), learners 4-5.
///   Fake "today" is 2026-10-16 10:00.
///   Sessions: 1 = teacher1 yesterday (no attendance), 2 = teacher1 today 14:00, 3 = teacher1 in 3 days,
///             4 = teacher2 today, 5 = teacher3 today (school 2), 6 = teacher1 last week (attendance recorded),
///             7 = teacher1 today 08:00-09:00 (already finished, no attendance)
/// </summary>
public static class TestData
{
    public static readonly DateOnly Today = new(2026, 10, 16);

    public static void Seed(LpmsDbContext db)
    {
        var hash = PasswordService.Hash(LpmsApiFactory.Password, workFactor: 4);

        db.Schools.AddRange(
            new School { SchoolId = 1, SchoolName = "Soweto Primary", Address = "123 Main Rd", PrincipalName = "Ms Dlamini", ContactNumber = "011-555-0001" },
            new School { SchoolId = 2, SchoolName = "Tembisa High", Address = "45 Oak Ave", PrincipalName = "Mr Khumalo", ContactNumber = "011-555-0002" });

        db.Programmes.Add(new Programme
        {
            ProgrammeId = 1, ProgrammeName = "Literacy Boost", Description = "Foundation literacy",
            StartDate = Today.AddDays(-200), EndDate = Today.AddDays(100)
        });

        db.Teachers.AddRange(
            new Teacher { TeacherId = 1, FirstName = "Nomsa", LastName = "Zulu", SchoolId = 1, ContactEmail = "nomsa.zulu@read.org.za" },
            new Teacher { TeacherId = 2, FirstName = "Lindiwe", LastName = "Dube", SchoolId = 1, ContactEmail = "lindiwe.dube@read.org.za" },
            new Teacher { TeacherId = 3, FirstName = "Themba", LastName = "Nkosi", SchoolId = 2, ContactEmail = "themba.nkosi@read.org.za" });

        db.Learners.AddRange(
            new Learner { LearnerId = 1, FirstName = "Amahle", LastName = "Dlamini", Grade = "Grade 3", SchoolId = 1 },
            new Learner { LearnerId = 2, FirstName = "Bongani", LastName = "Khumalo", Grade = "Grade 4", SchoolId = 1 },
            new Learner { LearnerId = 3, FirstName = "Cebo", LastName = "Mthethwa", Grade = "Grade 3", SchoolId = 1 },
            new Learner { LearnerId = 4, FirstName = "Dineo", LastName = "Molefe", Grade = "Grade 2", SchoolId = 2 },
            new Learner { LearnerId = 5, FirstName = "Ethan", LastName = "Naidoo", Grade = "Grade 3", SchoolId = 2 });

        db.Users.AddRange(
            new UserAccount { UserId = 1, Username = "admin1", PasswordHash = hash, Role = Roles.Administrator, FullName = "Admin One", Email = "admin1@read.org.za" },
            new UserAccount { UserId = 2, Username = "coord1", PasswordHash = hash, Role = Roles.Coordinator, FullName = "Coord One", Email = "coord1@read.org.za" },
            new UserAccount { UserId = 3, Username = "teacher1", PasswordHash = hash, Role = Roles.Teacher, FullName = "Nomsa Zulu", Email = "nomsa@read.org.za", TeacherId = 1 },
            new UserAccount { UserId = 4, Username = "teacher2", PasswordHash = hash, Role = Roles.Teacher, FullName = "Lindiwe Dube", Email = "lindiwe@read.org.za", TeacherId = 2 },
            new UserAccount { UserId = 5, Username = "teacher3", PasswordHash = hash, Role = Roles.Teacher, FullName = "Themba Nkosi", Email = "themba@read.org.za", TeacherId = 3 },
            new UserAccount { UserId = 6, Username = "inactive1", PasswordHash = hash, Role = Roles.Administrator, FullName = "Inactive User", Email = "inactive1@read.org.za", IsActive = false },
            new UserAccount { UserId = 7, Username = "pwchange", PasswordHash = hash, Role = Roles.Administrator, FullName = "Password Changer", Email = "pwchange@read.org.za" },
            new UserAccount { UserId = 8, Username = "parent1", PasswordHash = hash, Role = Roles.Parent, FullName = "Zodwa Dlamini", Email = "zodwa@parents.example.org" },
            new UserAccount { UserId = 9, Username = "parent2", PasswordHash = hash, Role = Roles.Parent, FullName = "Sipho Mthethwa", Email = "sipho@parents.example.org" });
        db.ParentLearners.AddRange(
            new ParentLearner { ParentLearnerId = 1, UserId = 8, LearnerId = 1, Relationship = "Mother" },
            new ParentLearner { ParentLearnerId = 2, UserId = 8, LearnerId = 2, Relationship = "Mother" },
            new ParentLearner { ParentLearnerId = 3, UserId = 9, LearnerId = 3, Relationship = "Father" });

        db.Sessions.AddRange(
            new Session { SessionId = 1, ProgrammeId = 1, SchoolId = 1, TeacherId = 1, SessionDate = Today.AddDays(-1), StartTime = new TimeOnly(9, 0), EndTime = new TimeOnly(10, 0) },
            new Session { SessionId = 2, ProgrammeId = 1, SchoolId = 1, TeacherId = 1, SessionDate = Today, StartTime = new TimeOnly(14, 0), EndTime = new TimeOnly(15, 0) },
            new Session { SessionId = 3, ProgrammeId = 1, SchoolId = 1, TeacherId = 1, SessionDate = Today.AddDays(3), StartTime = new TimeOnly(9, 0), EndTime = new TimeOnly(10, 0) },
            new Session { SessionId = 4, ProgrammeId = 1, SchoolId = 1, TeacherId = 2, SessionDate = Today, StartTime = new TimeOnly(11, 0), EndTime = new TimeOnly(12, 0) },
            new Session { SessionId = 5, ProgrammeId = 1, SchoolId = 2, TeacherId = 3, SessionDate = Today, StartTime = new TimeOnly(11, 0), EndTime = new TimeOnly(12, 0) },
            new Session { SessionId = 6, ProgrammeId = 1, SchoolId = 1, TeacherId = 1, SessionDate = Today.AddDays(-7), StartTime = new TimeOnly(9, 0), EndTime = new TimeOnly(10, 0) },
            new Session { SessionId = 7, ProgrammeId = 1, SchoolId = 1, TeacherId = 1, SessionDate = Today, StartTime = new TimeOnly(8, 0), EndTime = new TimeOnly(9, 0) });

        // Session 6 (last week) already has attendance: learners 1 and 2 present, learner 3 absent.
        db.AttendanceRecords.AddRange(
            new AttendanceRecord { AttendanceId = 1, SessionId = 6, LearnerId = 1, AttendanceStatus = AttendanceStatuses.Present },
            new AttendanceRecord { AttendanceId = 2, SessionId = 6, LearnerId = 2, AttendanceStatus = AttendanceStatuses.Present },
            new AttendanceRecord { AttendanceId = 3, SessionId = 6, LearnerId = 3, AttendanceStatus = AttendanceStatuses.Absent });

        // Learner 1 has two assessments by teacher 1 (average 70); learner 2 has one by teacher 2.
        db.ProgressRecords.AddRange(
            new ProgressRecord { ProgressId = 1, LearnerId = 1, TeacherId = 1, AssessmentScore = 60m, AssessmentDate = Today.AddDays(-20), Notes = "Phonics test" },
            new ProgressRecord { ProgressId = 2, LearnerId = 1, TeacherId = 1, AssessmentScore = 80m, AssessmentDate = Today.AddDays(-5), Notes = "Reading comprehension" },
            new ProgressRecord { ProgressId = 3, LearnerId = 2, TeacherId = 2, AssessmentScore = 45m, AssessmentDate = Today.AddDays(-5), Notes = "Spelling test" });

        db.SaveChanges();
    }
}
