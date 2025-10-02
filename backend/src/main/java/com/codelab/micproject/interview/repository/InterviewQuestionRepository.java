package com.codelab.micproject.interview.repository;

import com.codelab.micproject.interview.domain.InterviewQuestion;
import com.codelab.micproject.interview.domain.QuestionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, Long> {

    List<InterviewQuestion> findByQuestionType(QuestionType questionType);
}