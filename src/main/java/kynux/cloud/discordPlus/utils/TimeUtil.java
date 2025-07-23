package kynux.cloud.discordPlus.utils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;

public final class TimeUtil {

    private static ZoneId zoneId = ZoneId.systemDefault();

    public static void setTimeZone(String timeZone) {
        try {
            zoneId = ZoneId.of(timeZone);
        } catch (Exception e) {
            System.err.println("Invalid timezone ID: " + timeZone + ". Using system default.");
            zoneId = ZoneId.systemDefault();
        }
    }

    public static LocalDateTime now() {
        return LocalDateTime.now(zoneId);
    }

    public static long minutesBetween(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end).toMinutes();
    }

    public static long hoursBetween(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start, end).toHours();
    }

    public static long daysBetween(LocalDateTime start, LocalDateTime end) {
        return Duration.between(start.toLocalDate().atStartOfDay(), end.toLocalDate().atStartOfDay()).toDays();
    }

    public static boolean isSameDay(LocalDateTime date1, LocalDateTime date2) {
        if (date1 == null || date2 == null) {
            return false;
        }
        return date1.toLocalDate().equals(date2.toLocalDate());
    }

    public static String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "N/A";
        }
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm:ss");
        return dateTime.format(formatter);
    }

    public static String formatDuration(long millis) {
        Duration duration = Duration.ofMillis(millis);
        long days = duration.toDays();
        duration = duration.minusDays(days);
        long hours = duration.toHours();
        duration = duration.minusHours(hours);
        long minutes = duration.toMinutes();
        
        StringBuilder sb = new StringBuilder();
        if (days > 0) sb.append(days).append(" gün ");
        if (hours > 0) sb.append(hours).append(" saat ");
        if (minutes > 0 || (days == 0 && hours == 0)) sb.append(minutes).append(" dakika");
        
        return sb.toString().trim();
    }

    public static String formatTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null) {
            return "asla";
        }
        Duration duration = Duration.between(dateTime, now());
        long seconds = duration.getSeconds();

        if (seconds < 60) return "az önce";
        if (seconds < 3600) return (seconds / 60) + " dakika önce";
        if (seconds < 86400) return (seconds / 3600) + " saat önce";
        
        long days = seconds / 86400;
        if (days == 1) return "dün";
        return days + " gün önce";
    }
}
