using System.Net;
using System.Net.Http.Json;
using Lpms.Api.Dtos;

namespace Lpms.Api.Tests;

/// <summary>
/// Parent / staff conversations and announcements.
/// Seed: parent1 (user 8) has learners 1 and 2, parent2 (user 9) has learner 3 (all at school 1).
/// teacher1 and teacher2 work at school 1, teacher3 at school 2.
/// </summary>
public class MessagingTests : IClassFixture<LpmsApiFactory>
{
    private readonly LpmsApiFactory _factory;

    public MessagingTests(LpmsApiFactory factory)
    {
        _factory = factory;
    }

    private static string Unique(string prefix) => $"{prefix}-{Guid.NewGuid():N}";

    // ---------- conversations ----------

    [Fact]
    public async Task Parent_lists_a_conversation_for_each_of_their_children()
    {
        var parent = await _factory.CreateClientForAsync("parent1");

        var list = await parent.GetFromJsonAsync<List<ConversationSummaryDto>>("/api/messages/conversations");

        Assert.Equal(new[] { 1, 2 }, list!.Select(c => c.LearnerId).OrderBy(id => id).ToArray());
        Assert.All(list!, c => Assert.Equal(8, c.ParentUserId));
    }

    [Fact]
    public async Task Teacher_sees_only_conversations_for_learners_at_their_school()
    {
        var school1 = await _factory.CreateClientForAsync("teacher1");
        var school2 = await _factory.CreateClientForAsync("teacher3");

        var mine = await school1.GetFromJsonAsync<List<ConversationSummaryDto>>("/api/messages/conversations");
        var other = await school2.GetFromJsonAsync<List<ConversationSummaryDto>>("/api/messages/conversations");

        Assert.Equal(3, mine!.Count);
        Assert.Empty(other!);
    }

    [Fact]
    public async Task Coordinator_sees_every_conversation()
    {
        var coordinator = await _factory.CreateClientForAsync("coord1");

        var list = await coordinator.GetFromJsonAsync<List<ConversationSummaryDto>>("/api/messages/conversations");

        Assert.Equal(3, list!.Count);
    }

    [Fact]
    public async Task Parent_message_reaches_the_teacher_and_shows_as_the_last_message()
    {
        var parent = await _factory.CreateClientForAsync("parent2");
        var teacher = await _factory.CreateClientForAsync("teacher1");
        var text = Unique("Hello teacher");

        var send = await parent.PostAsJsonAsync("/api/messages/conversations/3/9", new SendMessageRequest { Body = text });

        Assert.Equal(HttpStatusCode.Created, send.StatusCode);
        var sent = await send.Content.ReadFromJsonAsync<MessageDto>();
        Assert.True(sent!.IsMine);
        Assert.Equal("Parent", sent.SenderRole);

        var thread = await teacher.GetFromJsonAsync<List<MessageDto>>("/api/messages/conversations/3/9");
        var seen = thread!.Single(m => m.Body == text);
        Assert.False(seen.IsMine);
        Assert.Equal("Sipho Mthethwa", seen.SenderName);

        var list = await teacher.GetFromJsonAsync<List<ConversationSummaryDto>>("/api/messages/conversations");
        var summary = list!.Single(c => c.LearnerId == 3);
        Assert.Equal(text, summary.LastMessage);
        Assert.True(summary.MessageCount >= 1);
    }

    [Fact]
    public async Task Teacher_reply_is_seen_by_the_parent_as_a_teacher_message()
    {
        var parent = await _factory.CreateClientForAsync("parent1");
        var teacher = await _factory.CreateClientForAsync("teacher2");
        var text = Unique("Amahle did well today");

        var send = await teacher.PostAsJsonAsync("/api/messages/conversations/1/8", new SendMessageRequest { Body = text });
        Assert.Equal(HttpStatusCode.Created, send.StatusCode);

        var thread = await parent.GetFromJsonAsync<List<MessageDto>>("/api/messages/conversations/1/8");
        var seen = thread!.Single(m => m.Body == text);
        Assert.Equal("Teacher", seen.SenderRole);
        Assert.False(seen.IsMine);
    }

    [Fact]
    public async Task Messages_come_back_oldest_first()
    {
        var parent = await _factory.CreateClientForAsync("parent1");
        var first = Unique("first");
        var second = Unique("second");
        await parent.PostAsJsonAsync("/api/messages/conversations/2/8", new SendMessageRequest { Body = first });
        _factory.Clock.Now = _factory.Clock.Now.AddMinutes(5);
        await parent.PostAsJsonAsync("/api/messages/conversations/2/8", new SendMessageRequest { Body = second });

        var thread = await parent.GetFromJsonAsync<List<MessageDto>>("/api/messages/conversations/2/8");

        var bodies = thread!.Select(m => m.Body).ToList();
        Assert.True(bodies.IndexOf(first) < bodies.IndexOf(second));
    }

    [Fact]
    public async Task A_parent_cannot_read_or_write_another_parents_conversation()
    {
        var parent2 = await _factory.CreateClientForAsync("parent2");

        var read = await parent2.GetAsync("/api/messages/conversations/1/8");
        var write = await parent2.PostAsJsonAsync("/api/messages/conversations/1/8", new SendMessageRequest { Body = "hi" });

        Assert.Equal(HttpStatusCode.Forbidden, read.StatusCode);
        Assert.Equal(HttpStatusCode.Forbidden, write.StatusCode);
    }

    [Fact]
    public async Task A_teacher_at_another_school_cannot_open_the_conversation()
    {
        var teacher3 = await _factory.CreateClientForAsync("teacher3");

        var read = await teacher3.GetAsync("/api/messages/conversations/1/8");
        var write = await teacher3.PostAsJsonAsync("/api/messages/conversations/1/8", new SendMessageRequest { Body = "hi" });

        Assert.Equal(HttpStatusCode.Forbidden, read.StatusCode);
        Assert.Equal(HttpStatusCode.Forbidden, write.StatusCode);
    }

    [Fact]
    public async Task A_learner_and_parent_who_are_not_linked_give_not_found()
    {
        var coordinator = await _factory.CreateClientForAsync("coord1");

        var response = await coordinator.GetAsync("/api/messages/conversations/4/8");

        Assert.Equal(HttpStatusCode.NotFound, response.StatusCode);
    }

    [Theory]
    [InlineData("")]
    [InlineData("   ")]
    public async Task A_blank_message_is_rejected(string body)
    {
        var parent = await _factory.CreateClientForAsync("parent1");

        var response = await parent.PostAsJsonAsync("/api/messages/conversations/1/8", new SendMessageRequest { Body = body });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task A_message_over_1000_characters_is_rejected()
    {
        var parent = await _factory.CreateClientForAsync("parent1");

        var response = await parent.PostAsJsonAsync("/api/messages/conversations/1/8",
            new SendMessageRequest { Body = new string('a', 1001) });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task Messages_require_a_token()
    {
        var response = await _factory.CreateClient().GetAsync("/api/messages/conversations");

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }

    // ---------- announcements ----------

    [Fact]
    public async Task Coordinator_announcement_for_parents_is_seen_by_parents_but_not_teachers()
    {
        var coordinator = await _factory.CreateClientForAsync("coord1");
        var parent = await _factory.CreateClientForAsync("parent1");
        var teacher = await _factory.CreateClientForAsync("teacher1");
        var title = Unique("Parents evening");

        var create = await coordinator.PostAsJsonAsync("/api/announcements",
            new CreateAnnouncementRequest { Title = title, Body = "Thursday at 17:00.", Audience = "Parents" });

        Assert.Equal(HttpStatusCode.Created, create.StatusCode);
        var parentView = await parent.GetFromJsonAsync<List<AnnouncementDto>>("/api/announcements");
        var teacherView = await teacher.GetFromJsonAsync<List<AnnouncementDto>>("/api/announcements");
        Assert.Contains(parentView!, a => a.Title == title);
        Assert.DoesNotContain(teacherView!, a => a.Title == title);
    }

    [Fact]
    public async Task Announcement_for_teachers_is_seen_by_teachers_but_not_parents()
    {
        var admin = await _factory.CreateClientForAsync("admin1");
        var parent = await _factory.CreateClientForAsync("parent1");
        var teacher = await _factory.CreateClientForAsync("teacher3");
        var title = Unique("Staff meeting");

        await admin.PostAsJsonAsync("/api/announcements",
            new CreateAnnouncementRequest { Title = title, Body = "Friday 08:00.", Audience = "Teachers" });

        var teacherView = await teacher.GetFromJsonAsync<List<AnnouncementDto>>("/api/announcements");
        var parentView = await parent.GetFromJsonAsync<List<AnnouncementDto>>("/api/announcements");
        Assert.Contains(teacherView!, a => a.Title == title);
        Assert.DoesNotContain(parentView!, a => a.Title == title);
    }

    [Fact]
    public async Task School_announcement_is_seen_only_by_people_at_that_school()
    {
        var coordinator = await _factory.CreateClientForAsync("coord1");
        var school2Teacher = await _factory.CreateClientForAsync("teacher3");
        var school1Teacher = await _factory.CreateClientForAsync("teacher1");
        var school1Parent = await _factory.CreateClientForAsync("parent1");
        var title = Unique("Tembisa notice");

        var create = await coordinator.PostAsJsonAsync("/api/announcements",
            new CreateAnnouncementRequest { Title = title, Body = "Library closed.", Audience = "Everyone", SchoolId = 2 });
        var created = await create.Content.ReadFromJsonAsync<AnnouncementDto>();

        Assert.Equal("Tembisa High", created!.SchoolName);
        Assert.Contains((await school2Teacher.GetFromJsonAsync<List<AnnouncementDto>>("/api/announcements"))!, a => a.Title == title);
        Assert.DoesNotContain((await school1Teacher.GetFromJsonAsync<List<AnnouncementDto>>("/api/announcements"))!, a => a.Title == title);
        Assert.DoesNotContain((await school1Parent.GetFromJsonAsync<List<AnnouncementDto>>("/api/announcements"))!, a => a.Title == title);
    }

    [Fact]
    public async Task Coordinators_see_every_announcement()
    {
        var admin = await _factory.CreateClientForAsync("admin1");
        var coordinator = await _factory.CreateClientForAsync("coord1");
        var title = Unique("Everything");
        await admin.PostAsJsonAsync("/api/announcements",
            new CreateAnnouncementRequest { Title = title, Body = "Body", Audience = "Teachers", SchoolId = 1 });

        var view = await coordinator.GetFromJsonAsync<List<AnnouncementDto>>("/api/announcements");

        Assert.Contains(view!, a => a.Title == title);
    }

    [Theory]
    [InlineData("teacher1")]
    [InlineData("parent1")]
    public async Task Teachers_and_parents_cannot_post_announcements(string username)
    {
        var client = await _factory.CreateClientForAsync(username);

        var response = await client.PostAsJsonAsync("/api/announcements",
            new CreateAnnouncementRequest { Title = "Hi", Body = "There", Audience = "Everyone" });

        Assert.Equal(HttpStatusCode.Forbidden, response.StatusCode);
    }

    [Theory]
    [InlineData("", "Body", "Everyone", null)]
    [InlineData("Title", "", "Everyone", null)]
    [InlineData("Title", "Body", "Nobody", null)]
    [InlineData("Title", "Body", "Everyone", 999)]
    public async Task Invalid_announcements_are_rejected(string title, string body, string audience, int? schoolId)
    {
        var coordinator = await _factory.CreateClientForAsync("coord1");

        var response = await coordinator.PostAsJsonAsync("/api/announcements",
            new CreateAnnouncementRequest { Title = title, Body = body, Audience = audience, SchoolId = schoolId });

        Assert.Equal(HttpStatusCode.BadRequest, response.StatusCode);
    }

    [Fact]
    public async Task Announcements_require_a_token()
    {
        var response = await _factory.CreateClient().GetAsync("/api/announcements");

        Assert.Equal(HttpStatusCode.Unauthorized, response.StatusCode);
    }
}
