using System.Net;
using System.Net.Http.Json;
using Lpms.Api.Dtos;

namespace Lpms.Api.Tests;

/// <summary>Attendance saving and session scheduling (changes data, so it has its own database).</summary>
public class SessionWriteTests : IClassFixture<LpmsApiFactory>
{
    private readonly LpmsApiFactory _factory;

    public SessionWriteTests(LpmsApiFactory factory)
    {
        _factory = factory;
    }

    private static SaveAttendanceRequest Attendance(params (int learnerId, string status)[] entries) =>
        new() { Entries = entries.Select(e => new AttendanceEntryDto { LearnerId = e.learnerId, Status = e.status }).ToList() };

    // ---------------------------------------------------------------- attendance

    [Fact]
    public async Task SaveAttendance_records_statuses_for_the_teachers_own_session()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PutAsJsonAsync("/api/sessions/1/attendance",
            Attendance((1, "Present"), (2, "Absent"), (3, "Present")));

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var detail = await response.Content.ReadFromJsonAsync<SessionDetailDto>();
        Assert.True(detail!.Session.AttendanceRecorded);
        Assert.Equal("Completed", detail.Session.Status);
        Assert.Equal(2, detail.Session.PresentCount);
        Assert.Equal(1, detail.Session.AbsentCount);
    }

    [Fact]
    public async Task SaveAttendance_corrects_an_existing_record_instead_of_duplicating_it()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PutAsJsonAsync("/api/sessions/6/attendance", Attendance((3, "Present")));

        Assert.Equal(HttpStatusCode.OK, response.StatusCode);
        var detail = await response.Content.ReadFromJsonAsync<SessionDetailDto>();
        Assert.Equal(3, detail!.Session.PresentCount);
        Assert.Equal(0, detail.Session.AbsentCount);
        Assert.Equal(3, detail.Learners.Count(l => l.Status is not null));
    }

    [Fact]
    public async Task SaveAttendance_rejects_an_unknown_status()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PutAsJsonAsync("/api/sessions/2/attendance", Attendance((1, "Late")));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task SaveAttendance_rejects_an_empty_list()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PutAsJsonAsync("/api/sessions/2/attendance", Attendance());

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task SaveAttendance_rejects_the_same_learner_twice()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PutAsJsonAsync("/api/sessions/2/attendance",
            Attendance((1, "Present"), (1, "Absent")));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task SaveAttendance_rejects_a_learner_from_another_school()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PutAsJsonAsync("/api/sessions/2/attendance", Attendance((4, "Present")));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task SaveAttendance_rejects_a_session_that_has_not_happened_yet()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PutAsJsonAsync("/api/sessions/3/attendance", Attendance((1, "Present")));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task SaveAttendance_is_forbidden_for_another_teachers_session()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PutAsJsonAsync("/api/sessions/4/attendance", Attendance((1, "Present")));

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task SaveAttendance_for_an_unknown_session_is_not_found()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PutAsJsonAsync("/api/sessions/9999/attendance", Attendance((1, "Present")));

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
    }

    // ---------------------------------------------------------------- scheduling

    private static CreateSessionRequest NewSession(int teacherId = 2, int schoolId = 1, int dayOffset = 10,
        TimeOnly? start = null, TimeOnly? end = null) =>
        new()
        {
            ProgrammeId = 1,
            SchoolId = schoolId,
            TeacherId = teacherId,
            SessionDate = TestData.Today.AddDays(dayOffset),
            StartTime = start ?? new TimeOnly(9, 0),
            EndTime = end ?? new TimeOnly(10, 0)
        };

    [Fact]
    public async Task Create_lets_a_coordinator_schedule_a_session()
    {
        var client = await _factory.CreateClientForAsync("coord1");

        var response = await client.PostAsJsonAsync("/api/sessions", NewSession());

        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
        var summary = await response.Content.ReadFromJsonAsync<SessionSummaryDto>();
        Assert.Equal("Literacy Boost", summary!.ProgrammeName);
        Assert.Equal("Upcoming", summary.Status);
        Assert.Equal(TestData.Today.AddDays(10), summary.SessionDate);
    }

    [Fact]
    public async Task Create_lets_a_teacher_schedule_their_own_session()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PostAsJsonAsync("/api/sessions", new CreateSessionRequest
        {
            ProgrammeId = 1,
            SessionDate = TestData.Today.AddDays(20),
            StartTime = new TimeOnly(9, 0),
            EndTime = new TimeOnly(10, 0)
        });

        Assert.Equal(HttpStatusCode.Created, response.StatusCode);
        var summary = await response.Content.ReadFromJsonAsync<SessionSummaryDto>();
        Assert.Equal(1, summary!.SchoolId);
    }

    [Fact]
    public async Task Create_is_forbidden_when_a_teacher_schedules_for_someone_else()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PostAsJsonAsync("/api/sessions", NewSession(teacherId: 2));

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task Create_reports_a_conflict_for_a_teachers_own_overlapping_session()
    {
        var client = await _factory.CreateClientForAsync("teacher1");

        var response = await client.PostAsJsonAsync("/api/sessions", new CreateSessionRequest
        {
            ProgrammeId = 1,
            SessionDate = TestData.Today,
            StartTime = new TimeOnly(14, 30),
            EndTime = new TimeOnly(15, 30)
        });

        Assert.Equal(HttpStatusCode.Conflict, response.StatusCode);
    }

    [Fact]
    public async Task Create_asks_a_coordinator_to_choose_a_teacher()
    {
        var client = await _factory.CreateClientForAsync("coord1");

        var response = await client.PostAsJsonAsync("/api/sessions", new CreateSessionRequest
        {
            ProgrammeId = 1,
            SchoolId = 1,
            SessionDate = TestData.Today.AddDays(21),
            StartTime = new TimeOnly(9, 0),
            EndTime = new TimeOnly(10, 0)
        });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task Create_is_forbidden_for_a_parent()
    {
        var client = await _factory.CreateClientForAsync("parent1");

        var response = await client.PostAsJsonAsync("/api/sessions", NewSession());

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Fact]
    public async Task Create_rejects_an_end_time_before_the_start_time()
    {
        var client = await _factory.CreateClientForAsync("coord1");

        var response = await client.PostAsJsonAsync("/api/sessions",
            NewSession(dayOffset: 11, start: new TimeOnly(10, 0), end: new TimeOnly(9, 0)));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task Create_rejects_a_teacher_who_works_at_another_school()
    {
        var client = await _factory.CreateClientForAsync("coord1");

        var response = await client.PostAsJsonAsync("/api/sessions", NewSession(teacherId: 3, schoolId: 1, dayOffset: 12));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task Create_rejects_a_date_outside_the_programme()
    {
        var client = await _factory.CreateClientForAsync("coord1");

        var response = await client.PostAsJsonAsync("/api/sessions", NewSession(dayOffset: 500));

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task Create_reports_a_conflict_when_the_teacher_is_already_booked()
    {
        var client = await _factory.CreateClientForAsync("coord1");

        // Teacher 1 already teaches today 14:00-15:00 (session 2).
        var response = await client.PostAsJsonAsync("/api/sessions",
            NewSession(teacherId: 1, dayOffset: 0, start: new TimeOnly(14, 30), end: new TimeOnly(15, 30)));

        Assert.Equal(HttpStatusCode.Conflict, response.StatusCode);
    }
}
