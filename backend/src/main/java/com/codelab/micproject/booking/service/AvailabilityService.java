// src/main/java/com/codelab/micproject/booking/service/AvailabilityService.java
package com.codelab.micproject.booking.service;

import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.booking.domain.Availability;
import com.codelab.micproject.booking.domain.AvailableSlot;
import com.codelab.micproject.booking.dto.AvailabilitySlotDto;
import com.codelab.micproject.booking.dto.AvailabilityView;
import com.codelab.micproject.booking.dto.SlotDto;
import com.codelab.micproject.booking.dto.UpsertAvailabilityReq;
import com.codelab.micproject.booking.repository.AvailabilityRepository;
import com.codelab.micproject.booking.repository.AvailableSlotRepository;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AvailabilityService {

    private static final DateTimeFormatter HM = DateTimeFormatter.ofPattern("HH:mm");

    private final AvailabilityRepository availabilityRepository;
    private final AvailableSlotRepository availableSlotRepository; // ★ 추가
    private final UserRepository userRepository;

    /** 유효성 검사: 가용시간+간격에 맞는지 (불일치 시 예외) */
    @Transactional(readOnly = true)
    public void validate(User consultant, OffsetDateTime startAt, OffsetDateTime endAt) {
        if (!isValid(consultant, startAt, endAt)) {
            throw new IllegalArgumentException("NOT_IN_AVAILABILITY_OR_SLOT_MISMATCH");
        }
    }

    /** 유효성 검사: 가용시간+간격에 맞는지 (boolean) */
    @Transactional(readOnly = true)
    public boolean isValid(User consultant, OffsetDateTime startAt, OffsetDateTime endAt) {
        var avails = availabilityRepository.findByConsultant(consultant);
        if (avails == null || avails.isEmpty()) {
            throw new IllegalStateException("NO_AVAILABILITY_FOR_CONSULTANT");
        }
        return avails.stream().anyMatch(av -> fits(av, startAt, endAt));
    }

    /** 단일 Availability에 슬롯이 “정확히” 들어맞는지 */
    private boolean fits(Availability av, OffsetDateTime startAt, OffsetDateTime endAt) {
        String zoneStr = (av.getZoneId() != null && !av.getZoneId().isBlank())
                ? av.getZoneId() : "Asia/Seoul";
        ZoneId zone = ZoneId.of(zoneStr);

        ZonedDateTime startZ = startAt.atZoneSameInstant(zone);
        ZonedDateTime endZ   = endAt.atZoneSameInstant(zone);

        if (!startZ.toLocalDate().equals(endZ.toLocalDate())) return false;
        if (startZ.getDayOfWeek().getValue() != av.getWeekday()) return false;

        LocalTime availStart = parseHm(av.getStartTime());
        LocalTime availEnd   = parseHm(av.getEndTime());
        if (availStart == null || availEnd == null) return false;

        LocalTime s = startZ.toLocalTime();
        LocalTime e = endZ.toLocalTime();

        if (s.isBefore(availStart)) return false;
        if (e.isAfter(availEnd)) return false;

        int slot = av.getSlotMinutes();
        if (slot <= 0) return false;

        long minutes = Duration.between(s, e).toMinutes();
        if (minutes <= 0 || minutes % slot != 0) return false;

        long startOffset = Duration.between(availStart, s).toMinutes();
        long endOffset   = Duration.between(availStart, e).toMinutes();
        return (startOffset % slot == 0) && (endOffset % slot == 0);
    }

    @Transactional
    public AvailabilityView upsert(UserPrincipal me, UpsertAvailabilityReq req){
        var meUser = userRepository.findById(me.id()).orElseThrow();
        var a = Availability.builder()
                .consultant(meUser)
                .weekday(req.weekday())
                .startTime(req.startTime())
                .endTime(req.endTime())
                .slotMinutes(req.slotMinutes())
                .zoneId(req.zoneId())
                .build();
        availabilityRepository.save(a);
        return new AvailabilityView(
                a.getId(), a.getWeekday(), a.getStartTime(),
                a.getEndTime(), a.getSlotMinutes(), a.getZoneId());
    }

    private static LocalTime parseHm(String v) {
        if (v == null || v.isBlank()) return null;
        try { return LocalTime.parse(v, HM); } catch (Exception e) { return null; }
    }

    /** 공개: [from,to] 범위에 대한 가용 슬롯 생성(규칙 기반) */
    @Transactional(readOnly = true)
    public List<SlotDto> generateSlots(Long consultantId, LocalDate from, LocalDate to){
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("`to` must be the same or after `from`");
        }
        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        if (days > 60) {
            throw new IllegalArgumentException("Date range is too wide (max 60 days)");
        }

        var consultant = userRepository.findById(consultantId).orElseThrow();
        var availList = availabilityRepository.findByConsultant(consultant);

        List<SlotDto> out = new ArrayList<>();
        for (var day = from; !day.isAfter(to); day = day.plusDays(1)){
            int wd = day.getDayOfWeek().getValue(); // 1~7
            for (var av : availList){
                if (av.getWeekday() != wd) continue;

                var zone = (av.getZoneId()!=null && !av.getZoneId().isBlank())
                        ? ZoneId.of(av.getZoneId()) : ZoneId.systemDefault();

                var availStart = parseHm(av.getStartTime());
                var availEnd   = parseHm(av.getEndTime());
                if (availStart == null || availEnd == null) continue;

                var slot = Duration.ofMinutes(av.getSlotMinutes());
                for (var t = availStart; !t.plus(slot).isAfter(availEnd); t = t.plus(slot)){
                    var startZdt = ZonedDateTime.of(day, t, zone);
                    var endZdt = startZdt.plus(slot);
                    out.add(new SlotDto(startZdt.toOffsetDateTime(), endZdt.toOffsetDateTime()));
                }
            }
        }
        return out;
    }

    // ===== 여기부터 프론트 /api/availability/me/slots 3종 =====

    @Transactional(readOnly = true)
    public List<AvailabilitySlotDto> list(UserPrincipal me, LocalDate from, LocalDate to) {
        var consultant = userRepository.findById(me.id()).orElseThrow();
        var start = from.atStartOfDay();
        var end = to.atTime(LocalTime.MAX);

        return availableSlotRepository
                .findByConsultantAndStartAtBetweenOrderByStartAtAsc(consultant, start, end)
                .stream()
                .map(s -> new AvailabilitySlotDto(s.getId(), s.getStartAt().toString(), s.getEndAt().toString()))
                .toList();
    }

    @Transactional
    public void save(UserPrincipal me, List<AvailabilitySlotDto> slots) {
        var consultant = userRepository.findById(me.id()).orElseThrow();
        for (var dto : slots) {
            var start = LocalDateTime.parse(dto.startAt());
            var end   = LocalDateTime.parse(dto.endAt());
            if (!end.isAfter(start)) {
                throw new IllegalArgumentException("endAt must be after startAt");
            }
            var entity = AvailableSlot.builder()
                    .consultant(consultant)
                    .startAt(start)
                    .endAt(end)
                    .build();
            availableSlotRepository.save(entity);
        }
    }

    @Transactional
    public void delete(UserPrincipal me, Long id) {
        var consultant = userRepository.findById(me.id()).orElseThrow();
        var slot = availableSlotRepository.findById(id).orElseThrow();
        if (!slot.getConsultant().getId().equals(consultant.getId())) {
            throw new IllegalArgumentException("본인의 슬롯만 삭제할 수 있습니다.");
        }
        availableSlotRepository.delete(slot);
    }

    /** 공개: [from,to] 범위의 '저장된 개별 슬롯' 조회 */
    @Transactional(readOnly = true)
    public List<SlotDto> listSavedSlotsPublic(Long consultantId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) {
            throw new IllegalArgumentException("`to` must be the same or after `from`");
        }
        var consultant = userRepository.findById(consultantId).orElseThrow();

        var start = from.atStartOfDay();
        var end   = to.atTime(LocalTime.MAX);

        var list = availableSlotRepository
                .findByConsultantAndStartAtBetweenOrderByStartAtAsc(consultant, start, end);

        // ★ 컨설턴트가 등록한 슬롯은 KST 기준이므로 Asia/Seoul 타임존 사용
        ZoneId zone = ZoneId.of("Asia/Seoul");
        return list.stream().map(s -> new SlotDto(
                s.getStartAt().atZone(zone).toOffsetDateTime(),
                s.getEndAt().atZone(zone).toOffsetDateTime()
        )).toList();
    }
}
