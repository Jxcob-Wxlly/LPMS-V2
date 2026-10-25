/* =====================================================================
   LPMS - messaging seed data (run after 02-seed-data.sql)
   Message: 12 rows, Announcement: 10 rows. Times are relative to "now" so the
   demo always looks current. User ids refer to 02-seed-data.sql:
   2 and 3 = coordinators, 1 = administrator, 4 = Nomsa Zulu (school 1),
   5 = Themba Nkosi (school 2), 6 = Lindiwe Dube (school 1), 16-25 = parents.
   ===================================================================== */

SET IDENTITY_INSERT dbo.Message ON;
INSERT INTO dbo.Message (MessageID, LearnerID, ParentUserID, SenderUserID, Body, SentAt) VALUES
    (1,  1,  16, 16, N'Good morning Ms Zulu. Sipho was unwell on Monday, so he missed the reading session. Is there anything he should catch up on?', DATEADD(HOUR, -52, SYSDATETIME())),
    (2,  1,  16, 4,  N'Thank you for letting me know. He only missed the phonics game. I will send home the word list so he can practise.', DATEADD(HOUR, -50, SYSDATETIME())),
    (3,  1,  16, 16, N'Perfect, thank you. We will practise it this evening.', DATEADD(HOUR, -49, SYSDATETIME())),
    (4,  11, 16, 4,  N'Faith read a whole page aloud today without help. Well done to her!', DATEADD(HOUR, -30, SYSDATETIME())),
    (5,  2,  17, 17, N'Hello. Could you tell me which reading level Ayanda is on at the moment?', DATEADD(HOUR, -28, SYSDATETIME())),
    (6,  2,  17, 6,  N'Hi Mr Radebe. Ayanda is reading at the Grade 5 level and her comprehension score is improving each month.', DATEADD(HOUR, -26, SYSDATETIME())),
    (7,  12, 17, 5,  N'Lwazi has been very quiet in class this week. Is everything alright at home?', DATEADD(HOUR, -20, SYSDATETIME())),
    (8,  12, 17, 17, N'He had a cold, but he is better now. Thanks for checking in.', DATEADD(HOUR, -18, SYSDATETIME())),
    (9,  3,  18, 18, N'Good afternoon. Will there be a session on the public holiday?', DATEADD(HOUR, -10, SYSDATETIME())),
    (10, 3,  18, 5,  N'No session on the holiday. The next one is on Friday at 15:30 as normal.', DATEADD(HOUR, -9, SYSDATETIME())),
    (11, 4,  19, 19, N'Amahle forgot her reading book at home. May she borrow one from the library hour?', DATEADD(HOUR, -5, SYSDATETIME())),
    (12, 4,  19, 4,  N'Of course. I have put a copy aside for her.', DATEADD(HOUR, -4, SYSDATETIME()));
SET IDENTITY_INSERT dbo.Message OFF;

SET IDENTITY_INSERT dbo.Announcement ON;
INSERT INTO dbo.Announcement (AnnouncementID, AuthorUserID, Title, Body, Audience, SchoolID, CreatedAt) VALUES
    (1,  2, N'Welcome to the new term',           N'Welcome back to everyone. Sessions start again on Monday. Please check your schedule in the app.', N'Everyone', NULL, DATEADD(DAY, -20, SYSDATETIME())),
    (2,  2, N'Take attendance on the day',        N'Please record attendance on the day of each session so the reports stay accurate.', N'Teachers', NULL, DATEADD(DAY, -18, SYSDATETIME())),
    (3,  3, N'Parents evening',                   N'Our parents evening is on Thursday at 17:00 in the school hall. All parents are welcome.', N'Parents', 1, DATEADD(DAY, -14, SYSDATETIME())),
    (4,  1, N'New assessment forms',              N'Use the new assessment form from this week. Scores are out of 100.', N'Teachers', NULL, DATEADD(DAY, -12, SYSDATETIME())),
    (5,  2, N'Reading challenge',                 N'The 20-book reading challenge starts this month. Ask your teacher for a reading log.', N'Everyone', NULL, DATEADD(DAY, -9, SYSDATETIME())),
    (6,  3, N'Library closed on Friday',          N'The library at Tembisa High is closed on Friday for repairs. Library Hour moves to Monday.', N'Everyone', 2, DATEADD(DAY, -7, SYSDATETIME())),
    (7,  2, N'Teacher training workshop',         N'A short training on the new app features takes place on Saturday at 09:00.', N'Teachers', NULL, DATEADD(DAY, -5, SYSDATETIME())),
    (8,  1, N'Public holiday',                    N'There are no sessions on the public holiday. Normal sessions resume the next day.', N'Everyone', NULL, DATEADD(DAY, -3, SYSDATETIME())),
    (9,  3, N'Progress reports',                  N'Progress reports for this term can now be downloaded from the child page in the app.', N'Parents', NULL, DATEADD(DAY, -2, SYSDATETIME())),
    (10, 2, N'Thank you',                         N'Thank you to all teachers and parents for a great term of reading.', N'Everyone', NULL, DATEADD(DAY, -1, SYSDATETIME()));
SET IDENTITY_INSERT dbo.Announcement OFF;

PRINT 'LPMS messaging seed data inserted.';
