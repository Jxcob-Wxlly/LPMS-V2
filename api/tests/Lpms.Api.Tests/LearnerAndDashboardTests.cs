using System.Net;
using System.Net.Http.Json;
using Lpms.Api.Dtos;

namespace Lpms.Api.Tests;

/// <summary>Read-only endpoints: learners, learner progress, dashboard, lookups and health.</summary>
public class LearnerAndDashboardTests : IClassFixture<LpmsApiFactory>
{
    private readonly LpmsApiFactory _factory;

    public LearnerAndDashboardTests(LpmsApiFactory factory)
    {
        _factory = factory;
    }

    // ---------------------------------------------------------------- learners

    [Fact]
    public async Task Learners_for_a_teacher_are_limited_to_the_teachers_school()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        // The schoolId parameter is ignored for teachers.
        var learners = await client.GetFromJsonAsync<List<LearnerSummaryDto>>("/api/learners?schoolId=2");

        Assert.Equal(new[] { 1, 2, 3 }, learners!.Select(l => l.LearnerId).ToArray());
    }

    [Fact]
    public async Task Learners_include_average_score_attendance_rate_and_assessment_count()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var learners = await client.GetFromJsonAsync<List<LearnerSummaryDto>>("/api/learners");

        var amahle = learners!.Single(l => l.LearnerId == 1);
        Assert.Equal(70.0, amahle.AverageScore);
        Assert.Equal(2, amahle.AssessmentCount);
        Assert.Equal(100.0, amahle.AttendanceRate);

        var cebo = learners!.Single(l => l.LearnerId == 3);
        Assert.Null(cebo.AverageScore);
        Assert.Equal(0, cebo.AssessmentCount);
        Assert.Equal(0.0, cebo.AttendanceRate);
    }

    [Fact]
    public async Task Learners_can_be_searched_by_name()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var learners = await client.GetFromJsonAsync<List<LearnerSummaryDto>>("/api/learners?search=Amah");

        Assert.Equal(new[] { 1 }, learners!.Select(l => l.LearnerId).ToArray());
    }

    [Fact]
    public async Task Learners_for_a_coordinator_can_be_filtered_by_school()
    {
        var client = await _factory.CreateClientForAsync("coord1");

        var learners = await client.GetFromJsonAsync<List<LearnerSummaryDto>>("/api/learners?schoolId=2");

        Assert.Equal(new[] { 4, 5 }, learners!.Select(l => l.LearnerId).OrderBy(id => id).ToArray());
    }

    [Fact]
    public async Task Learners_without_token_is_unauthorized()
    {
        var response = await _factory.CreateClient().GetAsync("/api/learners");

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    [Fact]
    public async Task Learner_progress_lists_assessments_newest_first_with_a_label()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var progress = await client.GetFromJsonAsync<LearnerProgressDto>("/api/learners/1/progress");

        Assert.Equal(70.0, progress!.Learner.AverageScore);
        Assert.Equal("Good progress", progress.ProgressLabel);
        Assert.Equal(new[] { 2, 1 }, progress.Assessments.Select(a => a.ProgressId).ToArray());
        Assert.All(progress.Assessments, a => Assert.True(a.CanEdit));
        var attendance = Assert.Single(progress.Attendance);
        Assert.Equal("Present", attendance.Status);
    }

    [Fact]
    public async Task Learner_progress_marks_other_teachers_assessments_as_read_only()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var progress = await client.GetFromJsonAsync<LearnerProgressDto>("/api/learners/2/progress");

        var assessment = Assert.Single(progress!.Assessments);
        Assert.False(assessment.CanEdit);
        Assert.Equal("Lindiwe Dube", assessment.RecordedBy);
        Assert.Equal("Needs support", progress.ProgressLabel);
    }

    [Fact]
    public async Task Learner_progress_for_another_school_is_forbidden()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.GetAsync("/api/learners/4/progress");

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task Learner_progress_for_an_unknown_learner_is_not_found()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.GetAsync("/api/learners/9999/progress");

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
    }

    // ---------------------------------------------------------------- dashboard

    [Fact]
    public async Task Dashboard_summarises_the_teachers_week()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var dashboard = await client.GetFromJsonAsync<TeacherDashboardDto>("/api/dashboard/teacher");

        Assert.Equal("Nomsa Zulu", dashboard!.TeacherName);
        Assert.Equal("Soweto Primary", dashboard.SchoolName);
        Assert.Equal(3, dashboard.SessionsThisWeek);      // sessions 1, 2 and 7 (Mon 12 - Sun 18 Oct)
        Assert.Equal(2, dashboard.SessionsToday);         // sessions 7 and 2
        Assert.Equal(3, dashboard.TotalLearners);
        Assert.Equal(2, dashboard.PendingAttendance);     // sessions 1 and 7
        Assert.Equal(66.7, dashboard.AttendanceRate);     // session 6: 2 present, 1 absent
        Assert.Equal(new[] { 7, 2 }, dashboard.TodaySessions.Select(s => s.SessionId).ToArray());
    }

    [Fact]
    public async Task Dashboard_is_forbidden_for_a_coordinator()
    {
        var client = await _factory.CreateClientForAsync("coord1");

        var response = await client.GetAsync("/api/dashboard/teacher");

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    // ---------------------------------------------------------------- lookups and health

    [Fact]
    public async Task Schools_are_listed_alphabetically_for_any_signed_in_user()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var schools = await client.GetFromJsonAsync<List<SchoolDto>>("/api/schools");

        Assert.Equal(new[] { "Soweto Primary", "Tembisa High" }, schools!.Select(s => s.SchoolName).ToArray());
    }

    [Fact]
    public async Task Programmes_are_listed()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var programmes = await client.GetFromJsonAsync<List<ProgrammeDto>>("/api/programmes");

        Assert.Equal("Literacy Boost", Assert.Single(programmes!).ProgrammeName);
    }

    [Fact]
    public async Task Health_is_public_and_reports_the_database()
    {
        var response = await _factory.CreateClient().GetAsync("/api/health");

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var health = await response.Content.ReadFromJsonAsync<HealthDto>();
        Assert.Equal("Healthy", health!.Status);
        Assert.Equal("Connected", health.Database);
    }
}
