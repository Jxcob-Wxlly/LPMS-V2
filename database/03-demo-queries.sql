/* =====================================================================
   LPMS - 03-demo-queries.sql
   Read-only queries to run in the Azure portal Query editor (or SSMS /
   Azure Data Studio) while recording the demonstration video. They show
   what is stored in the online database and how the tables relate.
   Run each statement on its own (select it, then Run).
   ===================================================================== */

/* 1. Row count of every table (the brief asks for at least ten per table) */
SELECT 'School' AS TableName, COUNT(*) AS Rows FROM dbo.School
UNION ALL SELECT 'Teacher',    COUNT(*) FROM dbo.Teacher
UNION ALL SELECT 'Learner',    COUNT(*) FROM dbo.Learner
UNION ALL SELECT 'Programme',  COUNT(*) FROM dbo.Programme
UNION ALL SELECT 'Session',    COUNT(*) FROM dbo.Session
UNION ALL SELECT 'Attendance', COUNT(*) FROM dbo.Attendance
UNION ALL SELECT 'Progress',   COUNT(*) FROM dbo.Progress
UNION ALL SELECT 'Resource',   COUNT(*) FROM dbo.Resource
UNION ALL SELECT 'User',       COUNT(*) FROM dbo.[User]
UNION ALL SELECT 'ParentLearner', COUNT(*) FROM dbo.ParentLearner;

/* 2. Authentication store: who can sign in, and proof that passwords are hashed.
      Only the first characters of the BCrypt hash are shown. */
SELECT UserID, Username, Role, FullName, Email, IsActive, TeacherID,
       LEFT(PasswordHash, 10) + '...' AS PasswordHashStart, LEN(PasswordHash) AS HashLength
FROM dbo.[User]
ORDER BY UserID;

/* 3. The demo teacher (teacher01) and the school she teaches at */
SELECT u.Username, t.TeacherID, t.FirstName, t.LastName, s.SchoolName
FROM dbo.[User] u
JOIN dbo.Teacher t ON t.TeacherID = u.TeacherID
JOIN dbo.School  s ON s.SchoolID  = t.SchoolID
WHERE u.Username = N'teacher01';

/* 4. Sessions for teacher01 with attendance counts (what the app's "My Sessions" shows) */
SELECT se.SessionID, p.ProgrammeName, sc.SchoolName, se.SessionDate, se.StartTime, se.EndTime,
       SUM(CASE WHEN a.AttendanceStatus = N'Present' THEN 1 ELSE 0 END) AS Present,
       SUM(CASE WHEN a.AttendanceStatus = N'Absent'  THEN 1 ELSE 0 END) AS Absent
FROM dbo.Session se
JOIN dbo.Programme p  ON p.ProgrammeID = se.ProgrammeID
JOIN dbo.School    sc ON sc.SchoolID   = se.SchoolID
LEFT JOIN dbo.Attendance a ON a.SessionID = se.SessionID
WHERE se.TeacherID = 1
GROUP BY se.SessionID, p.ProgrammeName, sc.SchoolName, se.SessionDate, se.StartTime, se.EndTime
ORDER BY se.SessionDate, se.StartTime;

/* 5. Attendance saved from the app for one session (change @SessionID after saving in the app) */
DECLARE @SessionID INT = 6;
SELECT a.AttendanceID, l.FirstName, l.LastName, l.Grade, a.AttendanceStatus
FROM dbo.Attendance a
JOIN dbo.Learner l ON l.LearnerID = a.LearnerID
WHERE a.SessionID = @SessionID
ORDER BY l.LastName, l.FirstName;

/* 6. Learner progress: average score and number of assessments per learner (school 1) */
SELECT l.LearnerID, l.FirstName, l.LastName, l.Grade,
       COUNT(pr.ProgressID) AS Assessments,
       CAST(AVG(pr.AssessmentScore) AS DECIMAL(5,1)) AS AverageScore
FROM dbo.Learner l
LEFT JOIN dbo.Progress pr ON pr.LearnerID = l.LearnerID
WHERE l.SchoolID = 1
GROUP BY l.LearnerID, l.FirstName, l.LastName, l.Grade
ORDER BY l.LastName;

/* 7. The newest assessments (shows rows added from the app, with the teacher who recorded them) */
SELECT TOP (10) pr.ProgressID, l.FirstName + N' ' + l.LastName AS Learner, pr.AssessmentScore,
       pr.AssessmentDate, pr.Notes, t.FirstName + N' ' + t.LastName AS RecordedBy
FROM dbo.Progress pr
JOIN dbo.Learner l ON l.LearnerID = pr.LearnerID
JOIN dbo.Teacher t ON t.TeacherID = pr.TeacherID
ORDER BY pr.ProgressID DESC;

/* 8. Resource allocation per school */
SELECT s.SchoolName, r.ResourceName, r.ResourceType, r.Quantity, r.AllocationDate
FROM dbo.Resource r
JOIN dbo.School s ON s.SchoolID = r.SchoolID
ORDER BY s.SchoolName, r.ResourceName;

/* 9. Parent accounts and the children each parent may see (the parent view in the app) */
SELECT u.Username, u.FullName AS Parent, pl.Relationship,
       l.LearnerID, l.FirstName + N' ' + l.LastName AS Child, l.Grade, sc.SchoolName
FROM dbo.ParentLearner pl
JOIN dbo.[User]  u  ON u.UserID    = pl.UserID
JOIN dbo.Learner l  ON l.LearnerID = pl.LearnerID
JOIN dbo.School  sc ON sc.SchoolID = l.SchoolID
ORDER BY u.Username, l.LastName;
