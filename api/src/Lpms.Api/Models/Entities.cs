namespace Lpms.Api.Models;

/// <summary>Role names stored in [User].Role and used in [Authorize(Roles = ...)].</summary>
public static class Roles
{
    public const string Administrator = "Administrator";
    public const string Coordinator = "Coordinator";
    public const string Teacher = "Teacher";
    public const string Parent = "Parent";
    public const string AdministratorOrCoordinator = Administrator + "," + Coordinator;
}

/// <summary>Allowed values of Attendance.AttendanceStatus.</summary>
public static class AttendanceStatuses
{
    public const string Present = "Present";
    public const string Absent = "Absent";
}

public class School
{
    public int SchoolId { get; set; }
    public string SchoolName { get; set; } = string.Empty;
    public string Address { get; set; } = string.Empty;
    public string PrincipalName { get; set; } = string.Empty;
    public string ContactNumber { get; set; } = string.Empty;
    public string? ContactEmail { get; set; }

    public List<Teacher> Teachers { get; set; } = new();
    public List<Learner> Learners { get; set; } = new();
    public List<Session> Sessions { get; set; } = new();
    public List<Resource> Resources { get; set; } = new();
}

public class Teacher
{
    public int TeacherId { get; set; }
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
    public int SchoolId { get; set; }
    public string ContactEmail { get; set; } = string.Empty;
    public string? ContactNumber { get; set; }

    public School School { get; set; } = null!;
    public List<Session> Sessions { get; set; } = new();
    public List<ProgressRecord> ProgressRecords { get; set; } = new();
}

public class Learner
{
    public int LearnerId { get; set; }
    public string FirstName { get; set; } = string.Empty;
    public string LastName { get; set; } = string.Empty;
    public string Grade { get; set; } = string.Empty;
    public int SchoolId { get; set; }

    public School School { get; set; } = null!;
    public List<AttendanceRecord> AttendanceRecords { get; set; } = new();
    public List<ProgressRecord> ProgressRecords { get; set; } = new();
}

public class Programme
{
    public int ProgrammeId { get; set; }
    public string ProgrammeName { get; set; } = string.Empty;
    public string? Description { get; set; }
    public DateOnly StartDate { get; set; }
    public DateOnly EndDate { get; set; }

    public List<Session> Sessions { get; set; } = new();
}

public class Session
{
    public int SessionId { get; set; }
    public int ProgrammeId { get; set; }
    public int SchoolId { get; set; }
    public int TeacherId { get; set; }
    public DateOnly SessionDate { get; set; }
    public TimeOnly StartTime { get; set; }
    public TimeOnly EndTime { get; set; }

    public Programme Programme { get; set; } = null!;
    public School School { get; set; } = null!;
    public Teacher Teacher { get; set; } = null!;
    public List<AttendanceRecord> AttendanceRecords { get; set; } = new();
}

/// <summary>Mapped to the Attendance table.</summary>
public class AttendanceRecord
{
    public int AttendanceId { get; set; }
    public int SessionId { get; set; }
    public int LearnerId { get; set; }
    public string AttendanceStatus { get; set; } = AttendanceStatuses.Present;

    public Session Session { get; set; } = null!;
    public Learner Learner { get; set; } = null!;
}

/// <summary>Mapped to the Progress table.</summary>
public class ProgressRecord
{
    public int ProgressId { get; set; }
    public int LearnerId { get; set; }
    public int TeacherId { get; set; }
    public decimal AssessmentScore { get; set; }
    public DateOnly AssessmentDate { get; set; }
    public string? Notes { get; set; }

    public Learner Learner { get; set; } = null!;
    public Teacher Teacher { get; set; } = null!;
}

public class Resource
{
    public int ResourceId { get; set; }
    public string ResourceName { get; set; } = string.Empty;
    public string ResourceType { get; set; } = string.Empty;
    public int SchoolId { get; set; }
    public int Quantity { get; set; }
    public DateOnly AllocationDate { get; set; }

    public School School { get; set; } = null!;
}

/// <summary>Mapped to the [User] table (named UserAccount to avoid clashing with ControllerBase.User).</summary>
public class UserAccount
{
    public int UserId { get; set; }
    public string Username { get; set; } = string.Empty;
    public string PasswordHash { get; set; } = string.Empty;
    public string Role { get; set; } = Roles.Teacher;
    public string FullName { get; set; } = string.Empty;
    public string Email { get; set; } = string.Empty;
    public bool IsActive { get; set; } = true;
    public int? TeacherId { get; set; }

    public Teacher? Teacher { get; set; }
}

/// <summary>Links a parent login (User with Role = Parent) to a child they may view.</summary>
public class ParentLearner
{
    public int ParentLearnerId { get; set; }
    public int UserId { get; set; }
    public int LearnerId { get; set; }
    public string Relationship { get; set; } = "Guardian";

    public UserAccount User { get; set; } = null!;
    public Learner Learner { get; set; } = null!;
}
