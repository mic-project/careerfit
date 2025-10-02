package com.codelab.micproject.interview;

import com.codelab.micproject.account.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewPracticeVideoRepository extends JpaRepository<PracticeVideo, Long> {
    List<PracticeVideo> findByUserOrderByCreatedAtDesc(User user);
}
