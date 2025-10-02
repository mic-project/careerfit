// com.codelab.micproject.payment.repository.OrderRepository
package com.codelab.micproject.payment.repository;

import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.payment.domain.Order;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Long> {

    // 사용자별 주문 목록 (컨설턴트/예약까지 fetch)
    @Query("""
        select distinct o
        from Order o
        left join fetch o.consultant c
        left join fetch o.appointments oa
        left join fetch oa.appointment a
        where o.user = :user
        order by o.id desc
    """)
    List<Order> findByUser(@Param("user") User user);

    // 사용자별 + PAID 상태만 조회
    @Query("""
        select distinct o
        from Order o
        left join fetch o.consultant c
        left join fetch o.appointments oa
        left join fetch oa.appointment a
        where o.user = :user and o.status = com.codelab.micproject.payment.domain.OrderStatus.PAID
        order by o.id desc
    """)
    List<Order> findPaidOrdersByUser(@Param("user") User user);

    // 컨설턴트별 주문 목록 (사용자/예약까지 fetch)
    @Query("""
        select distinct o
        from Order o
        left join fetch o.user u
        left join fetch o.appointments oa
        left join fetch oa.appointment a
        where o.consultant = :consultant
        order by o.id desc
    """)
    List<Order> findByConsultant(@Param("consultant") User consultant);
}
