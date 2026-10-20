namespace Lpms.Api.Services;

/// <summary>Source of "now" in South African time, replaceable in tests.</summary>
public interface IClock
{
    DateTime Now { get; }
}

public static class ClockExtensions
{
    public static DateOnly Today(this IClock clock) => DateOnly.FromDateTime(clock.Now);

    public static TimeOnly TimeOfDay(this IClock clock) => TimeOnly.FromDateTime(clock.Now);
}

/// <summary>South African Standard Time (UTC+2, no daylight saving).</summary>
public sealed class SastClock : IClock
{
    private static readonly TimeZoneInfo Zone = ResolveZone();

    public DateTime Now => TimeZoneInfo.ConvertTimeFromUtc(DateTime.UtcNow, Zone);

    private static TimeZoneInfo ResolveZone()
    {
        // Linux uses IANA ids, Windows uses its own ids.
        foreach (var id in new[] { "Africa/Johannesburg", "South Africa Standard Time" })
        {
            try
            {
                return TimeZoneInfo.FindSystemTimeZoneById(id);
            }
            catch (TimeZoneNotFoundException)
            {
            }
            catch (InvalidTimeZoneException)
            {
            }
        }

        return TimeZoneInfo.CreateCustomTimeZone("SAST", TimeSpan.FromHours(2), "SAST", "SAST");
    }
}
