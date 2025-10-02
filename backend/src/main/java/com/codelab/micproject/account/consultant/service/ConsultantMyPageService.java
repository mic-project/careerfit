package com.codelab.micproject.account.consultant.service;

import com.codelab.micproject.account.consultant.dto.ConsultantMyPageDto;
import com.codelab.micproject.account.consultant.dto.UpdateConsultantForm;
import com.codelab.micproject.account.profile.domain.Profile;
import com.codelab.micproject.account.profile.dto.ProfileDto;
import com.codelab.micproject.account.profile.repository.ProfileRepository;
import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.booking.domain.Appointment;
import com.codelab.micproject.booking.domain.AppointmentStatus;
import com.codelab.micproject.booking.dto.AppointmentView;
import com.codelab.micproject.booking.repository.AppointmentRepository;
import com.codelab.micproject.security.oauth2.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Year;
import java.util.List;
import java.util.stream.Collectors;

// ★ 추가: 레벨/메타
import com.codelab.micproject.account.consultant.domain.ConsultantLevel;
import com.codelab.micproject.account.consultant.domain.ConsultantMeta;
import com.codelab.micproject.account.consultant.repository.ConsultantMetaRepository;
// ★ 추가: 등급 규칙 유틸 (사용자가 생성한다고 한 클래스)
import com.codelab.micproject.account.consultant.support.LevelRules;

// ★ 프로필 이미지 저장소 (기존 그대로)
import com.codelab.micproject.account.consultant.service.ProfileImageStorage;

@Service
@RequiredArgsConstructor
public class ConsultantMyPageService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final AppointmentRepository appointmentRepository;
    private final ProfileImageStorage profileImageStorage;

    // ★ 추가: 컨설턴트 메타
    private final ConsultantMetaRepository consultantMetaRepository;

    public ConsultantMyPageDto getMyPage(UserPrincipal me){
        User u = userRepository.findById(me.id()).orElseThrow();
        Profile p = profileRepository.findByUser(u).orElse(null);

        Integer startYear = (p != null ? p.getCareerStartYear() : null);
        Integer careerYears = (startYear == null ? null : Year.now().getValue() - startYear);

        ProfileDto profileDto = null;
        if (p != null) {
            profileDto = new ProfileDto(
                    p.getId(), p.getBio(), p.getSkills(),
                    p.getCareer(), p.getHourlyRate(), p.isPublicCalendar()
            );
        }

        String company = null;
        try {
            var m = User.class.getMethod("getCompany");
            company = (String) m.invoke(u);
        } catch (Exception ignore) { /* 필드 없으면 무시 */ }

        String specialty = null;
        if (p != null && p.getSkills() != null && !p.getSkills().isBlank()) {
            String[] parts = p.getSkills().trim().split("[,\\n]");
            if (parts.length > 0) specialty = parts[0].trim();
        }

        return new ConsultantMyPageDto(
                u.getId(),
                u.getName(),
                u.getEmail(),
                u.getPhone(),
                company,
                specialty,
                (p != null ? p.getBio() : null),
                startYear,
                careerYears,
                u.getProfileImage(),
                profileDto
        );
    }

    @Transactional
    public ConsultantMyPageDto updateMyPage(UserPrincipal me, UpdateConsultantForm form) {
        User u = userRepository.findById(me.id()).orElseThrow();

        if (form.getName() != null)  u.setName(form.getName());
        if (form.getPhone() != null) u.setPhone(form.getPhone());
        if (form.getEmail() != null) u.setEmail(form.getEmail()); // 정책상 불가면 제거

        if (form.getProfileImage() != null && !form.getProfileImage().isEmpty()){
            String url = profileImageStorage.store(u.getId(), form.getProfileImage());
            u.setProfileImage(url);
        }

        try {
            if (form.getCompany() != null) {
                var m = User.class.getMethod("setCompany", String.class);
                m.invoke(u, form.getCompany());
            }
        } catch (Exception ignore) { /* 필드 없으면 다른 저장소 사용 */ }

        userRepository.save(u);

        Profile p = profileRepository.findByUser(u)
                .orElseGet(() -> Profile.builder().user(u).build());

        if (form.getIntroduction() != null) p.setBio(form.getIntroduction());
        if (form.getCareerStartYear() != null) p.setCareerStartYear(form.getCareerStartYear());

        if (form.getSpecialty() != null) {
            String skills = p.getSkills();
            if (skills == null || skills.isBlank()) {
                p.setSkills(form.getSpecialty());
            } else {
                p.setSkills(form.getSpecialty() + "," + skills);
            }
        }

        profileRepository.save(p);

        // ───────────────────────────────────────────────────────────────
        // ★★ 핵심: 경력 시작연도 → 등급 재계산하여 ConsultantMeta.level 갱신
        // ───────────────────────────────────────────────────────────────
        if (form.getCareerStartYear() != null) {
            ConsultantLevel newLevel = LevelRules.fromCareerStartYear(form.getCareerStartYear());

            ConsultantMeta meta = consultantMetaRepository.findByConsultant(u)
                    .orElseGet(() -> ConsultantMeta.builder().consultant(u).build());

            // 등급이 바뀌면 갱신
            if (meta.getLevel() == null || meta.getLevel() != newLevel) {
                meta.setLevel(newLevel);
            }

            meta.setBasePrice(null);

            consultantMetaRepository.save(meta);
        }
        // ───────────────────────────────────────────────────────────────

        return getMyPage(me);
    }

    public List<AppointmentView> getSchedules(UserPrincipal me){
        User c = userRepository.findById(me.id()).orElseThrow();
        List<Appointment> appts = appointmentRepository.findByConsultant(c);
        return appts.stream().map(AppointmentView::from).collect(Collectors.toList());
    }

    public List<AppointmentView> getCompletedInterviews(UserPrincipal me){
        User c = userRepository.findById(me.id()).orElseThrow();
        List<Appointment> appts = appointmentRepository.findByConsultantAndStatus(c, AppointmentStatus.DONE);
        return appts.stream().map(AppointmentView::from).collect(Collectors.toList());
    }
}

