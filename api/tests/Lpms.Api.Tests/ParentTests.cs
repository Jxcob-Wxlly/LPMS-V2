using System.Net;
using System.Net.Http.Json;
using Lpms.Api.Dtos;

namespace Lpms.Api.Tests;

/// <summary>The parent / guardian role: sees only its own children and nothing else.</summary>
public class ParentTests : IClassFixture<LpmsApiFactory>
{
    private readonly LpmsApiFactory _factory;

    public ParentTests(LpmsApiFactory factory)
    {
        _factory = factory;
    }

    [Fact]
    public async Task Parent_can_sign_in_and_profile_has_the_parent_role()
    {
        var client = await _factory.CreateClientForAsync("parent1");

        var me = await client.GetFromJsonAsync<UserProfileDto>("/api/auth/me");

        Assert.Equal("Parent", me!.Role);
        Assert.Null(me.TeacherId);
        Assert.Null(me.SchoolId);
    }

    [Fact]
    public async Task Children_returns_only_the_parents_own_children_with_figures()
    {
        var client = await _factory.CreateClientForAsync("parent1");

        var children = await client.GetFromJsonAsync<List<LearnerSummaryDto>>("/api/parent/children");

        Assert.Equal(new[] { 1, 2 }, children!.Select(c => c.LearnerId).ToArray());
        var amahle = children!.Single(c => c.LearnerId == 1);
        Assert.Equal(70.0, amahle.AverageScore);
        Assert.Equal(2, amahle.AssessmentCount);
    }

    [Fact]
    public async Task A_second_parent_sees_a_different_child()
    {
        var client = await _factory.CreateClientForAsync("parent2");

        var children = await client.GetFromJsonAsync<List<LearnerSummaryDto>>("/api/parent/children");

        Assert.Equal(new[] { 3 }, children!.Select(c => c.LearnerId).ToArray());
    }

    [Fact]
    public async Task Children_requires_a_token()
    {
        var response = await _factory.CreateClient().GetAsync("/api/parent/children");

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Theory]
    [InlineData("teacher1")]
    [InlineData("coord1")]
    [InlineData("admin1")]
    public async Task Children_is_forbidden_for_staff(string username)
    {
        var client = await _factory.CreateClientForAsync(username);

        var response = await client.GetAsync("/api/parent/children");

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task Parent_can_open_the_report_of_their_own_child()
    {
        var client = await _factory.CreateClientForAsync("parent1");

        var report = await client.GetFromJsonAsync<LearnerProgressDto>("/api/learners/1/progress");

        Assert.Equal(1, report!.Learner.LearnerId);
        Assert.Equal(2, report.Assessments.Count);
        Assert.Single(report.Attendance);
    }

    [Fact]
    public async Task Parent_report_marks_every_assessment_read_only()
    {
        var client = await _factory.CreateClientForAsync("parent1");

        var report = await client.GetFromJsonAsync<LearnerProgressDto>("/api/learners/1/progress");

        Assert.All(report!.Assessments, a => Assert.False(a.CanEdit));
    }

    [Fact]
    public async Task Parent_cannot_open_the_report_of_another_parents_child()
    {
        var client = await _factory.CreateClientForAsync("parent1");

        var response = await client.GetAsync("/api/learners/3/progress");

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task Parent_cannot_open_a_child_from_another_school()
    {
        var client = await _factory.CreateClientForAsync("parent1");

        var response = await client.GetAsync("/api/learners/4/progress");

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task Parent_cannot_list_all_learners()
    {
        var client = await _factory.CreateClientForAsync("parent1");

        var response = await client.GetAsync("/api/learners");

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task Parent_cannot_open_a_session_roster()
    {
        var client = await _factory.CreateClientForAsync("parent1");

        var response = await client.GetAsync("/api/sessions/6");

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task Parent_cannot_use_teacher_endpoints()
    {
        var client = await _factory.CreateClientForAsync("parent1");

        Assert.Equal(HttpStatusCode.Forbidden, (await client.GetAsync("/api/sessions/mine")).StatusCode);
        Assert.Equal(HttpStatusCode.Forbidden, (await client.GetAsync("/api/dashboard/teacher")).StatusCode);
        var post = await client.PostAsJsonAsync("/api/progress",
            new { learnerId = 1, assessmentScore = 50, assessmentDate = "2026-10-01" });
        Assert.Equal(HttpStatusCode.Forbidden, post.StatusCode);
    }

    [Fact]
    public async Task Teacher_still_sees_the_report_of_a_child_without_a_parent_link()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.GetAsync("/api/learners/3/progress");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
    }
}
