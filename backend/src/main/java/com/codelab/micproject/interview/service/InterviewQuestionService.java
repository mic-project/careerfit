package com.codelab.micproject.interview.service;

import com.codelab.micproject.interview.domain.InterviewQuestion;
import com.codelab.micproject.interview.domain.QuestionType;
import com.codelab.micproject.interview.dto.InterviewQuestionResponse;
import com.codelab.micproject.interview.repository.InterviewQuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewQuestionService {

    private final InterviewQuestionRepository interviewQuestionRepository;

    public List<InterviewQuestionResponse> getQuestionsByType(QuestionType questionType) {
        List<InterviewQuestion> questions = interviewQuestionRepository.findByQuestionType(questionType);
        return questions.stream()
                .map(InterviewQuestionResponse::from)
                .collect(Collectors.toList());
    }

    public List<InterviewQuestionResponse> getAllQuestions() {
        List<InterviewQuestion> questions = interviewQuestionRepository.findAll();
        return questions.stream()
                .map(InterviewQuestionResponse::from)
                .collect(Collectors.toList());
    }
}