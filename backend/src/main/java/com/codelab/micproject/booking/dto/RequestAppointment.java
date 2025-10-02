package com.codelab.micproject.booking.dto;

import java.time.OffsetDateTime;

public record RequestAppointment(Long consultantId, OffsetDateTime startAt, OffsetDateTime endAt){}
