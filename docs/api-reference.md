# LPMS REST API reference

Base URL (hosted): `https://<your-app>.azurewebsites.net` - interactive documentation at `/swagger`.
Local development: `http://localhost:5080`.

All bodies are JSON (camelCase). Dates are `yyyy-MM-dd`, times `HH:mm:ss`, timestamps ISO-8601 UTC.

## Authentication

1. `POST /api/auth/login` with an email address or username and a password returns a JWT.
2. Send it on every other request: `Authorization: Bearer <token>`.
3. Tokens last 8 hours. A missing/expired/invalid token returns **401** with an empty body.

Passwords are verified against BCrypt hashes in the `User` table. Login is rate limited (default 10 attempts per minute per client; **429** when exceeded).

## Roles

| Role | Can |
|---|---|
| Parent | only the children linked to the parent: `/api/parent/children` and `/api/learners/{id}/progress` for those children |
| Teacher | own sessions, schedule own sessions, take attendance, learners at own school, own assessments, dashboard |
| Coordinator, Administrator | view any session, schedule sessions, list learners (filter by school), lookups |

## Endpoints

| Method and path | Roles | Purpose |
|---|---|---|
| `POST /api/auth/login` | anonymous | Sign in, returns token + profile |
| `GET /api/auth/schools` | anonymous | Schools to pick from on the Create account screen (id and name only) |
| `POST /api/auth/register` | anonymous | Create a teacher account (`fullName`, `email`, `schoolId`, `password`); returns token + profile (201). 400 for a bad name, email, school or weak password; 409 if the email is already used. Rate limited like login |
| `GET /api/auth/me` | any | Profile of the signed-in user |
| `POST /api/auth/change-password` | any | Change own password (204) |
| `GET /api/sessions/mine?scope=upcoming\|past\|all` | Teacher | Own sessions with status and attendance counts |
| `GET /api/sessions/{id}` | Teacher (own only), Coordinator, Administrator (parents get 403) | Session plus learner roster and recorded statuses |
| `PUT /api/sessions/{id}/attendance` | Teacher (own session) | Record or correct attendance; returns the session detail |
| `POST /api/sessions` | Teacher, Coordinator, Administrator | Schedule a session (201). Teachers send only programme, date and times; the teacher and school come from the token (403 if they send someone else's). Coordinators and administrators also send `schoolId` and `teacherId` |
| `GET /api/learners?search=&schoolId=` | Teacher, Coordinator, Administrator (teachers: own school; parents get 403) | Learners with average score, attendance rate, assessment count |
| `GET /api/learners/{id}/progress` | any (teachers: own school; parents: linked children only) | Learner, progress label, assessments, attendance history |
| `GET /api/parent/children` | Parent | The parent's children with average score, attendance rate, assessment count |
| `POST /api/progress` | Teacher | Add an assessment (201) |
| `PUT /api/progress/{id}` | Teacher (own entry) | Edit an assessment |
| `DELETE /api/progress/{id}` | Teacher (own entry) | Delete an assessment (204) |
| `GET /api/dashboard/teacher` | Teacher | Counts for the home screen |
| `GET /api/schools`, `GET /api/programmes` | any | Reference lists |
| `GET /api/health` | anonymous | 200 when the database is reachable, otherwise 503 |

## Examples

### Sign in

```http
POST /api/auth/login
Content-Type: application/json

{ "identifier": "teacher01", "password": "Lpms@2026!" }
```

```json
{
  "token": "eyJhbGciOiJIUzI1NiIs...",
  "expiresAtUtc": "2026-10-17T14:02:11.5Z",
  "user": {
    "userId": 4, "username": "teacher01", "fullName": "Nomsa Zulu", "email": "nomsa@read.org.za",
    "role": "Teacher", "teacherId": 1, "schoolId": 1, "schoolName": "Soweto Primary"
  }
}
```

### Record attendance

```http
PUT /api/sessions/7/attendance
Authorization: Bearer <token>

{ "entries": [ { "learnerId": 1, "status": "Present" }, { "learnerId": 2, "status": "Absent" } ] }
```

Returns the updated `SessionDetail` (`session` summary with `status: "Completed"` and the `learners` roster).

### Add an assessment

```http
POST /api/progress
Authorization: Bearer <token>

{ "learnerId": 1, "assessmentScore": 72.5, "assessmentDate": "2026-10-14", "notes": "Reading comprehension test" }
```

### curl quick test

```bash
BASE=https://lpms-api-yourname.azurewebsites.net
TOKEN=$(curl -s $BASE/api/auth/login -H 'Content-Type: application/json' \
  -d '{"identifier":"teacher01","password":"Lpms@2026!"}' | python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])')
curl -s $BASE/api/sessions/mine?scope=upcoming -H "Authorization: Bearer $TOKEN"
```

## Errors

Errors use the standard `application/problem+json` shape:

```json
{ "type": "...", "title": "Invalid request", "status": 400, "detail": "The assessment date cannot be in the future." }
```

Automatic model validation returns `title: "One or more validation errors occurred."` with an `errors` object keyed by field.

| Status | Meaning |
|---|---|
| 400 | Invalid input (missing field, score outside 0-100, date in the future, unknown status, ...) |
| 401 | Not signed in, token expired/invalid, or wrong password at login |
| 403 | Signed in but not allowed (another teacher's data, wrong role, inactive account) |
| 404 | Session, learner or assessment not found |
| 409 | Clash (teacher already booked at that time; attendance changed at the same time) |
| 429 | Too many login attempts |
| 500 | Unexpected server error (generic JSON body, details only in the server log) |
| 503 | `/api/health` only: database unreachable |

## Business rules enforced by the API

- Attendance status must be `Present` or `Absent`; each learner only once per request; learners must belong to the session's school;
  attendance cannot be recorded for a future session. Recording again **corrects** the existing row (unique on session + learner).
- Assessment score 0-100 (2 decimals), date not in the future, notes at most 255 characters.
- Registration: first and last name, valid email (unique across users and teachers), existing school, password policy; the username is made from the email's first part (a number is added if taken).
- New sessions: end after start, date inside the programme's dates, teacher must work at the school, no overlap with the teacher's other sessions.
- Session status: **Completed** (attendance recorded), **Pending** (finished, no attendance), **Upcoming** (anything else). Times are South African time.
- New passwords: 8-100 characters with an uppercase letter, a lowercase letter and a digit, different from the old one.
