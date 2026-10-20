# Test plan

## 1. Strategy

| Level | Tool | Runs | Purpose |
|---|---|---|---|
| API integration tests | xUnit + `WebApplicationFactory` + in-memory SQLite | every push (CI) | Call the real endpoints over HTTP and check status codes, JSON and database effects |
| API unit tests | xUnit | every push (CI) | Password hashing, password policy, session status rules |
| App unit tests | JUnit 4 | every push (CI) | Input validation, formatting, attendance rules, error mapping, JSON contract with the API |
| Manual tests | Phone + hosted API | before release / demo | Real device, real network, UI behaviour (section 4) |

The API tests replace SQL Server with SQLite in memory and a fixed clock (2026-10-16 10:00, South African time), so they are fast and repeatable.

## 2. Automated API tests (`api/tests/Lpms.Api.Tests`)

| File | Covers |
|---|---|
| `AuthTests` | login by username / email (case-insensitive), wrong password, unknown user, empty password, inactive account, `/me` with no/garbage token, change password (wrong current, valid) |
| `SessionReadTests` | `mine` for upcoming / past / all and ordering, unknown scope, no token, wrong role, session detail with roster and statuses, other teacher's session (403), unknown session (404), coordinator can view |
| `SessionWriteTests` | save attendance, correct instead of duplicate, invalid status, empty list, duplicate learner, learner from another school, future session, other teacher, unknown session; coordinator schedules a session, teacher schedules own session, teacher scheduling for someone else (403), teacher clash (409), coordinator without a teacher, parent forbidden, end before start, teacher at another school, date outside programme, double-booking (409) |
| `ProgressTests` | add assessment (teacher taken from token), boundary scores 0 and 100, missing score/date, future date, notes > 255, other school, unknown learner, coordinator forbidden; edit / delete own, forbidden for others |
| `LearnerAndDashboardTests` | learners limited to the teacher's school, averages/rates/counts, search, coordinator school filter, no token; learner progress (ordering, label, read-only flags, other school 403, unknown 404); dashboard numbers and role check; schools, programmes, health |
| `ParentTests` | parent sign-in and profile, own children only, second parent sees different child, no token, staff forbidden on parent endpoint, own child's report (read-only), other parent's child / other school / all learners / session roster / teacher endpoints all 403, teachers still see every report |
| `RegisterTests` | public school list, register signs the teacher in and the password works, duplicate email (409), weak password, one-word name, bad email, unknown school |
| `RulesUnitTests` | password length limit, BCrypt verify/different salts, session status rules |

Run: `dotnet test api/Lpms.Api.sln` (results: GitHub Actions run > *Test* step, artifact `api-test-results`).

## 3. Automated app tests (`android/app/src/test`)

| File | Covers |
|---|---|
| `ValidatorsTest` | create-account name, email, password, school; new-session programme, date (past, programme range) and times; identifier, passwords (length, upper/lower/digit, differs, confirm), score parsing (whole, decimal, comma, range, junk like `abc`, `-5`, `1e5`), date and notes rules, server address normalisation and rejection |
| `AttendanceLogicTest` | draft from saved data, counts, marking all, change detection, save validation, order of entries, "future session" rule |
| `FormattersTest` | dates, times, percentages, scores, names/initials, greeting, plurals, safe fallback on bad data |
| `ApiErrorsTest` | message extraction from problem JSON, status code to error kind, friendly texts (never raw server text on 500) |
| `ApiModelsTest` | the app reads the API's real JSON (login, session, learner progress, missing fields) and sends the names the API expects |
| `TokenRulesTest` | token expiry check |
| `ReportLogicTest` | attendance target met / below / no data, target range, message wording, sessions needed to reach the target, safe PDF file names |

Run: `cd android && ./gradlew testDebugUnitTest` (report: `android/app/build/reports/tests/testDebugUnitTest/index.html`).

## 4. Manual test cases (device)

Environment: Android phone, app `1.0.0` debug build, hosted API + Azure SQL with the seed data.
Fill in **Actual result**, **Pass/Fail** and **Date/tester** while testing; keep a screenshot for any failure.

| ID | Area | Steps | Expected result | Actual | Pass/Fail |
|---|---|---|---|---|---|
| M01 | Sign in | `teacher01` / `Lpms@2026!` > Sign in | Home opens with "Good ..., Nomsa" |  |  |
| M02 | Sign in | Sign in with email `nomsa@read.org.za` | Home opens |  |  |
| M03 | Sign in | Leave both boxes empty > Sign in | Red messages under both boxes; no network call |  |  |
| M04 | Sign in | Wrong password | "Incorrect email/username or password."; password box cleared |  |  |
| M05 | Sign in | `teacher12` (inactive) | "This account is inactive..." |  |  |
| M06 | Sign in | `admin01` | "This app is for teachers and parents..." |  |  |
| M07 | Sign in | Type `teacher 01` (space) | "can't contain spaces" |  |  |
| M08 | Sign in | Airplane mode > Sign in | "Can't reach the server..." and the app does not crash |  |  |
| M09 | Session | Sign in, close the app from recents, reopen | Still signed in (token kept) |  |  |
| M10 | Home | Open Home | Four numbers and today's sessions shown; refresh button works |  |  |
| M11 | Sessions | Switch Upcoming / Past / All | List changes; empty state shown if none |  |  |
| M12 | Attendance | Open a session needing attendance > All present > change one learner > Save | "Attendance saved."; status becomes Completed |  |  |
| M13 | Attendance | Mark some learners only > Save | Message "Mark every learner ... (n still to mark)" |  |  |
| M14 | Attendance | Mark a learner then press Back | "Discard changes?" dialog |  |  |
| M15 | Attendance | Open an upcoming (future) session | Blue banner; buttons disabled; no Save bar |  |  |
| M16 | Learners | Type a partial name | List filters after a short pause; Clear restores it |  |  |
| M17 | Learners | Search for `zzzz` | "No matches" message |  |  |
| M18 | Progress | Add assessment 72.5 today | Appears in the list; average updates |  |  |
| M19 | Progress | Score `101`, `abc`, empty | Red message under the score; nothing saved |  |  |
| M20 | Progress | Notes longer than 255 | Input stops at 255; counter shows 255/255 |  |  |
| M21 | Progress | Date picker | Future dates cannot be selected |  |  |
| M22 | Progress | Edit then delete own assessment | Updated, then removed after confirmation |  |  |
| M23 | Progress | Open an assessment recorded by another teacher | No edit/delete icons |  |  |
| M24 | Settings | Theme Dark / Light / System | App theme changes immediately and survives restart |  |  |
| M25 | Settings | Larger text on | Text grows; layout still usable |  |  |
| M26 | Settings | Remember username off > sign out | Username box empty on the sign-in screen |  |  |
| M27 | Settings | Change server address to a wrong URL > Save and test | Red message; restoring the right one shows "Connected" |  |  |
| M28 | Settings | Change password: wrong current / weak new / mismatch / valid | Matching message each time; valid one succeeds, sign out and in with the new password works |  |  |
| M29 | Settings | Sign out > confirm | Back on the sign-in screen; Back button does not return to the app |  |  |
| M30 | Help | Open Help from sign-in and from Settings | Questions expand and collapse |  |  |
| M31 | Rotation | Rotate while on the add-assessment form | Entered values kept |  |  |
| M33 | Parent | Sign in `parent01` / `Lpms@2026!` | Lands on **My children** with two children; bottom bar shows only My children and Settings |  |  |
| M34 | Parent | Tap a child | Report with scores, attendance, assessments; no add/edit/delete controls |  |  |
| M35 | Parent | Settings > Attendance expectation: drag to 95% then 50% | Tag on the child changes between *Below* and *Meets* |  |  |
| M36 | Parent | Tap **Download PDF report** | Share sheet opens; the PDF opens and shows name, summary, assessments and attendance |  |  |
| M37 | Parent | Sign in as `parent02`, compare children | Different children from `parent01` |  |  |
| M38 | Teacher | Learner progress > share icon | PDF is created and the share sheet opens |  |  |
| M39 | Look | Open every screen in Light then Dark | Teal and amber design matches the mockups; text readable in both; status bar icons visible |  |  |
| M40 | Create account | Sign-in > Create an account > fill all four fields > Create account | Account created and Home opens; the new teacher has no sessions yet |  |  |
| M41 | Create account | Empty form; one-word name; `abc` email; weak password | A red message under each wrong field; nothing sent |  |  |
| M42 | Create account | Use an email that already exists | "An account with this email address already exists..." |  |  |
| M43 | New session | Sessions > + New (or Home > New session) > choose programme, date, start, end > Save | "Session scheduled."; it appears under Upcoming |  |  |
| M44 | New session | Date in the past is not selectable; end before start; overlapping an existing session | Past dates greyed out; messages for the other two |  |  |
| M32 | Expiry | (Optional) set `Jwt__ExpiryMinutes=1`, wait, open a session | Returned to sign-in with "Your session has expired" |  |  |

## 5. Defect log

| ID | Found in | Description | Severity | Status | Fixed in commit |
|---|---|---|---|---|---|
|  |  |  |  |  |  |
