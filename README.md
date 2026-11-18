# LPMS - Literacy Programme Management System

[![CI](https://github.com/Jxcob-Wxlly/LPMS-V2/actions/workflows/ci.yml/badge.svg)](https://github.com/Jxcob-Wxlly/LPMS-V2/actions/workflows/ci.yml)

**Client:** READ Educational Trust  **Team:** Team Point Blank  **Module:** XISD6329 WIL 3B (Task 2 and Task 3, final submission)

LPMS replaces the paper registers and spreadsheets READ Educational Trust uses to run its school literacy programmes.
This repository holds the build for Task 2 and the final Task 3 version: the **mobile app (Android) for teachers and parents**, the **REST API** that the app talks to, and the
**relational database**, plus the DevOps pipelines, automated tests and documentation.

| Part | Technology | Folder |
|---|---|---|
| Mobile app | Kotlin, Jetpack Compose, Material 3, Retrofit | [`android/`](android) |
| REST API | ASP.NET Core 8, Entity Framework Core, JWT, Swagger | [`api/`](api) |
| Database | SQL Server / Azure SQL (T-SQL scripts) | [`database/`](database) |
| Pipelines | GitHub Actions and Azure DevOps | [`.github/workflows/`](.github/workflows), [`azure-pipelines.yml`](azure-pipelines.yml) |
| Documentation | Markdown | [`docs/`](docs) |

## Screenshots

Taken on a physical Android phone running the app.

| Sign in | Create account | Home | Sessions | Attendance |
|---|---|---|---|---|
| <img src="docs/images/app-signin.jpg" width="150"> | <img src="docs/images/app-create-account.jpg" width="150"> | <img src="docs/images/app-home.jpg" width="150"> | <img src="docs/images/app-sessions.jpg" width="150"> | <img src="docs/images/app-attendance.jpg" width="150"> |

| New session | Learners | Learner progress | PDF report | Settings (dark) |
|---|---|---|---|---|
| <img src="docs/images/app-new-session.jpg" width="150"> | <img src="docs/images/app-learners.jpg" width="150"> | <img src="docs/images/app-learner.jpg" width="150"> | <img src="docs/images/app-report.jpg" width="150"> | <img src="docs/images/app-settings-dark.jpg" width="150"> |

The management website (Task 1, a separate repository: [Jxcob-Wxlly/lpms](https://github.com/Jxcob-Wxlly/lpms)) looks like this:

<img src="docs/images/web-dashboard.png" width="620">

## Purpose and design considerations

**Purpose.** READ Educational Trust runs literacy programmes in many under-resourced schools. Attendance and learner progress are kept on paper and in
spreadsheets, so reports to donors are late, records are duplicated and nobody has one view of whether the programmes work. LPMS gives teachers a fast way to record
attendance and scores on a phone, gives parents a read-only view of their own children plus a direct line to the teacher, and keeps everything in one database that
coordinators can report from.

**Design considerations**

- **Built for the classroom:** large touch targets, five-tab bottom navigation, taking attendance is two taps from Home, light, dark and large-text options.
- **One design system:** teal and amber, Bricolage Grotesque and Figtree fonts bundled in the app, the same components on every screen, following the mockups designed before building.
- **Invalid input never crashes the app:** every field is validated on the phone and again in the API, and network errors become friendly messages.
- **Least privilege:** parents see only their own children, teachers only their school; the rules live in the API, not only in the app.
- **Security:** BCrypt password hashes, JWT sessions, encrypted token storage on the phone, rate-limited sign-in, secrets kept in Azure settings and never in the code.
- **Testable:** business rules are pure Kotlin functions and the API is tested end to end, so changes are safe to make.

## What the app does

A teacher signs in and can:

- **create their own account** from the sign-in screen (name, email, school, password) and be signed straight in;
- see a **dashboard** (sessions this week and today, learner count, attendance rate, sessions that still need attendance);
- browse **upcoming and past sessions**, **schedule a new session** for themselves (+ New) and **take attendance** (Present / Absent per learner, "All present" shortcut);
- **search learners** and open a learner's **progress** (average score, progress label, attendance history);
- **add, edit and delete assessments** (score 0-100, date, notes);
- use **Settings**: theme (light / dark / phone setting), larger text, remember username, server address with a connection test,
  change password, sign out;
- **message parents** about a learner and read **announcements** from coordinators in the **Messages** tab;
- open the built-in **Help** (FAQ) from the sign-in screen or Settings.

A **parent / guardian** signs in with their own account and can:

- see **only their own children** with average score and attendance;
- see at a glance whether each child's attendance **meets their own expectation** (set in Settings, default 80%);
- open a **child report** (assessments, recent attendance) and **download it as a PDF** to save, print or share;
- **message their child's teachers** (one conversation per child) and read school **announcements** in the **Messages** tab.

Teachers can also create the same PDF from a learner's progress screen. The API enforces the separation: a parent gets
403 for any child who is not linked to them, and for rosters, other learners and every teacher endpoint.

The whole app follows one design system (teal and amber, light and dark, Bricolage Grotesque headings with Figtree body text, bundled in the APK) so it matches the mockups the team designed.

Invalid input never crashes the app: every field is validated before anything is sent (see
[`Validators.kt`](android/app/src/main/java/com/teampointblank/lpms/domain/Validators.kt)), the API validates again, and every network
failure is turned into a friendly message.

## Architecture

```mermaid
flowchart LR
    A["Android app<br/>(Kotlin, Compose)"] -- "HTTPS + JSON<br/>Bearer JWT" --> B["LPMS REST API<br/>ASP.NET Core 8<br/>Azure App Service"]
    B -- "EF Core / TLS" --> C[("Azure SQL Database<br/>12 tables incl. User, ParentLearner, Message and Announcement")]
    D["GitHub / Azure DevOps<br/>CI pipeline"] -- "deploy" --> B
    D -- "debug APK artifact" --> A
```

**Authentication.** The API owns sign-in. `POST /api/auth/login` checks the password against the BCrypt hash in the `User` table
and returns a signed JWT (HS256, 8 hours). The app keeps the token in `EncryptedSharedPreferences` (Android Keystore) and sends it
as `Authorization: Bearer ...`. Passwords are never stored on the phone and never returned by the API. Login attempts are rate limited.

**Authorisation.** Roles are Administrator, Coordinator and Teacher. Teachers only reach their own sessions and their own school's
learners; only the teacher who recorded an assessment can change it; teachers schedule sessions for themselves only (the API takes teacher and school from the token), coordinators/administrators can schedule for any teacher. New teachers register through `POST /api/auth/register`.

## Repository layout

```
lpms/
├── android/                 Android Studio project (Gradle wrapper included)
│   └── app/src/main/java/com/teampointblank/lpms/
│       ├── data/            Retrofit API, models, token store, settings, repository
│       ├── domain/          Validators, formatters, attendance rules (pure Kotlin, unit tested)
│       └── ui/              Compose screens + ViewModels: login, home, sessions, learners, settings, help
├── api/
│   ├── Lpms.sln
│   ├── src/Lpms.Api/        Controllers, DTOs, EF Core model, services, Program.cs
│   └── tests/Lpms.Api.Tests integration + unit tests (xUnit, in-memory SQLite)
├── database/
│   ├── 01-create-schema.sql tables, keys, constraints, indexes
│   ├── 02-seed-data.sql     simulated data (10+ rows per table, BCrypt-hashed passwords)
│   ├── 03-demo-queries.sql  read-only queries used in the demo video
│   ├── 04-seed-messages.sql sample conversations and announcements (run after 02)
│   └── tools/generate-seed.py   generator + self-check for the seed data
├── docs/                    user guide, deployment guide, API reference, test plan, demo script, DevOps lifecycle
├── .github/workflows/ci.yml GitHub Actions: build, test, APK, deploy
└── azure-pipelines.yml      Azure DevOps equivalent
```

## Quick start

### 1. Database (Azure SQL or local SQL Server)

Run the scripts in `database/` in order (Azure portal **Query editor**, SSMS or Azure Data Studio):
`01-create-schema.sql`, then `02-seed-data.sql`, then `04-seed-messages.sql` (sample messages and announcements), then `03-demo-queries.sql` to check the data.
The seed script uses dates relative to today, so there are always sessions today, upcoming and in the past.
Full Azure steps are in [`docs/deployment-guide.md`](docs/deployment-guide.md).

### 2. API

Requires the [.NET 8 SDK](https://dotnet.microsoft.com/download).

```bash
cd api/src/Lpms.Api
# point the API at your database (or edit ConnectionStrings:LpmsDb in appsettings.json)
export ConnectionStrings__LpmsDb="Server=tcp:YOUR-SERVER.database.windows.net,1433;Database=LpmsDb;User ID=...;Password=...;Encrypt=True;"
dotnet run          # Development profile: http://localhost:5080/swagger
```

In Development a throw-away signing key is supplied by `appsettings.Development.json`. In any other environment set
`Jwt__Key` to a random string of at least 32 characters.

### 3. Android app

Open the `android/` folder in Android Studio (Ladybug or newer, JDK 17) and run it on a phone or emulator, or build from the command line:

```bash
cd android
./gradlew assembleDebug -PlpmsApiBaseUrl=https://YOUR-APP.azurewebsites.net/
# APK: app/build/outputs/apk/debug/app-debug.apk
```

The server address can also be changed inside the app (sign-in screen > **Server settings**, or Settings > **Change server address**),
which is handy for a PC on the same Wi-Fi: `http://192.168.x.x:5080/` (debug builds allow plain http; release builds require https).

### Demo accounts

All seeded accounts use the password **`Lpms@2026!`**.

| Username | Role | Notes |
|---|---|---|
| `teacher01` | Teacher | Nomsa Zulu, Soweto Primary - the main demo account |
| `teacher02` ... `teacher11` | Teacher | one per school |
| `teacher12` | Teacher | **inactive**: shows the "account inactive" message |
| `coord01`, `coord02` | Coordinator | can sign in to Swagger; the mobile app is for teachers and parents |
| `parent01` ... `parent10` | Parent | `parent01` and `parent02` each have two children (the best demo accounts) |
| `admin01` | Administrator | Swagger only |

## Tests

| Suite | Command | What it covers |
|---|---|---|
| API (xUnit, 100+ tests) | `dotnet test api/Lpms.sln` | every endpoint end-to-end over HTTP with an in-memory SQLite database: sign-in, roles, validation, attendance, progress, dashboard, parent access, business rules |
| Android (JUnit, 82 tests) | `cd android && ./gradlew testDebugUnitTest` | validators (incl. create-account and new-session rules), formatters, attendance rules, parent report rules, error mapping, JSON contract with the API |

Both suites run automatically on every push (see **CI/CD** below). The manual test table is in [`docs/test-plan.md`](docs/test-plan.md).

## CI/CD and the DevOps lifecycle

Pipelines: [`.github/workflows/ci.yml`](.github/workflows/ci.yml) (GitHub Actions) and [`azure-pipelines.yml`](azure-pipelines.yml) (Azure DevOps).
Each push builds and tests the API, runs the Android unit tests, builds a debug APK (downloadable from the run's **Artifacts**), and,
on `main`, deploys the API to Azure App Service and smoke-tests `/api/health`.

| Stage | How it is done in this project |
|---|---|
| Plan | Azure DevOps Boards: epics/user stories from the RAD, sprint backlog, task board |
| Code | Git feature branches, small commits, pull requests into `main` |
| Build | `dotnet build`, `./gradlew assembleDebug` |
| Test | xUnit API tests, JUnit app tests, manual test plan |
| Release | versioned build artifacts (published API, APK) |
| Deploy | `azure/webapps-deploy` / `AzureWebApp@1` to App Service; database scripts for Azure SQL |
| Operate | Azure App Service + Azure SQL; configuration through App Service settings (`Jwt__Key`, connection string) |
| Monitor | `GET /api/health` smoke test, App Service logs and metrics |

More detail: [`docs/devops-lifecycle.md`](docs/devops-lifecycle.md).

### How GitHub and GitHub Actions are used

The whole project (app, API, database scripts and docs) lives in this one repository so a single clone contains everything. Work is committed in small steps with messages that say what changed.
The workflow [`ci.yml`](.github/workflows/ci.yml) runs on every push and pull request to `main` with three jobs:

1. **API - build and test:** restores, builds and runs the xUnit tests, uploads the test results and the published API as artifacts.
2. **Android - unit tests and debug APK:** runs the JUnit tests, builds the debug APK and uploads it (and the test report) as downloadable artifacts.
3. **Deploy:** on `main`, when the Azure publish profile secret is set, deploys the API to Azure App Service and smoke-tests `/api/health`.

The pipeline has caught real mistakes during development (for example, wrong imports that broke the Android build), which is exactly its purpose.
To get the APK without Android Studio: open the latest green run under **Actions**, then download the `lpms-debug-apk` artifact.

## Release notes

Full notes: [`RELEASE_NOTES.md`](RELEASE_NOTES.md). Summary of what changed since the Task 2 prototype (1.0):

**Innovative features added in 1.1 (final)**

- **Parent and teacher messaging:** one conversation per learner between a parent and the teachers at that school, with access checked in the API.
- **Targeted announcements:** coordinators and administrators post to Everyone, Teachers or Parents, for all schools or one school.
- **Attendance that guides the teacher:** Present, Absent and Not marked tiles, and a save button that counts down what is left so a register is never saved half done.

**Other changes:** Create account and New session screens, Messages tab, database grown to 12 tables (Message, Announcement) with seed data, about 35 new automated tests, GitHub Actions pipeline, updated documentation.

## Single sign-on and publication status

- **SSO:** the group's Firebase build ([PSYCHOHITMAN/LPMS-Mobile](https://github.com/PSYCHOHITMAN/LPMS-Mobile)) has Google sign-in. This repository signs users in with its own API (JWT and BCrypt); Google sign-in for the API and the website is the next planned step and is not built yet.
- **Publication:** the group repository holds release signing, store listing text and release notes. A Google Play Console upload has not been done. This repository builds a debug APK in CI on every push.
- **Hosting:** the Azure SQL database and App Service exist; at the time of writing the hosted API still reported its database as unavailable, which is being investigated (the health endpoint now logs the error).

## Documentation

- [User guide](docs/user-guide.md) - how to use every screen of the app
- [Deployment guide](docs/deployment-guide.md) - Azure SQL, App Service, secrets, installing the APK
- [API reference](docs/api-reference.md) - endpoints, examples, error format
- [Test plan](docs/test-plan.md) - automated tests and the manual test table
- [Demo video script](docs/demo-video-script.md) - what to show and say
- [DevOps lifecycle](docs/devops-lifecycle.md)

## Changes from the System Design Document

| Change | Why |
|---|---|
| `User.TeacherID` (nullable FK to `Teacher`) | links a sign-in account to the teacher it belongs to, so the API can scope data to the teacher |
| At least 10 rows in every table (SDD sample data had 3) | the Task 2 brief requires it |
| Sign-in accepts email **or** username | teachers know both; `Email` and `Username` are both unique |
| Assessment names stored in `Progress.Notes` | the SDD `Progress` table has no separate name column |
| New `ParentLearner` table and `Parent` role | lets a parent sign in and see only their own children's attendance and progress (Task 2 extension, not in the SDD) |
| Seed dates relative to the day the script runs | there are always sessions "today" for the demo |

## Team

Team Point Blank - XISD6329.
