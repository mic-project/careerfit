package com.codelab.micproject.booking.dto;

import java.util.List;

public record RequestAppointmentBatch(Long consultantId, SessionBundle bundle, List<SlotDto> slots){}
