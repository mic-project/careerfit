package com.codelab.micproject.booking.dto;

public record AvailabilityView(Long id, int weekday, String startTime, String endTime, int slotMinutes, String zoneId){}