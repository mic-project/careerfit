package com.codelab.micproject.booking.dto;


import jakarta.validation.constraints.*;
import java.time.*;
import java.util.List;


public record UpsertAvailabilityReq(
        @Min(1) @Max(7) int weekday,
        @Pattern(regexp = "^\\d{2}:\\d{2}$") String startTime,
        @Pattern(regexp = "^\\d{2}:\\d{2}$") String endTime,
        @Min(10) int slotMinutes,
        String zoneId
){}