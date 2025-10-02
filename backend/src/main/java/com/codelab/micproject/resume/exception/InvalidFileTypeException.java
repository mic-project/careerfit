package com.codelab.micproject.resume.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** 지원하지 않는 파일 형식 에러 */
@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidFileTypeException extends RuntimeException {
    public InvalidFileTypeException(String msg) { super(msg); }
}
