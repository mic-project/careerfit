package com.codelab.micproject.resume.exception;

/** 지원하지 않는 파일 형식 에러 */
public class UnsupportedFileTypeException extends RuntimeException {
    public UnsupportedFileTypeException(String msg) { super(msg); }
}
