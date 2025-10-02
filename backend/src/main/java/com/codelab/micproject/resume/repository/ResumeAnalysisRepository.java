package com.codelab.micproject.resume.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.codelab.micproject.resume.entity.ResumeAnalysis;

@Repository
public interface ResumeAnalysisRepository extends JpaRepository<ResumeAnalysis, Long>{


    // Find a record by unique requestId.
    Optional<ResumeAnalysis> findByRequestId(String requestId);

    // Find recent analysis for a user, ordered by creation time (descending).
    List<ResumeAnalysis> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);
}
