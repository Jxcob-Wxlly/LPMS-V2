/* =====================================================================
   LPMS - Literacy Programme Management System
   File    : 01-create-schema.sql
   Target  : Microsoft SQL Server / Azure SQL Database (T-SQL)
   Purpose : Creates the nine LPMS tables from the System Design Document (plus ParentLearner)
             (School, Teacher, Learner, Programme, Session, Attendance,
              Progress, Resource, User) plus ParentLearner (Task 2 addition),
              normalised to 3NF.

   How to run
     * Azure SQL : create an empty database (e.g. LpmsDb) in the portal, open
                   it in the Query editor / Azure Data Studio / SSMS and run
                   this file, then 02-seed-data.sql.
     * Local     : CREATE DATABASE LpmsDb;  then run this file inside it.
   The script is re-runnable: it drops the tables first (children before
   parents). It contains no GO separators so it also works in the portal
   Query editor.

   Naming conventions
     * Tables  : singular PascalCase (School, Teacher ...)
     * Columns : PascalCase, primary keys are <Table>ID, foreign keys keep the
                 name of the key they reference (SchoolID, TeacherID ...)
     * Constraints: PK_<Table>, FK_<Child>_<Parent>, UQ_<Table>_<Column>,
                 CK_<Table>_<Rule>, IX_<Table>_<Column(s)>

   Changes compared with the XISD5319 System Design Document
     1. [User].TeacherID (nullable FK) was added so a teacher login can be
        linked to its Teacher record (needed by the mobile app).
     2. CHECK constraints, UNIQUE constraints and indexes were added.
     3. Role 'Parent' and the ParentLearner table (parent login <-> child) were
        added so parents can view their own children's attendance and progress.
   ===================================================================== */

SET NOCOUNT ON;

/* ---------- drop in reverse dependency order ---------- */
DROP TABLE IF EXISTS dbo.Attendance;
DROP TABLE IF EXISTS dbo.Progress;
DROP TABLE IF EXISTS dbo.Session;
DROP TABLE IF EXISTS dbo.Resource;
DROP TABLE IF EXISTS dbo.ParentLearner;
DROP TABLE IF EXISTS dbo.[User];
DROP TABLE IF EXISTS dbo.Learner;
DROP TABLE IF EXISTS dbo.Teacher;
DROP TABLE IF EXISTS dbo.Programme;
DROP TABLE IF EXISTS dbo.School;

/* ---------- School ---------- */
CREATE TABLE dbo.School (
    SchoolID       INT IDENTITY(1,1) NOT NULL,
    SchoolName     NVARCHAR(100)     NOT NULL,
    Address        NVARCHAR(150)     NOT NULL,
    PrincipalName  NVARCHAR(80)      NOT NULL,
    ContactNumber  NVARCHAR(15)      NOT NULL,
    ContactEmail   NVARCHAR(100)     NULL,
    CONSTRAINT PK_School            PRIMARY KEY (SchoolID),
    CONSTRAINT UQ_School_SchoolName UNIQUE (SchoolName)
);

/* ---------- Programme ---------- */
CREATE TABLE dbo.Programme (
    ProgrammeID    INT IDENTITY(1,1) NOT NULL,
    ProgrammeName  NVARCHAR(100)     NOT NULL,
    Description    NVARCHAR(255)     NULL,
    StartDate      DATE              NOT NULL,
    EndDate        DATE              NOT NULL,
    CONSTRAINT PK_Programme               PRIMARY KEY (ProgrammeID),
    CONSTRAINT UQ_Programme_ProgrammeName UNIQUE (ProgrammeName),
    CONSTRAINT CK_Programme_Dates         CHECK (EndDate >= StartDate)
);

/* ---------- Teacher ---------- */
CREATE TABLE dbo.Teacher (
    TeacherID      INT IDENTITY(1,1) NOT NULL,
    FirstName      NVARCHAR(50)      NOT NULL,
    LastName       NVARCHAR(50)      NOT NULL,
    SchoolID       INT               NOT NULL,
    ContactEmail   NVARCHAR(100)     NOT NULL,
    ContactNumber  NVARCHAR(15)      NULL,
    CONSTRAINT PK_Teacher              PRIMARY KEY (TeacherID),
    CONSTRAINT UQ_Teacher_ContactEmail UNIQUE (ContactEmail),
    CONSTRAINT FK_Teacher_School       FOREIGN KEY (SchoolID) REFERENCES dbo.School (SchoolID)
);
CREATE INDEX IX_Teacher_SchoolID ON dbo.Teacher (SchoolID);

/* ---------- Learner ---------- */
CREATE TABLE dbo.Learner (
    LearnerID  INT IDENTITY(1,1) NOT NULL,
    FirstName  NVARCHAR(50)      NOT NULL,
    LastName   NVARCHAR(50)      NOT NULL,
    Grade      NVARCHAR(8)       NOT NULL,
    SchoolID   INT               NOT NULL,
    CONSTRAINT PK_Learner        PRIMARY KEY (LearnerID),
    CONSTRAINT FK_Learner_School FOREIGN KEY (SchoolID) REFERENCES dbo.School (SchoolID),
    CONSTRAINT CK_Learner_Grade  CHECK (Grade IN (N'Grade R', N'Grade 1', N'Grade 2', N'Grade 3',
                                                  N'Grade 4', N'Grade 5', N'Grade 6', N'Grade 7',
                                                  N'Grade 8', N'Grade 9', N'Grade 10', N'Grade 11',
                                                  N'Grade 12'))
);
CREATE INDEX IX_Learner_SchoolID ON dbo.Learner (SchoolID);

/* ---------- Session ---------- */
CREATE TABLE dbo.Session (
    SessionID    INT IDENTITY(1,1) NOT NULL,
    ProgrammeID  INT               NOT NULL,
    SchoolID     INT               NOT NULL,
    TeacherID    INT               NOT NULL,
    SessionDate  DATE              NOT NULL,
    StartTime    TIME(0)           NOT NULL,
    EndTime      TIME(0)           NOT NULL,
    CONSTRAINT PK_Session           PRIMARY KEY (SessionID),
    CONSTRAINT FK_Session_Programme FOREIGN KEY (ProgrammeID) REFERENCES dbo.Programme (ProgrammeID),
    CONSTRAINT FK_Session_School    FOREIGN KEY (SchoolID)    REFERENCES dbo.School (SchoolID),
    CONSTRAINT FK_Session_Teacher   FOREIGN KEY (TeacherID)   REFERENCES dbo.Teacher (TeacherID),
    CONSTRAINT CK_Session_Times     CHECK (EndTime > StartTime)
);
CREATE INDEX IX_Session_TeacherID_SessionDate ON dbo.Session (TeacherID, SessionDate);
CREATE INDEX IX_Session_SchoolID              ON dbo.Session (SchoolID);
CREATE INDEX IX_Session_ProgrammeID           ON dbo.Session (ProgrammeID);

/* ---------- Attendance ---------- */
CREATE TABLE dbo.Attendance (
    AttendanceID      INT IDENTITY(1,1) NOT NULL,
    SessionID         INT               NOT NULL,
    LearnerID         INT               NOT NULL,
    AttendanceStatus  NVARCHAR(10)      NOT NULL,
    CONSTRAINT PK_Attendance                 PRIMARY KEY (AttendanceID),
    CONSTRAINT FK_Attendance_Session         FOREIGN KEY (SessionID) REFERENCES dbo.Session (SessionID),
    CONSTRAINT FK_Attendance_Learner         FOREIGN KEY (LearnerID) REFERENCES dbo.Learner (LearnerID),
    CONSTRAINT UQ_Attendance_Session_Learner UNIQUE (SessionID, LearnerID),
    CONSTRAINT CK_Attendance_Status          CHECK (AttendanceStatus IN (N'Present', N'Absent'))
);
CREATE INDEX IX_Attendance_LearnerID ON dbo.Attendance (LearnerID);

/* ---------- Progress ---------- */
CREATE TABLE dbo.Progress (
    ProgressID       INT IDENTITY(1,1) NOT NULL,
    LearnerID        INT               NOT NULL,
    TeacherID        INT               NOT NULL,
    AssessmentScore  DECIMAL(5,2)      NOT NULL,
    AssessmentDate   DATE              NOT NULL,
    Notes            NVARCHAR(255)     NULL,
    CONSTRAINT PK_Progress         PRIMARY KEY (ProgressID),
    CONSTRAINT FK_Progress_Learner FOREIGN KEY (LearnerID) REFERENCES dbo.Learner (LearnerID),
    CONSTRAINT FK_Progress_Teacher FOREIGN KEY (TeacherID) REFERENCES dbo.Teacher (TeacherID),
    CONSTRAINT CK_Progress_Score   CHECK (AssessmentScore BETWEEN 0 AND 100)
);
CREATE INDEX IX_Progress_LearnerID_AssessmentDate ON dbo.Progress (LearnerID, AssessmentDate);
CREATE INDEX IX_Progress_TeacherID                ON dbo.Progress (TeacherID);

/* ---------- Resource ---------- */
CREATE TABLE dbo.Resource (
    ResourceID      INT IDENTITY(1,1) NOT NULL,
    ResourceName    NVARCHAR(100)     NOT NULL,
    ResourceType    NVARCHAR(20)      NOT NULL,
    SchoolID        INT               NOT NULL,
    Quantity        INT               NOT NULL,
    AllocationDate  DATE              NOT NULL,
    CONSTRAINT PK_Resource          PRIMARY KEY (ResourceID),
    CONSTRAINT FK_Resource_School   FOREIGN KEY (SchoolID) REFERENCES dbo.School (SchoolID),
    CONSTRAINT CK_Resource_Type     CHECK (ResourceType IN (N'Books', N'Stationery', N'Digital', N'Other')),
    CONSTRAINT CK_Resource_Quantity CHECK (Quantity > 0)
);
CREATE INDEX IX_Resource_SchoolID ON dbo.Resource (SchoolID);

/* ---------- User (authentication store) ----------
   PasswordHash holds a BCrypt hash (60 characters). Plain-text passwords
   are never stored. [User] is bracketed because USER is a T-SQL keyword. */
CREATE TABLE dbo.[User] (
    UserID        INT IDENTITY(1,1) NOT NULL,
    Username      NVARCHAR(50)      NOT NULL,
    PasswordHash  NVARCHAR(100)     NOT NULL,
    Role          NVARCHAR(20)      NOT NULL,
    FullName      NVARCHAR(100)     NOT NULL,
    Email         NVARCHAR(100)     NOT NULL,
    IsActive      BIT               NOT NULL CONSTRAINT DF_User_IsActive DEFAULT (1),
    TeacherID     INT               NULL,
    CONSTRAINT PK_User             PRIMARY KEY (UserID),
    CONSTRAINT UQ_User_Username    UNIQUE (Username),
    CONSTRAINT UQ_User_Email       UNIQUE (Email),
    CONSTRAINT FK_User_Teacher     FOREIGN KEY (TeacherID) REFERENCES dbo.Teacher (TeacherID),
    CONSTRAINT CK_User_Role        CHECK (Role IN (N'Administrator', N'Coordinator', N'Teacher', N'Parent')),
    CONSTRAINT CK_User_TeacherLink CHECK (Role <> N'Teacher' OR TeacherID IS NOT NULL)
);
/* one login per teacher */
CREATE UNIQUE INDEX UX_User_TeacherID ON dbo.[User] (TeacherID) WHERE TeacherID IS NOT NULL;

/* ---------- ParentLearner (which children a parent login may see) ----------
   Many-to-many between [User] (Role = Parent) and Learner: a parent can have
   several children and a child can have several guardians. */
CREATE TABLE dbo.ParentLearner (
    ParentLearnerID INT IDENTITY(1,1) NOT NULL,
    UserID          INT               NOT NULL,
    LearnerID       INT               NOT NULL,
    Relationship    NVARCHAR(20)      NOT NULL CONSTRAINT DF_ParentLearner_Relationship DEFAULT (N'Guardian'),
    CONSTRAINT PK_ParentLearner         PRIMARY KEY (ParentLearnerID),
    CONSTRAINT FK_ParentLearner_User    FOREIGN KEY (UserID)    REFERENCES dbo.[User] (UserID),
    CONSTRAINT FK_ParentLearner_Learner FOREIGN KEY (LearnerID) REFERENCES dbo.Learner (LearnerID),
    CONSTRAINT UQ_ParentLearner_Pair    UNIQUE (UserID, LearnerID),
    CONSTRAINT CK_ParentLearner_Relationship CHECK (Relationship IN (N'Mother', N'Father', N'Guardian'))
);
CREATE INDEX IX_ParentLearner_LearnerID ON dbo.ParentLearner (LearnerID);

PRINT 'LPMS schema created: 10 tables.';
