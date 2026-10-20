using System.Net;
using System.Net.Http.Json;
using Lpms.Api.Dtos;

namespace Lpms.Api.Tests;

/// <summary>Read-only session endpoints (this class never changes the data).</summary>
public class SessionReadTests : IClassFixture<LpmsApiFactory>
{
    private readonly LpmsApiFactory _factory;

    public SessionReadTests(LpmsApiFactory factory)
    {
        _factory = factory;
    }

    [Fact]
    public async Task Mine_without_token_is_unauthorized()
    {
        var response = await _factory.CreateClient().GetAsync("/api/sessions/mine");

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task Mine_is_forbidden_for_a_coordinator()
    {
        var client = await _factory.CreateClientForAsync("coord1");

        var response = await client.GetAsync("/api/sessions/mine");

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task Mine_upcoming_returns_only_own_sessions_from_today_in_time_order()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var sessions = await client.GetFromJsonAsync<List<SessionSummaryDto>>("/api/sessions/mine?scope=upcoming");

        Assert.Equal(new[] { 7, 2, 3 }, sessions!.Select(s => s.SessionId).ToArray());
        Assert.Equal("Pending", sessions[0].Status);      // 08:00-09:00 today, finished, no attendance
        Assert.Equal("Upcoming", sessions[1].Status);     // 14:00 today
        Assert.True(sessions[0].IsToday);
        Assert.False(sessions[2].IsToday);
        Assert.All(sessions, s => Assert.Equal("Soweto Primary", s.SchoolName));
        Assert.All(sessions, s => Assert.Equal(3, s.TotalLearners));
    }

    [Fact]
    public async Task Mine_past_returns_older_sessions_newest_first()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var sessions = await client.GetFromJsonAsync<List<SessionSummaryDto>>("/api/sessions/mine?scope=past");

        Assert.Equal(new[] { 1, 6 }, sessions!.Select(s => s.SessionId).ToArray());
        Assert.Equal("Pending", sessions[0].Status);
        Assert.Equal("Completed", sessions[1].Status);
        Assert.Equal(2, sessions[1].PresentCount);
        Assert.Equal(1, sessions[1].AbsentCount);
    }

    [Fact]
    public async Task Mine_all_returns_every_own_session()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var sessions = await client.GetFromJsonAsync<List<SessionSummaryDto>>("/api/sessions/mine?scope=all");

        Assert.Equal(new[] { 6, 1, 7, 2, 3 }, sessions!.Select(s => s.SessionId).ToArray());
    }

    [Fact]
    public async Task Mine_with_an_unknown_scope_is_bad_request()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.GetAsync("/api/sessions/mine?scope=tomorrow");

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task GetById_returns_the_roster_with_recorded_statuses()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var detail = await client.GetFromJsonAsync<SessionDetailDto>("/api/sessions/6");

        Assert.Equal(new[] { 1, 2, 3 }, detail!.Learners.Select(l => l.LearnerId).ToArray());
        Assert.Equal(new string?[] { "Present", "Present", "Absent" }, detail.Learners.Select(l => l.Status).ToArray());
        Assert.True(detail.Session.AttendanceRecorded);
    }

    [Fact]
    public async Task GetById_before_attendance_has_null_statuses()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var detail = await client.GetFromJsonAsync<SessionDetailDto>("/api/sessions/2");

        Assert.All(detail!.Learners, l => Assert.Null(l.Status));
        Assert.False(detail.Session.AttendanceRecorded);
    }

    [Fact]
    public async Task GetById_for_another_teachers_session_is_forbidden()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.GetAsync("/api/sessions/4");

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task GetById_for_an_unknown_session_is_not_found()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.GetAsync("/api/sessions/9999");

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
    }

    [Fact]
    public async Task GetById_lets_a_coordinator_view_any_session()
    {
        var client = await _factory.CreateClientForAsync("coord1");

        var response = await client.GetAsync("/api/sessions/4");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }
}
