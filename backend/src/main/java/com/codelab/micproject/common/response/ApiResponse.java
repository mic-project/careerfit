package com.codelab.micproject.common.response;


import lombok.*;


@Getter @AllArgsConstructor @NoArgsConstructor @Builder
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;


    public static <T> ApiResponse<T> ok(T data){
        return ApiResponse.<T>builder().success(true).message("OK").data(data).build();
    }
    public static ApiResponse<Void> ok(){
        return ApiResponse.<Void>builder().success(true).message("OK").build();
    }

    // ✅ 제네릭 타입으로 수정
    public static <T> ApiResponse<T> error(String msg){
        return ApiResponse.<T>builder().success(false).message(msg).build();
    }
}