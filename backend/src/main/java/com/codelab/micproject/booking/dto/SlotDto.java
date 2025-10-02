package com.codelab.micproject.booking.dto;

import java.time.OffsetDateTime;

public record SlotDto(OffsetDateTime startAt, OffsetDateTime endAt){}