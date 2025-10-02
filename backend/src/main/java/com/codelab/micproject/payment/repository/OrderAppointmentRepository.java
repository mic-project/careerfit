package com.codelab.micproject.payment.repository;


import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.booking.domain.AppointmentStatus;
import com.codelab.micproject.payment.domain.OrderAppointment;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.OffsetDateTime;

import java.util.List;


public interface OrderAppointmentRepository extends JpaRepository<OrderAppointment, Long> {

    // 유저 기준으로 조회
    List<OrderAppointment> findByOrder_User(User user);                 // 주문의 사용자
    List<OrderAppointment> findByAppointment_User(User user);           // 예약의 사용자 (둘 중 하나만 써도 됨)

    // 컨설턴트 기준으로 조회
    List<OrderAppointment> findByOrder_Consultant(User consultant);
    List<OrderAppointment> findByAppointment_Consultant(User consultant);

    // A 정책 1단계: DONE 존재 여부
    boolean existsByAppointment_ConsultantAndAppointment_UserAndAppointment_Status(
            User consultant, User user, AppointmentStatus status
    );

    // A 정책 2단계: APPROVED & 이미 종료됨
    boolean existsByAppointment_ConsultantAndAppointment_UserAndAppointment_StatusAndAppointment_EndAtBefore(
            User consultant, User user, AppointmentStatus status, OffsetDateTime endBefore
    );

}