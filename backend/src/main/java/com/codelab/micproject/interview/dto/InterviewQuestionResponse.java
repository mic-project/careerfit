package com.codelab.micproject.interview.dto;

import com.codelab.micproject.interview.domain.InterviewQuestion;
import com.codelab.micproject.interview.domain.QuestionType;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InterviewQuestionResponse {

    private Long id;
    private String question;
    private QuestionType questionType;

    public static InterviewQuestionResponse from(InterviewQuestion interviewQuestion) {
        return new InterviewQuestionResponse(
                interviewQuestion.getId(),
                interviewQuestion.getQuestion(),
                interviewQuestion.getQuestionType()
        );
    }
}