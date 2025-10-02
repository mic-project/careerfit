// src/main/java/com/codelab/micproject/booking/dto/AvailabilitySlotDto.java
package com.codelab.micproject.booking.dto;

public record AvailabilitySlotDto(
        Long id,
        String startAt, // ISO-8601, e.g. 2025-09-29T10:00:00
        String endAt
) {}
