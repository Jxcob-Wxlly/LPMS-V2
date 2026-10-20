using Lpms.Api.Models;
using Microsoft.EntityFrameworkCore;

namespace Lpms.Api.Data;

/// <summary>
/// Entity Framework Core mapping onto the tables created by database/01-create-schema.sql.
/// The SQL scripts are the source of truth for the schema; this class only describes how the
/// C# classes map onto it (table and column names follow the System Design Document).
/// </summary>
public class LpmsDbContext : DbContext
{
    public LpmsDbContext(DbContextOptions<LpmsDbContext> options) : base(options)
    {
    }

    public DbSet<School> Schools => Set<School>();
    public DbSet<Teacher> Teachers => Set<Teacher>();
    public DbSet<Learner> Learners => Set<Learner>();
    public DbSet<Programme> Programmes => Set<Programme>();
    public DbSet<Session> Sessions => Set<Session>();
    public DbSet<AttendanceRecord> AttendanceRecords => Set<AttendanceRecord>();
    public DbSet<ProgressRecord> ProgressRecords => Set<ProgressRecord>();
    public DbSet<Resource> Resources => Set<Resource>();
    public DbSet<UserAccount> Users => Set<UserAccount>();
    public DbSet<ParentLearner> ParentLearners => Set<ParentLearner>();

    protected override void OnModelCreating(ModelBuilder modelBuilder)
    {
        modelBuilder.Entity<School>(e =>
        {
            e.ToTable("School");
            e.HasKey(x => x.SchoolId);
            e.Property(x => x.SchoolId).HasColumnName("SchoolID");
            e.Property(x => x.SchoolName).HasMaxLength(100);
            e.Property(x => x.Address).HasMaxLength(150);
            e.Property(x => x.PrincipalName).HasMaxLength(80);
            e.Property(x => x.ContactNumber).HasMaxLength(15);
            e.Property(x => x.ContactEmail).HasMaxLength(100);
            e.HasIndex(x => x.SchoolName).IsUnique();
        });

        modelBuilder.Entity<Programme>(e =>
        {
            e.ToTable("Programme");
            e.HasKey(x => x.ProgrammeId);
            e.Property(x => x.ProgrammeId).HasColumnName("ProgrammeID");
            e.Property(x => x.ProgrammeName).HasMaxLength(100);
            e.Property(x => x.Description).HasMaxLength(255);
        });

        modelBuilder.Entity<Teacher>(e =>
        {
            e.ToTable("Teacher");
            e.HasKey(x => x.TeacherId);
            e.Property(x => x.TeacherId).HasColumnName("TeacherID");
            e.Property(x => x.SchoolId).HasColumnName("SchoolID");
            e.Property(x => x.FirstName).HasMaxLength(50);
            e.Property(x => x.LastName).HasMaxLength(50);
            e.Property(x => x.ContactEmail).HasMaxLength(100);
            e.Property(x => x.ContactNumber).HasMaxLength(15);
            e.HasOne(x => x.School).WithMany(s => s.Teachers)
                .HasForeignKey(x => x.SchoolId).OnDelete(DeleteBehavior.Restrict);
        });

        modelBuilder.Entity<Learner>(e =>
        {
            e.ToTable("Learner");
            e.HasKey(x => x.LearnerId);
            e.Property(x => x.LearnerId).HasColumnName("LearnerID");
            e.Property(x => x.SchoolId).HasColumnName("SchoolID");
            e.Property(x => x.FirstName).HasMaxLength(50);
            e.Property(x => x.LastName).HasMaxLength(50);
            e.Property(x => x.Grade).HasMaxLength(8);
            e.HasOne(x => x.School).WithMany(s => s.Learners)
                .HasForeignKey(x => x.SchoolId).OnDelete(DeleteBehavior.Restrict);
        });

        modelBuilder.Entity<Session>(e =>
        {
            e.ToTable("Session");
            e.HasKey(x => x.SessionId);
            e.Property(x => x.SessionId).HasColumnName("SessionID");
            e.Property(x => x.ProgrammeId).HasColumnName("ProgrammeID");
            e.Property(x => x.SchoolId).HasColumnName("SchoolID");
            e.Property(x => x.TeacherId).HasColumnName("TeacherID");
            e.HasOne(x => x.Programme).WithMany(p => p.Sessions)
                .HasForeignKey(x => x.ProgrammeId).OnDelete(DeleteBehavior.Restrict);
            e.HasOne(x => x.School).WithMany(s => s.Sessions)
                .HasForeignKey(x => x.SchoolId).OnDelete(DeleteBehavior.Restrict);
            e.HasOne(x => x.Teacher).WithMany(t => t.Sessions)
                .HasForeignKey(x => x.TeacherId).OnDelete(DeleteBehavior.Restrict);
        });

        modelBuilder.Entity<AttendanceRecord>(e =>
        {
            e.ToTable("Attendance");
            e.HasKey(x => x.AttendanceId);
            e.Property(x => x.AttendanceId).HasColumnName("AttendanceID");
            e.Property(x => x.SessionId).HasColumnName("SessionID");
            e.Property(x => x.LearnerId).HasColumnName("LearnerID");
            e.Property(x => x.AttendanceStatus).HasMaxLength(10);
            e.HasOne(x => x.Session).WithMany(s => s.AttendanceRecords)
                .HasForeignKey(x => x.SessionId).OnDelete(DeleteBehavior.Restrict);
            e.HasOne(x => x.Learner).WithMany(l => l.AttendanceRecords)
                .HasForeignKey(x => x.LearnerId).OnDelete(DeleteBehavior.Restrict);
            e.HasIndex(x => new { x.SessionId, x.LearnerId }).IsUnique();
        });

        modelBuilder.Entity<ProgressRecord>(e =>
        {
            e.ToTable("Progress");
            e.HasKey(x => x.ProgressId);
            e.Property(x => x.ProgressId).HasColumnName("ProgressID");
            e.Property(x => x.LearnerId).HasColumnName("LearnerID");
            e.Property(x => x.TeacherId).HasColumnName("TeacherID");
            e.Property(x => x.AssessmentScore).HasPrecision(5, 2);
            e.Property(x => x.Notes).HasMaxLength(255);
            e.HasOne(x => x.Learner).WithMany(l => l.ProgressRecords)
                .HasForeignKey(x => x.LearnerId).OnDelete(DeleteBehavior.Restrict);
            e.HasOne(x => x.Teacher).WithMany(t => t.ProgressRecords)
                .HasForeignKey(x => x.TeacherId).OnDelete(DeleteBehavior.Restrict);
        });

        modelBuilder.Entity<Resource>(e =>
        {
            e.ToTable("Resource");
            e.HasKey(x => x.ResourceId);
            e.Property(x => x.ResourceId).HasColumnName("ResourceID");
            e.Property(x => x.SchoolId).HasColumnName("SchoolID");
            e.Property(x => x.ResourceName).HasMaxLength(100);
            e.Property(x => x.ResourceType).HasMaxLength(20);
            e.HasOne(x => x.School).WithMany(s => s.Resources)
                .HasForeignKey(x => x.SchoolId).OnDelete(DeleteBehavior.Restrict);
        });

        modelBuilder.Entity<UserAccount>(e =>
        {
            e.ToTable("User");
            e.HasKey(x => x.UserId);
            e.Property(x => x.UserId).HasColumnName("UserID");
            e.Property(x => x.TeacherId).HasColumnName("TeacherID");
            e.Property(x => x.Username).HasMaxLength(50);
            e.Property(x => x.PasswordHash).HasMaxLength(100);
            e.Property(x => x.Role).HasMaxLength(20);
            e.Property(x => x.FullName).HasMaxLength(100);
            e.Property(x => x.Email).HasMaxLength(100);
            e.HasIndex(x => x.Username).IsUnique();
            e.HasIndex(x => x.Email).IsUnique();
            e.HasIndex(x => x.TeacherId).IsUnique();
            e.HasOne(x => x.Teacher).WithMany()
                .HasForeignKey(x => x.TeacherId).OnDelete(DeleteBehavior.Restrict);
        });

        modelBuilder.Entity<ParentLearner>(e =>
        {
            e.ToTable("ParentLearner");
            e.HasKey(x => x.ParentLearnerId);
            e.Property(x => x.ParentLearnerId).HasColumnName("ParentLearnerID");
            e.Property(x => x.UserId).HasColumnName("UserID");
            e.Property(x => x.LearnerId).HasColumnName("LearnerID");
            e.Property(x => x.Relationship).HasMaxLength(20);
            e.HasIndex(x => new { x.UserId, x.LearnerId }).IsUnique();
            e.HasOne(x => x.User).WithMany().HasForeignKey(x => x.UserId).OnDelete(DeleteBehavior.Restrict);
            e.HasOne(x => x.Learner).WithMany().HasForeignKey(x => x.LearnerId).OnDelete(DeleteBehavior.Restrict);
        });
    }
}
