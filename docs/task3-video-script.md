# Task 3 video presentation script (aim for 8-10 minutes)

Record the phone screen (Android screen recorder with microphone on) and the PC screen, join the clips in Clipchamp, upload as unlisted and put the link in the report.
Speak in your own words; the italics are a guide. Be honest about what is not finished: it scores better than overclaiming.

## Before recording
- Phone signed out, Do Not Disturb on. Seed data loaded, including `04-seed-messages.sql`.
- Tabs ready: Azure Web App, `/swagger`, Azure SQL Query editor, GitHub repo (README), GitHub Actions run, Azure DevOps board, group repo, website.
- If the hosted API still says "database unavailable", run the API locally or against Azure SQL from your PC and say so in the video. Do not hide it.

## 1. Intro (30 s)
*"I'm Siyabonga Ndlovu from Team Point Blank. This is LPMS for READ Educational Trust: an Android app for teachers and parents, a website for managers, a REST API and a relational database. My part is the API, the database, the Android app and the pipeline; Percy's group build uses Firebase. I'll show both."*

## 2. The problem and design (45 s, title slide or README)
*"Attendance and progress are on paper and spreadsheets, so reports are late. The app lets a teacher record attendance on a phone in two taps and lets parents see their own child."*

## 3. App demo on a real phone (4 min)
1. Sign in: empty fields (validation), wrong password, then `teacher01` / `Lpms@2026!`.
2. Home: *"sessions this week, learner count, attendance rate."*
3. Sessions > open a session > attendance: show "Mark N more to save", **All present**, change one to Absent, Save. Mention the Present / Absent / Not marked tiles.
4. New session: pick programme, date and time (clash check).
5. Learners > a learner > add assessment (101 shows an error, 72.5 saves) > average updates.
6. **Messages (new):** open a conversation, send a message. Switch to `parent01`: *"the parent sees the same conversation"*, reply. Read an announcement.
7. Parent: child report, download the PDF.
8. Settings: dark theme, large text, server address.

## 4. Hosted services and database (1.5 min, PC)
Azure Web App (Running) > `/swagger` > login > `GET /api/messages/conversations` > Azure SQL Query editor: `Message` and `Announcement` rows, `User` table with BCrypt hashes.
*"Access rules are in the API: a parent gets 403 for a child who is not theirs."*

## 5. Website and the group build (1 min)
Open the website (dashboard, schools, reports). *"This is the Task 1 website."* Then the group app: **Google sign-in** screen. *"Single sign-on with Google exists in the group's Firebase build. In my own API and the website it is the next step; I did not finish it."*

## 6. GitHub, GitHub Actions and DevOps (1 min)
README (screenshots, release notes), commit history, **Actions** run (API tests, Android tests, APK artifact), Azure DevOps board.
*"Every push is built and tested automatically. It caught real mistakes."*

## 7. Preparation for publication (30 s)
Show release notes, the group's `STORE_LISTING.md` and `RELEASING.md` (signing). *"The Play Console upload itself is not done."*

## 8. Reflection and close (30 s)
*"What I would do differently: tackle cloud setup and sign-in first and keep evidence as I go. Thank you."*

## Checklist
- [ ] Voice clear; only demo passwords on screen.
- [ ] Shown: phone app, messaging, API, database, website, Google sign-in (group), GitHub Actions, README.
- [ ] Video link pasted into section 1 of the report and the README.
