package com.codelab.micproject.booking.domain;


import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.common.jpa.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;


import java.time.OffsetDateTime;


@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@Table(indexes = {
        @Index(columnList = "consultant_id, startAt"),
        @Index(columnList = "consultant_id, endAt")
})
public class Appointment extends BaseTimeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="consultant_id")
    private User consultant;


    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name="user_id")
    private User user;


    private OffsetDateTime startAt;
    private OffsetDateTime endAt;


    @Enumerated(EnumType.STRING)
    private AppointmentStatus status;


    private String meetingUrl; // 확정 시 채움

    @Version
    private Long version;
}