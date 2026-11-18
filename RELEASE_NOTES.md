# Release notes

## 1.1 (final, Task 3)

### Innovative features
- **Messaging between parents and teachers.** One conversation per learner and parent. Teachers see the parents of learners at their own school; parents see only their own children's conversations; coordinators and administrators can see all. Messages are 1 to 1000 characters.
- **Targeted announcements.** Coordinators and administrators post a notice to Everyone, Teachers or Parents, for all schools or one school. Each person only sees notices meant for them.
- **Attendance that guides the teacher.** New header with Present, Absent and Not marked tiles; the save button counts down ("Mark 8 more to save") so a register cannot be saved half done; a clear notice when a session has not started yet.

### Other changes since the prototype
- Messages tab in the bottom navigation (five tabs for teachers, three for parents).
- New API endpoints: `GET /api/messages/conversations`, `GET` and `POST /api/messages/conversations/{learnerId}/{parentUserId}`, `GET` and `POST /api/announcements`.
- Database grew from 10 to 12 tables (`Message`, `Announcement`) with seed data (`database/04-seed-messages.sql`).
- Create account screen and New session screen (date and time pickers with a clash check).
- Home screen wording fixed ("1 session still needs attendance").
- About 35 new automated tests (API and app). Totals: 120+ API test cases, 82 app tests.
- GitHub Actions builds and tests the API and the app on every push and builds a debug APK.
- README, API reference, user guide and test plan updated.

### Known limitations
- Single sign-on (Google) is not yet implemented in this API or in the website; it exists in the group's Firebase build.
- The hosted API's connection to Azure SQL was still being fixed at the time of writing.
- Google Play Console upload not done.

## 1.0 (Task 2 prototype)
Sign in, create account, dashboard, sessions, attendance, learners and progress, PDF reports, settings and help; REST API with JWT; 10-table Azure SQL database; xUnit and JUnit tests.
