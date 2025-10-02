package com.codelab.micproject.account.consultant.repository;


import com.codelab.micproject.account.consultant.domain.ConsultantMeta;
import com.codelab.micproject.account.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;


import java.util.Optional;


public interface ConsultantMetaRepository extends JpaRepository<ConsultantMeta, Long> {
    Optional<ConsultantMeta> findByConsultant(User consultant);
}