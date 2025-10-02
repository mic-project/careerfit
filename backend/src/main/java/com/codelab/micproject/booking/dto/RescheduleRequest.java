package com.codelab.micproject.booking.dto;

import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;

public record RescheduleRequest(
        @NotNull OffsetDateTime startAt,
        @NotNull OffsetDateTime endAt
) {}
