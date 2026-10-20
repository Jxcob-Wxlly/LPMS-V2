using System.Net;
using System.Net.Http.Json;
using Lpms.Api.Dtos;

namespace Lpms.Api.Tests;

/// <summary>Adding, editing and deleting assessments (changes data, so it has its own database).</summary>
public class ProgressTests : IClassFixture<LpmsApiFactory>
{
    private readonly LpmsApiFactory _factory;

    public ProgressTests(LpmsApiFactory factory)
    {
        _factory = factory;
    }

    private static object Valid(int learnerId = 3) => new
    {
        learnerId,
        assessmentScore = 72.5,
        assessmentDate = TestData.Today.AddDays(-1),
        notes = "Fluency check"
    };

    [Fact]
    public async Task Create_adds_an_assessment_recorded_by_the_signed_in_teacher()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PostAsJsonAsync("/api/progress", Valid());

        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
        var dto = await response.Content.ReadFromJsonAsync<AssessmentDto>();
        Assert.Equal(72.5m, dto!.AssessmentScore);
        Assert.Equal("Nomsa Zulu", dto.RecordedBy);
        Assert.True(dto.CanEdit);
    }

    [Fact]
    public async Task Create_accepts_the_boundary_scores_0_and_100()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var zero = await client.PostAsJsonAsync("/api/progress",
            new { learnerId = 2, assessmentScore = 0, assessmentDate = TestData.Today });
        var hundred = await client.PostAsJsonAsync("/api/progress",
            new { learnerId = 2, assessmentScore = 100, assessmentDate = TestData.Today });

        Assert.Equal(HttpStatusCode.Created, zero.StatusCode);
        Assert.Equal(HttpStatusCode.Created, hundred.StatusCode);
    }

    [Theory]
    [InlineData(100.5)]
    [InlineData(101)]
    [InlineData(-1)]
    [InlineData(-0.01)]
    public async Task Create_rejects_scores_outside_0_to_100(double score)
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PostAsJsonAsync("/api/progress",
            new { learnerId = 3, assessmentScore = score, assessmentDate = TestData.Today });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task Create_rejects_a_missing_score()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PostAsJsonAsync("/api/progress",
            new { learnerId = 3, assessmentDate = TestData.Today });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task Create_rejects_a_missing_date()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PostAsJsonAsync("/api/progress",
            new { learnerId = 3, assessmentScore = 50 });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task Create_rejects_a_date_in_the_future()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PostAsJsonAsync("/api/progress",
            new { learnerId = 3, assessmentScore = 50, assessmentDate = TestData.Today.AddDays(1) });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task Create_rejects_notes_longer_than_255_characters()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PostAsJsonAsync("/api/progress",
            new { learnerId = 3, assessmentScore = 50, assessmentDate = TestData.Today, notes = new string('x', 256) });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task Create_is_forbidden_for_a_learner_at_another_school()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PostAsJsonAsync("/api/progress", Valid(learnerId: 4));

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task Create_for_an_unknown_learner_is_not_found()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PostAsJsonAsync("/api/progress", Valid(learnerId: 9999));

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
    }

    [Fact]
    public async Task Create_is_forbidden_for_a_coordinator()
    {
        var client = await _factory.CreateClientForAsync("coord1");

        var response = await client.PostAsJsonAsync("/api/progress", Valid());

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task Update_changes_the_teachers_own_assessment()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PutAsJsonAsync("/api/progress/1",
            new { assessmentScore = 66, assessmentDate = TestData.Today.AddDays(-2), notes = "Re-marked" });

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var dto = await response.Content.ReadFromJsonAsync<AssessmentDto>();
        Assert.Equal(66m, dto!.AssessmentScore);
        Assert.Equal("Re-marked", dto.Notes);
    }

    [Fact]
    public async Task Update_is_forbidden_for_another_teachers_assessment()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PutAsJsonAsync("/api/progress/3",
            new { assessmentScore = 66, assessmentDate = TestData.Today.AddDays(-2) });

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task Delete_removes_the_teachers_own_assessment()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.DeleteAsync("/api/progress/2");
        var again = await client.DeleteAsync("/api/progress/2");

        Assert.Equal(HttpStatusCode.NoContent, response.StatusCode);
        Assert.Equal(HttpStatusCode.NotFound, again.StatusCode);
    }

    [Fact]
    public async Task Delete_is_forbidden_for_another_teachers_assessment()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.DeleteAsync("/api/progress/3");

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }
}
