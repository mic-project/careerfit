package com.codelab.micproject.review.repository;


import com.codelab.micproject.review.domain.Review;
import com.codelab.micproject.account.user.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;


import java.util.List;


public interface ReviewRepository extends JpaRepository<Review, Long> {
    // 최신순 조회
    List<Review> findByConsultantOrderByCreatedAtDesc(User consultant);

    // 중복 리뷰 방지(원하면 사용)
    boolean existsByReviewerAndConsultant(User reviewer, User consultant);

    // 평균/개수 뽑을 때 (원하면)
    @Query("select avg(r.rating) from Review r where r.consultant = :consultant")
    Double avgRatingByConsultant(@Param("consultant") User consultant);

    @Query("select count(r) from Review r where r.consultant = :consultant")
    long countByConsultant(@Param("consultant") User consultant);
}