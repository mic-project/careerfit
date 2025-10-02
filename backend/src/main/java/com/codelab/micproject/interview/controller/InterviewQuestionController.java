package com.codelab.micproject.interview.controller;

import com.codelab.micproject.common.response.ApiResponse;
import com.codelab.micproject.interview.domain.QuestionType;
import com.codelab.micproject.interview.dto.InterviewQuestionResponse;
import com.codelab.micproject.interview.service.InterviewQuestionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/interview/questions")
@RequiredArgsConstructor
public class InterviewQuestionController {

    private final InterviewQuestionService interviewQuestionService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<InterviewQuestionResponse>>> getAllQuestions() {
        List<InterviewQuestionResponse> questions = interviewQuestionService.getAllQuestions();
        return ResponseEntity.ok(ApiResponse.ok(questions));
    }

    @GetMapping("/type/{questionType}")
    public ResponseEntity<ApiResponse<List<InterviewQuestionResponse>>> getQuestionsByType(
            @PathVariable QuestionType questionType
    ) {
        List<InterviewQuestionResponse> questions = interviewQuestionService.getQuestionsByType(questionType);
        return ResponseEntity.ok(ApiResponse.ok(questions));
    }
}