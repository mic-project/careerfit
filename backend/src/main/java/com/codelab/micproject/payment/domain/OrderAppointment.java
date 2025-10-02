package com.codelab.micproject.payment.domain;


import com.codelab.micproject.booking.domain.Appointment;
import jakarta.persistence.*;
import lombok.*;


@Entity @Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderAppointment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "order_id")
    private Order order;


    @OneToOne(fetch = FetchType.LAZY) @JoinColumn(name = "appointment_id")
    private Appointment appointment; // 예약 1건과 대응

    @Version
    private Long version;
}