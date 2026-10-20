# Demo video script (aim for 6-8 minutes)

The brief requires: the app running with a **voice-over**, and the **data in the online-hosted authentication service, API and database**.
Record the phone screen (Android: swipe down > *Screen record*, tick *Record audio* / microphone) and your PC screen (OBS, Xbox Game Bar `Win+G`, or Teams/Zoom "record").
Cut the clips together in any editor (Clipchamp is free on Windows) and upload to YouTube (unlisted), OneDrive or the module's upload point.

Say the words in *italics* in your own voice - they are a guide, not a script to read robotically.

## Before you press record

- Run through [`deployment-guide.md` section 6](deployment-guide.md#6-before-recording-the-demo-video).
- Browser tabs ready: (1) Azure portal > Web App overview, (2) `/swagger`, (3) Azure SQL Query editor with `03-demo-queries.sql` open, (4) GitHub Actions run, (5) GitHub repo, (6) Azure DevOps board.
- Phone signed out, app on the sign-in screen, Do Not Disturb on.

## Scene 1 - Intro (30 s) - face the camera or show the title slide

*"Hi, I'm <name> from Team Point Blank. This is the LPMS app (teacher and parent views) for READ Educational Trust. It's an Android app that talks to our own REST API on Azure App Service, which stores everything in an Azure SQL database. I'll show the hosted services first, then the app."*

## Scene 2 - The hosted services (PC, ~2.5 min)

1. **Azure portal - App Service** (show the name, URL, status *Running*). *"This is the API, hosted on Azure App Service."*
2. Open `.../api/health`. *"The health endpoint confirms the API can reach the database."*
3. Open `.../swagger`. *"This is the API documentation generated from the code."*
4. **POST /api/auth/login** > Try it out > `{"identifier":"teacher01","password":"Lpms@2026!"}` > Execute. Point at the **token** and the profile.
   *"Sign-in is handled by our own API. It checks the password against a BCrypt hash and returns a signed JWT."*
5. Show a wrong password (401) quickly. *"A wrong password is rejected."*
6. **Authorize** with the token > **GET /api/sessions/mine** > Execute > show the JSON. *"Endpoints are protected: no token, no data."* (Optionally call it without Authorize to show 401.)
7. **Azure SQL > Query editor** (this is the authentication store and database):
   - Query 1 (row counts) - *"Ten tables, each with at least ten rows."*
   - Query 2 (`User` table) - *"This is the authentication store. Passwords are BCrypt hashes, never plain text; only the first characters are shown here."*
   - Query 3/4 (teacher01's sessions and attendance) - *"These rows are what the app will show."*

## Scene 3 - The app on the phone (~3.5 min)

Show the phone screen recording and talk over it.

1. **Sign in screen.** Tap **Sign in** with empty boxes (validation messages), then a wrong password (error message), then `teacher01` / `Lpms@2026!`. *"Invalid input never crashes the app - it explains the problem."*
2. **Home.** *"The dashboard comes from GET /api/dashboard/teacher."*
3. **Sessions** > Upcoming/Past. Open a session that needs attendance.
4. **Take attendance:** All present, change one learner to Absent, **Save attendance**. *"This is a PUT to the API."* Switch to the PC and re-run query 4 (attendance) - *"and here it is in the Azure SQL database."*
5. **Learners:** search by name, open a learner. **Add assessment**: enter `101` (show the error), then `72.5`, pick a date, Save. Show the average change. Re-run the progress query in Azure - *"stored in the Progress table."*
6. **Settings:** switch to Dark theme, Larger text, Remember username; show **Server** > Save and test (Connected). Change password (show a validation error) - or just show the screen.
7. **Help:** open Help and expand a question.
8. **Sign out** and show you land on the sign-in screen.
9. **Parent view (1 min):** sign in as `parent01`. *"Parents have their own login; the API only returns their own children."* Open a child, show the attendance tag, open Settings and change the **attendance expectation** to show the tag flip, go back and tap **Download PDF report**, open the PDF. On the PC, run the `ParentLearner` query to show the link table.

## Scene 4 - DevOps and quality (~1 min, PC)

- GitHub: commit history (multiple commits), README, the **Actions** run with green API tests + Android tests + APK artifact + deploy.
- Azure DevOps board with the sprint/user stories and the pipeline run.
  *"Every push is built and tested automatically, the API is deployed to Azure, and the APK is built as an artifact."*

## Scene 5 - Wrap-up (15 s)

*"That's LPMS: Android app, our own REST API, and an Azure SQL database, with automated tests and a CI/CD pipeline. Thank you."*

## Checklist before submitting the video

- [ ] Voice is clear throughout; no passwords other than the demo ones on screen.
- [ ] Shown: authentication data (`User` table with hashes), API (Swagger), database (Azure SQL), app on a real phone.
- [ ] Shown: invalid input handled without a crash.
- [ ] Link works for someone who isn't you (unlisted / "anyone with the link").
