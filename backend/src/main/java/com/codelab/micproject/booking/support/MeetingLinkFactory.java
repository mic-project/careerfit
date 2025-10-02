// booking.support/MeetingLinkFactory.java
package com.codelab.micproject.booking.support;

import com.codelab.micproject.booking.domain.Appointment;

public interface MeetingLinkFactory {
    String buildJoinUrl(Appointment a); // ex) "/Meeting?apt=123"
}