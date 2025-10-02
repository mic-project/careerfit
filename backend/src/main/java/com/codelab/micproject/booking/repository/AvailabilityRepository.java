package com.codelab.micproject.booking.repository;


import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.booking.domain.Availability;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.List;


public interface AvailabilityRepository extends JpaRepository<Availability, Long> {
    List<Availability> findByConsultant(User consultant);
}