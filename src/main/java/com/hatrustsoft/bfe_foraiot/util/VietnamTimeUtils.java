package com.hatrustsoft.bfe_foraiot.util;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utility class for Vietnam timezone (UTC+7)
 */
public class VietnamTimeUtils {
    
    public static final ZoneId VIETNAM_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    
    public static final DateTimeFormatter VIETNAM_DATETIME_FORMATTER = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    public static final DateTimeFormatter VIETNAM_DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    public static final DateTimeFormatter VIETNAM_TIME_FORMATTER = 
        DateTimeFormatter.ofPattern("HH:mm:ss");
    
    /**
     * Get current time in Vietnam timezone
     */
    public static LocalDateTime now() {
        return LocalDateTime.now(VIETNAM_ZONE);
    }
    
    /**
     * Get current ZonedDateTime in Vietnam timezone
     */
    public static ZonedDateTime nowZoned() {
        return ZonedDateTime.now(VIETNAM_ZONE);
    }
    
    /**
     * Convert UTC LocalDateTime to Vietnam timezone
     */
    public static LocalDateTime toVietnamTime(LocalDateTime utcTime) {
        if (utcTime == null) return null;
        return utcTime.atZone(ZoneId.of("UTC"))
                      .withZoneSameInstant(VIETNAM_ZONE)
                      .toLocalDateTime();
    }
    
    /**
     * Format LocalDateTime to Vietnam format string
     */
    public static String format(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.format(VIETNAM_DATETIME_FORMATTER);
    }
    
    /**
     * Format date only
     */
    public static String formatDate(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.format(VIETNAM_DATE_FORMATTER);
    }
    
    /**
     * Format time only
     */
    public static String formatTime(LocalDateTime dateTime) {
        if (dateTime == null) return "N/A";
        return dateTime.format(VIETNAM_TIME_FORMATTER);
    }
}
