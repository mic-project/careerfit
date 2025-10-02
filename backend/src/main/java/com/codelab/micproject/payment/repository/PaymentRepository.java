package com.codelab.micproject.payment.repository;

import com.codelab.micproject.payment.domain.Order;
import com.codelab.micproject.payment.domain.Payment;
import com.codelab.micproject.payment.domain.PaymentStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.*;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Payment p where p.merchantUid = :merchantUid")
    Optional<Payment> findWithLockByMerchantUid(@Param("merchantUid") String merchantUid);

    Page<Payment> findByUserIdOrderByIdDesc(Long userId, Pageable pageable);

    Optional<Payment> findByMerchantUid(String merchantUid);

    Optional<Payment> findByOrder(Order order);

    Page<Payment> findByUserIdAndStatusOrderByIdDesc(Long userId, PaymentStatus status, Pageable pageable);
}
