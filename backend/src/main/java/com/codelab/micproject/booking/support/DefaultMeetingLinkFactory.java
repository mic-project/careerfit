// booking.support/DefaultMeetingLinkFactory.java
package com.codelab.micproject.booking.support;

import com.codelab.micproject.booking.domain.Appointment;
import org.springframework.stereotype.Component;

@Component
public class DefaultMeetingLinkFactory implements MeetingLinkFactory {
    @Override public String buildJoinUrl(Appointment a) {
        return "/Meeting?apt=" + a.getId(); // 토큰은 별도 API에서 발급
    }
}