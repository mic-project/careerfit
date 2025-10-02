// src/main/java/com/codelab/micproject/account/consultant/dto/ConsultantPublicDto.java
package com.codelab.micproject.account.consultant.dto;

/**
 * 예약 화면(Booking / BookingDate)에서 쓰는 공개용 요약/상세 DTO
 *  - name / profileImage : User 기반
 *  - careerYears         : Profile.careerStartYear 기준 계산
 *  - specialty           : Profile.skills 중 대표값(없으면 null)
 *  - introduction        : Profile.bio (소개글)
 */
public record ConsultantPublicDto(
        Long consultantId,
        String name,
        String profileImage,
        Integer careerYears,
        String specialty,
        String introduction   // ★ 추가
) { }
