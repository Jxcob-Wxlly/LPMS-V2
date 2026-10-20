#!/usr/bin/env python3
"""
LPMS seed-data generator.

Writes database/02-seed-data.sql (T-SQL for SQL Server / Azure SQL) with simulated
data - at least ten rows in every table, as required by the XISD6329 Task 2 brief.
With --check it also loads the same data into an in-memory SQLite replica and
verifies row counts, foreign keys and the business rules, so mistakes are caught
before the script is run in Azure.

Dates are written relative to the day the SQL script is run (DATEADD on @today),
so the demo always has upcoming sessions and recent history.

Usage:   python3 database/tools/generate-seed.py --check
Needs:   pip install bcrypt
"""
from __future__ import annotations

import argparse
import random
import sqlite3
import sys
from pathlib import Path

import bcrypt

DEMO_PASSWORD = "Lpms@2026!"
PARENTS = [("Zodwa", "Mthembu"), ("Sandile", "Radebe"), ("Precious", "Ndaba"), ("Lerato", "Sithole"),
           ("Vusi", "Mahlangu"), ("Nomvula", "Zwane"), ("Tebogo", "Molefe"), ("Anele", "Jacobs"),
           ("Fatima", "Patel"), ("Pieter", "van Wyk")]
RELATIONSHIPS = ["Mother", "Father", "Guardian"]
BCRYPT_ROUNDS = 11
RNG_SEED = 2026

# ----------------------------------------------------------------------------
# Static data
# ----------------------------------------------------------------------------
SCHOOLS = [
    # id, name, address, principal, phone, email
    (1, "Soweto Primary", "123 Main Rd, Soweto", "Ms Dlamini", "011-555-0001", "soweto@read.org.za"),
    (2, "Tembisa High", "45 Oak Ave, Tembisa", "Mr Khumalo", "011-555-0002", "tembisa@read.org.za"),
    (3, "Alex Academy", "8 River St, Alexandra", "Mrs Mokoena", "011-555-0003", "alex@read.org.za"),
    (4, "Mamelodi Primary", "22 Church St, Mamelodi", "Mr Maseko", "012-555-0004", "mamelodi@read.org.za"),
    (5, "Diepsloot Junior School", "14 Hope Rd, Diepsloot", "Ms Radebe", "011-555-0005", "diepsloot@read.org.za"),
    (6, "Orange Farm Primary", "67 Unity St, Orange Farm", "Mr Ngcobo", "011-555-0006", None),
    (7, "Katlehong Primary", "9 Mandela Dr, Katlehong", "Mrs Molefe", "011-555-0007", "katlehong@read.org.za"),
    (8, "Thokoza Combined School", "31 Ubuntu Ave, Thokoza", "Mr Zwane", "011-555-0008", "thokoza@read.org.za"),
    (9, "Kagiso Primary", "5 Station Rd, Kagiso", "Ms Tshabalala", "011-555-0009", "kagiso@read.org.za"),
    (10, "Ivory Park Primary", "88 Sunrise St, Ivory Park", "Mrs Mahlangu", "011-555-0010", "ivorypark@read.org.za"),
]

TEACHERS = [
    # id, first, last, school_id, phone (None = optional field left empty)
    (1, "Nomsa", "Zulu", 1, "082-111-0001"),
    (2, "Themba", "Nkosi", 2, "082-111-0002"),
    (3, "Lindiwe", "Dube", 1, "082-111-0003"),
    (4, "Sipho", "Mahlaba", 3, "082-111-0004"),
    (5, "Thandi", "Mkhize", 4, "082-111-0005"),
    (6, "Bongani", "Cele", 5, "082-111-0006"),
    (7, "Zanele", "Khoza", 6, None),
    (8, "Mpho", "Letsoalo", 7, "082-111-0008"),
    (9, "Naledi", "Mokwena", 8, "082-111-0009"),
    (10, "Kagiso", "Mabena", 9, "082-111-0010"),
    (11, "Palesa", "Moloi", 10, "082-111-0011"),
    (12, "Ayanda", "Buthelezi", 2, "082-111-0012"),
]

# Learners 1-3 come from the System Design Document sample data; 4-11 complete
# school 1 (the demo teacher's school) with the names used in the wireframes.
LEARNERS_FIXED = [
    (1, "Sipho", "Mthembu", "Grade 4", 1),
    (2, "Ayanda", "Mokoena", "Grade 5", 1),
    (3, "Lethabo", "Sithole", "Grade 3", 2),
    (4, "Amahle", "Dlamini", "Grade 3", 1),
    (5, "Bongani", "Khumalo", "Grade 4", 1),
    (6, "Cebo", "Mthethwa", "Grade 3", 1),
    (7, "Dineo", "Molefe", "Grade 2", 1),
    (8, "Ethan", "Naidoo", "Grade 3", 1),
    (9, "Lerato", "Mokoena", "Grade 3", 1),
    (10, "Sophia", "Zulu", "Grade 3", 1),
    (11, "Faith", "Nkosi", "Grade 4", 1),
]
# How many learners each school should end up with (total 50)
LEARNERS_PER_SCHOOL = {1: 10, 2: 8, 3: 6, 4: 4, 5: 4, 6: 4, 7: 4, 8: 4, 9: 3, 10: 3}
FIRST_NAMES = [
    "Thabo", "Naledi", "Karabo", "Lwazi", "Zinhle", "Mandla", "Nomvula", "Tumelo", "Refilwe", "Sizwe",
    "Anele", "Palesa", "Kgomotso", "Nhlanhla", "Busisiwe", "Tshepo", "Lindani", "Zodwa", "Siphiwe", "Mbali",
    "Oratile", "Khanyi", "Vusi", "Rethabile", "Ntombi", "Jabu", "Onalenna", "Sbu", "Neo", "Aphiwe",
]
LAST_NAMES = [
    "Mahlangu", "Ndlovu", "Sibiya", "Mazibuko", "Motaung", "Radebe", "Nxumalo", "Phiri", "Mabaso", "Tau",
    "Zondo", "Ledwaba", "Mdluli", "Shange", "Masilela", "Kubheka", "Moyo", "Gumede", "Baloyi", "Ngwenya",
]
GRADES_PRIMARY = ["Grade R", "Grade 1", "Grade 2", "Grade 3", "Grade 4", "Grade 5", "Grade 6", "Grade 7"]

PROGRAMMES = [
    # id, name, description, start offset (days), end offset (days)
    (1, "Literacy Boost", "Foundation phase literacy", -250, 45),
    (2, "Read Aloud", "Oral reading programme", -230, 60),
    (3, "Phonics First", "Phonics-based intervention", -270, 30),
    (4, "Read to Lead", "Reading fluency and leadership for Grades 3 to 5", -200, 90),
    (5, "Story Stars", "Storytelling and listening comprehension", -190, 75),
    (6, "Let's Read", "After-school reading club", -180, 100),
    (7, "Word Builders", "Vocabulary and spelling workshops", -160, 120),
    (8, "Comprehension Club", "Reading comprehension strategies", -150, 110),
    (9, "Library Buddies", "Peer reading in school libraries", -140, 130),
    (10, "Rising Readers", "Early reader support for Grades R to 2", -130, 140),
]

SLOTS = [("08:00", "09:00"), ("09:00", "10:00"), ("10:30", "11:30"), ("13:00", "14:00")]
# teacher_id -> [(day offset from today, slot index, programme_id)]
SESSION_PLAN = [
    (1, [(-28, 1, 1), (-21, 1, 4), (-14, 1, 5), (-7, 1, 1), (-3, 2, 4),
         (0, 1, 1), (0, 3, 5), (2, 1, 4), (5, 2, 1), (9, 1, 5)]),
    (3, [(-10, 0, 3), (-2, 0, 3), (1, 0, 3), (6, 0, 3)]),
    (2, [(-12, 1, 2), (-5, 1, 2), (0, 2, 2), (4, 1, 2)]),
    (12, [(-6, 3, 2), (3, 3, 2)]),
    (4, [(-4, 1, 7), (2, 1, 7)]),
    (5, [(-4, 2, 8), (2, 2, 8)]),
    (6, [(-4, 1, 9), (2, 1, 9)]),
    (7, [(-4, 0, 10), (2, 0, 10)]),
    (8, [(-4, 3, 6), (2, 3, 6)]),
    (9, [(-4, 1, 7), (2, 1, 7)]),
    (10, [(-4, 2, 8), (2, 2, 8)]),
    (11, [(-4, 1, 9), (2, 1, 9)]),
]
# Past session whose attendance is deliberately NOT recorded (shows "Attendance due" in the app)
UNRECORDED = {(1, -3)}

ASSESSMENTS = ["Phonics test", "Reading comprehension", "Vocabulary quiz", "Fluency check", "Spelling test"]
COMMENTS = ["Good improvement", "Needs support", "Excellent progress", "Steady progress",
            "Struggles with blends", "Confident reader", "Improving fluency"]
# Matches the Learner Progress wireframe for learner 4 (Amahle Dlamini)
PROGRESS_FIXED = {
    4: [("Phonics test", 82.0, -9, "Excellent progress"),
        ("Reading comprehension", 75.0, -16, "Steady progress"),
        ("Vocabulary quiz", 76.0, -23, "Good improvement")],
}

RESOURCES = [
    # id, name, type, school_id, quantity, days ago
    (1, "Grade 4 Readers", "Books", 1, 50, 250),
    (2, "Pencil Sets", "Stationery", 2, 100, 250),
    (3, "Grade 3 Workbooks", "Books", 3, 60, 220),
    (4, "Phonics Flash Cards", "Stationery", 1, 40, 200),
    (5, "Story Time Picture Books", "Books", 4, 75, 190),
    (6, "Reading Tablets", "Digital", 5, 12, 180),
    (7, "Grade 2 Readers", "Books", 6, 45, 170),
    (8, "Exercise Books", "Stationery", 7, 200, 160),
    (9, "Audio Story Library", "Digital", 8, 10, 150),
    (10, "Bookshelves", "Other", 9, 6, 140),
    (11, "Grade 5 Readers", "Books", 10, 55, 120),
    (12, "Highlighter Packs", "Stationery", 1, 30, 100),
    (13, "Phonics Software Licences", "Digital", 2, 25, 80),
    (14, "Reading Corner Cushions", "Other", 3, 15, 60),
]

ADMIN_COORD_USERS = [
    # username, role, full name, email
    ("admin01", "Administrator", "John Admin", "admin@read.org.za"),
    ("coord01", "Coordinator", "Sarah Coord", "sarah@read.org.za"),
    ("coord02", "Coordinator", "Busisiwe Nel", "busisiwe@read.org.za"),
]
INACTIVE_TEACHER_ID = 12  # demonstrates IsActive = 0 (login is refused)


# ----------------------------------------------------------------------------
# Data builders
# ----------------------------------------------------------------------------
def build_data() -> dict:
    rng = random.Random(RNG_SEED)

    teacher_rows = [
        (tid, first, last, sid, f"{first.lower()}.{last.lower()}@read.org.za", phone)
        for tid, first, last, sid, phone in TEACHERS
    ]
    teacher_school = {t[0]: t[3] for t in teacher_rows}

    # learners
    learners = list(LEARNERS_FIXED)
    used = {(l[1], l[2]) for l in learners}
    counts = {sid: sum(1 for l in learners if l[4] == sid) for sid in LEARNERS_PER_SCHOOL}
    next_id = len(learners) + 1
    for sid, target in LEARNERS_PER_SCHOOL.items():
        while counts[sid] < target:
            first, last = rng.choice(FIRST_NAMES), rng.choice(LAST_NAMES)
            if (first, last) in used:
                continue
            used.add((first, last))
            learners.append((next_id, first, last, rng.choice(GRADES_PRIMARY), sid))
            counts[sid] += 1
            next_id += 1
    learners.sort(key=lambda l: l[0])
    learners_by_school: dict[int, list[int]] = {}
    for l in learners:
        learners_by_school.setdefault(l[4], []).append(l[0])

    # sessions + attendance
    sessions, attendance = [], []
    sid_counter = 1
    att_counter = 1
    for teacher_id, plan in SESSION_PLAN:
        school_id = teacher_school[teacher_id]
        for offset, slot, programme_id in plan:
            start, end = SLOTS[slot]
            recorded = offset < 0 and (teacher_id, offset) not in UNRECORDED
            sessions.append((sid_counter, programme_id, school_id, teacher_id, offset, start, end, recorded))
            if recorded:
                for learner_id in learners_by_school[school_id]:
                    status = "Present" if rng.random() < 0.85 else "Absent"
                    attendance.append((att_counter, sid_counter, learner_id, status))
                    att_counter += 1
            sid_counter += 1

    # progress
    teachers_by_school: dict[int, list[int]] = {}
    for t in teacher_rows:
        teachers_by_school.setdefault(t[3], []).append(t[0])
    progress = []
    pid = 1
    for l in learners:
        learner_id, school_id = l[0], l[4]
        teacher_pool = teachers_by_school[school_id]
        entries = []
        if learner_id in PROGRESS_FIXED:
            entries = [(score, off, f"{name} - {comment}") for name, score, off, comment in PROGRESS_FIXED[learner_id]]
        else:
            n = 3 if school_id == 1 else (2 if rng.random() < 0.5 else 1)
            for off in sorted(rng.sample(range(-70, -1), n)):
                score = round(rng.uniform(42, 96), 1)
                entries.append((score, off, f"{rng.choice(ASSESSMENTS)} - {rng.choice(COMMENTS)}"))
        for i, (score, off, notes) in enumerate(entries):
            teacher_id = teacher_pool[(learner_id + i) % len(teacher_pool)]
            progress.append((pid, learner_id, teacher_id, score, off, notes))
            pid += 1

    # users (hash every password with its own salt)
    def hash_pw() -> str:
        # $2a$ prefix: what BCrypt.Net-Next (used by the API) writes and reads; identical to $2b$
        # for passwords shorter than 256 bytes.
        hashed = bcrypt.hashpw(DEMO_PASSWORD.encode(), bcrypt.gensalt(BCRYPT_ROUNDS)).decode()
        return "$2a$" + hashed[4:]

    users = []
    uid = 1
    for username, role, full, email in ADMIN_COORD_USERS:
        users.append((uid, username, hash_pw(), role, full, email, 1, None))
        uid += 1
    for tid, first, last, _sid, _phone in TEACHERS:
        users.append((uid, f"teacher{tid:02d}", hash_pw(), "Teacher", f"{first} {last}",
                      f"{first.lower()}@read.org.za", 0 if tid == INACTIVE_TEACHER_ID else 1, tid))
        uid += 1

    # parent logins and the children they may see (ParentLearner)
    parent_links = []
    for i, (first, last) in enumerate(PARENTS, start=1):
        users.append((uid, f"parent{i:02d}", hash_pw(), "Parent", f"{first} {last}",
                      f"{first.lower()}.{last.lower()}@parents.example.org", 1, None))
        kids = [i, i + 10] if i <= 2 else [i]          # parent01/02 have two children
        for k, learner_id in enumerate(kids):
            parent_links.append((len(parent_links) + 1, uid, learner_id, RELATIONSHIPS[(i + k) % 3]))
        uid += 1
    # one extra link so a child has two guardians
    parent_links.append((len(parent_links) + 1, uid - 1, 1, "Guardian"))

    return {
        "parent_links": parent_links,
        "schools": SCHOOLS,
        "programmes": PROGRAMMES,
        "teachers": teacher_rows,
        "learners": learners,
        "sessions": sessions,
        "attendance": attendance,
        "progress": progress,
        "resources": RESOURCES,
        "users": users,
    }


# ----------------------------------------------------------------------------
# T-SQL emission
# ----------------------------------------------------------------------------
def q(value) -> str:
    """Quote a Python value as a T-SQL literal."""
    if value is None:
        return "NULL"
    if isinstance(value, bool):
        return "1" if value else "0"
    if isinstance(value, (int, float)):
        return repr(value)
    return "N'" + str(value).replace("'", "''") + "'"


def rel(offset: int) -> str:
    return f"DATEADD(DAY, {offset}, @today)"


def insert_block(table: str, columns: list[str], rows: list[list[str]]) -> str:
    body = ",\n".join("    (" + ", ".join(r) + ")" for r in rows)
    return (
        f"SET IDENTITY_INSERT dbo.{table} ON;\n"
        f"INSERT INTO dbo.{table} ({', '.join(columns)}) VALUES\n{body};\n"
        f"SET IDENTITY_INSERT dbo.{table} OFF;\n"
    )


def emit_sql(d: dict) -> str:
    parts = [
        "/* =====================================================================\n"
        "   LPMS - Literacy Programme Management System\n"
        "   File    : 02-seed-data.sql   (GENERATED by database/tools/generate-seed.py)\n"
        "   Target  : Microsoft SQL Server / Azure SQL Database (T-SQL)\n"
        "   Purpose : Simulated demo data - at least ten rows in every table.\n"
        f"   Demo login : every seeded user has the password  {DEMO_PASSWORD}\n"
        "               e.g. teacher01 (Nomsa Zulu, Soweto Primary), coord01, admin01.\n"
        "               teacher12 is inactive on purpose (login is refused).\n"
        "   Dates are relative to the day this script runs, so the demo always has\n"
        "   upcoming sessions. Run 01-create-schema.sql first. Re-run this file only\n"
        "   after re-running the schema script (it does not clear existing rows).\n"
        "   ===================================================================== */\n",
        "SET NOCOUNT ON;",
        "DECLARE @today DATE = CAST(SYSDATETIMEOFFSET() AT TIME ZONE 'South Africa Standard Time' AS DATE);\n",
    ]
    parts.append(insert_block(
        "School", ["SchoolID", "SchoolName", "Address", "PrincipalName", "ContactNumber", "ContactEmail"],
        [[q(c) for c in r] for r in d["schools"]]))
    parts.append(insert_block(
        "Programme", ["ProgrammeID", "ProgrammeName", "Description", "StartDate", "EndDate"],
        [[q(r[0]), q(r[1]), q(r[2]), rel(r[3]), rel(r[4])] for r in d["programmes"]]))
    parts.append(insert_block(
        "Teacher", ["TeacherID", "FirstName", "LastName", "SchoolID", "ContactEmail", "ContactNumber"],
        [[q(c) for c in r] for r in d["teachers"]]))
    parts.append(insert_block(
        "Learner", ["LearnerID", "FirstName", "LastName", "Grade", "SchoolID"],
        [[q(c) for c in r] for r in d["learners"]]))
    parts.append(insert_block(
        "Session", ["SessionID", "ProgrammeID", "SchoolID", "TeacherID", "SessionDate", "StartTime", "EndTime"],
        [[q(r[0]), q(r[1]), q(r[2]), q(r[3]), rel(r[4]), q(r[5]), q(r[6])] for r in d["sessions"]]))
    parts.append(insert_block(
        "Attendance", ["AttendanceID", "SessionID", "LearnerID", "AttendanceStatus"],
        [[q(c) for c in r] for r in d["attendance"]]))
    parts.append(insert_block(
        "Progress", ["ProgressID", "LearnerID", "TeacherID", "AssessmentScore", "AssessmentDate", "Notes"],
        [[q(r[0]), q(r[1]), q(r[2]), q(r[3]), rel(r[4]), q(r[5])] for r in d["progress"]]))
    parts.append(insert_block(
        "Resource", ["ResourceID", "ResourceName", "ResourceType", "SchoolID", "Quantity", "AllocationDate"],
        [[q(r[0]), q(r[1]), q(r[2]), q(r[3]), q(r[4]), rel(-r[5])] for r in d["resources"]]))
    parts.append(insert_block(
        "[User]", ["UserID", "Username", "PasswordHash", "Role", "FullName", "Email", "IsActive", "TeacherID"],
        [[q(c) for c in r] for r in d["users"]]))
    parts.append(insert_block(
        "ParentLearner", ["ParentLearnerID", "UserID", "LearnerID", "Relationship"],
        [[q(c) for c in r] for r in d["parent_links"]]))
    parts.append("PRINT 'LPMS seed data inserted.';\n")
    return "\n".join(parts)


# ----------------------------------------------------------------------------
# Verification (SQLite replica) - checks data, not T-SQL syntax
# ----------------------------------------------------------------------------
def check(d: dict) -> None:
    db = sqlite3.connect(":memory:")
    db.execute("PRAGMA foreign_keys = ON")
    db.executescript(
        """
        CREATE TABLE School (SchoolID INTEGER PRIMARY KEY, SchoolName TEXT UNIQUE NOT NULL, Address TEXT NOT NULL,
            PrincipalName TEXT NOT NULL, ContactNumber TEXT NOT NULL, ContactEmail TEXT);
        CREATE TABLE Programme (ProgrammeID INTEGER PRIMARY KEY, ProgrammeName TEXT UNIQUE NOT NULL, Description TEXT,
            StartOffset INTEGER NOT NULL, EndOffset INTEGER NOT NULL, CHECK (EndOffset >= StartOffset));
        CREATE TABLE Teacher (TeacherID INTEGER PRIMARY KEY, FirstName TEXT NOT NULL, LastName TEXT NOT NULL,
            SchoolID INTEGER NOT NULL REFERENCES School(SchoolID), ContactEmail TEXT UNIQUE NOT NULL, ContactNumber TEXT);
        CREATE TABLE Learner (LearnerID INTEGER PRIMARY KEY, FirstName TEXT NOT NULL, LastName TEXT NOT NULL,
            Grade TEXT NOT NULL, SchoolID INTEGER NOT NULL REFERENCES School(SchoolID));
        CREATE TABLE Session (SessionID INTEGER PRIMARY KEY, ProgrammeID INTEGER NOT NULL REFERENCES Programme(ProgrammeID),
            SchoolID INTEGER NOT NULL REFERENCES School(SchoolID), TeacherID INTEGER NOT NULL REFERENCES Teacher(TeacherID),
            Offset INTEGER NOT NULL, StartTime TEXT NOT NULL, EndTime TEXT NOT NULL, CHECK (EndTime > StartTime));
        CREATE TABLE Attendance (AttendanceID INTEGER PRIMARY KEY, SessionID INTEGER NOT NULL REFERENCES Session(SessionID),
            LearnerID INTEGER NOT NULL REFERENCES Learner(LearnerID), AttendanceStatus TEXT NOT NULL
            CHECK (AttendanceStatus IN ('Present','Absent')), UNIQUE (SessionID, LearnerID));
        CREATE TABLE Progress (ProgressID INTEGER PRIMARY KEY, LearnerID INTEGER NOT NULL REFERENCES Learner(LearnerID),
            TeacherID INTEGER NOT NULL REFERENCES Teacher(TeacherID), AssessmentScore REAL NOT NULL
            CHECK (AssessmentScore BETWEEN 0 AND 100), Offset INTEGER NOT NULL, Notes TEXT);
        CREATE TABLE Resource (ResourceID INTEGER PRIMARY KEY, ResourceName TEXT NOT NULL, ResourceType TEXT NOT NULL
            CHECK (ResourceType IN ('Books','Stationery','Digital','Other')), SchoolID INTEGER NOT NULL REFERENCES School(SchoolID),
            Quantity INTEGER NOT NULL CHECK (Quantity > 0), DaysAgo INTEGER NOT NULL);
        CREATE TABLE UserTbl (UserID INTEGER PRIMARY KEY, Username TEXT UNIQUE NOT NULL, PasswordHash TEXT NOT NULL,
            Role TEXT NOT NULL CHECK (Role IN ('Administrator','Coordinator','Teacher','Parent')), FullName TEXT NOT NULL,
            Email TEXT UNIQUE NOT NULL, IsActive INTEGER NOT NULL, TeacherID INTEGER REFERENCES Teacher(TeacherID),
            CHECK (Role <> 'Teacher' OR TeacherID IS NOT NULL));
        CREATE UNIQUE INDEX UX_User_TeacherID ON UserTbl(TeacherID) WHERE TeacherID IS NOT NULL;
        CREATE TABLE ParentLearner (ParentLearnerID INTEGER PRIMARY KEY, UserID INTEGER NOT NULL REFERENCES UserTbl(UserID),
            LearnerID INTEGER NOT NULL REFERENCES Learner(LearnerID),
            Relationship TEXT NOT NULL CHECK (Relationship IN ('Mother','Father','Guardian')), UNIQUE (UserID, LearnerID));
        """
    )

    def ins(table: str, rows: list[tuple]) -> None:
        marks = ",".join("?" * len(rows[0]))
        db.executemany(f"INSERT INTO {table} VALUES ({marks})", rows)

    ins("School", d["schools"])
    ins("Programme", d["programmes"])
    ins("Teacher", d["teachers"])
    ins("Learner", d["learners"])
    ins("Session", [(s[0], s[1], s[2], s[3], s[4], s[5], s[6]) for s in d["sessions"]])
    ins("Attendance", d["attendance"])
    ins("Progress", d["progress"])
    ins("Resource", d["resources"])
    ins("UserTbl", d["users"])
    ins("ParentLearner", d["parent_links"])

    errors: list[str] = []

    # row counts (brief: at least ten records per table)
    counts = {}
    for table in ["School", "Teacher", "Learner", "Programme", "Session", "Attendance", "Progress", "Resource", "UserTbl", "ParentLearner"]:
        n = db.execute(f"SELECT COUNT(*) FROM {table}").fetchone()[0]
        counts[table] = n
        if n < 10:
            errors.append(f"{table} has only {n} rows (need >= 10)")

    # column lengths from the schema
    limits = {
        "School": {"SchoolName": 100, "Address": 150, "PrincipalName": 80, "ContactNumber": 15, "ContactEmail": 100},
        "Teacher": {"FirstName": 50, "LastName": 50, "ContactEmail": 100, "ContactNumber": 15},
        "Learner": {"FirstName": 50, "LastName": 50, "Grade": 8},
        "Programme": {"ProgrammeName": 100, "Description": 255},
        "Progress": {"Notes": 255},
        "Resource": {"ResourceName": 100},
        "UserTbl": {"Username": 50, "FullName": 100, "Email": 100, "PasswordHash": 100},
    }
    for table, cols in limits.items():
        for col, max_len in cols.items():
            bad = db.execute(f"SELECT COUNT(*) FROM {table} WHERE LENGTH({col}) > ?", (max_len,)).fetchone()[0]
            if bad:
                errors.append(f"{table}.{col} has {bad} value(s) longer than {max_len}")
    grades = {"Grade R"} | {f"Grade {i}" for i in range(1, 13)}
    for (g,) in db.execute("SELECT DISTINCT Grade FROM Learner"):
        if g not in grades:
            errors.append(f"invalid grade {g}")

    # business rules
    q1 = db.execute("""SELECT COUNT(*) FROM Session s JOIN Teacher t ON t.TeacherID = s.TeacherID
                       WHERE t.SchoolID <> s.SchoolID""").fetchone()[0]
    if q1:
        errors.append(f"{q1} sessions where the teacher does not belong to the session school")
    q2 = db.execute("""SELECT COUNT(*) FROM Attendance a JOIN Session s ON s.SessionID = a.SessionID
                       JOIN Learner l ON l.LearnerID = a.LearnerID WHERE l.SchoolID <> s.SchoolID""").fetchone()[0]
    if q2:
        errors.append(f"{q2} attendance rows for learners from another school")
    q3 = db.execute("""SELECT COUNT(*) FROM Attendance a JOIN Session s ON s.SessionID = a.SessionID
                       WHERE s.Offset >= 0""").fetchone()[0]
    if q3:
        errors.append(f"{q3} attendance rows on today/future sessions")
    q4 = db.execute("""SELECT COUNT(*) FROM Progress p JOIN Teacher t ON t.TeacherID = p.TeacherID
                       JOIN Learner l ON l.LearnerID = p.LearnerID WHERE t.SchoolID <> l.SchoolID""").fetchone()[0]
    if q4:
        errors.append(f"{q4} progress rows recorded by a teacher from another school")
    q5 = db.execute("SELECT COUNT(*) FROM Progress WHERE Offset >= 0").fetchone()[0]
    if q5:
        errors.append(f"{q5} progress rows dated today or later")
    if db.execute("PRAGMA foreign_key_check").fetchall():
        errors.append("foreign key violations")

    demo = db.execute("""SELECT COUNT(*) FROM Session WHERE TeacherID = 1""").fetchone()[0]
    pending = db.execute("""SELECT COUNT(*) FROM Session s WHERE TeacherID = 1 AND Offset < 0
                            AND NOT EXISTS (SELECT 1 FROM Attendance a WHERE a.SessionID = s.SessionID)""").fetchone()[0]
    upcoming = db.execute("SELECT COUNT(*) FROM Session WHERE TeacherID = 1 AND Offset >= 0").fetchone()[0]

    print("Row counts:", ", ".join(f"{k}={v}" for k, v in counts.items()))
    print(f"Demo teacher01: {demo} sessions ({upcoming} today/upcoming, {pending} past without attendance)")
    if errors:
        print("CHECK FAILED:")
        for e in errors:
            print("  -", e)
        sys.exit(1)
    print("CHECK PASSED: counts, lengths, foreign keys and business rules are consistent.")


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__, formatter_class=argparse.RawDescriptionHelpFormatter)
    parser.add_argument("--out", default=str(Path(__file__).resolve().parents[1] / "02-seed-data.sql"))
    parser.add_argument("--check", action="store_true", help="verify the data in an in-memory SQLite replica")
    args = parser.parse_args()

    data = build_data()
    if args.check:
        check(data)
    Path(args.out).write_text(emit_sql(data), encoding="utf-8", newline="\n")
    print(f"Wrote {args.out}")


if __name__ == "__main__":
    main()
