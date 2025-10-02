package com.codelab.micproject.config;

import com.codelab.micproject.account.consultant.domain.ConsultantLevel;
import com.codelab.micproject.account.consultant.domain.ConsultantMeta;
import com.codelab.micproject.account.consultant.repository.ConsultantMetaRepository;
import com.codelab.micproject.account.profile.domain.Profile;
import com.codelab.micproject.account.profile.repository.ProfileRepository;
import com.codelab.micproject.account.user.domain.AuthProvider;
import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.domain.UserRole;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.booking.domain.AvailableSlot;
import com.codelab.micproject.booking.repository.AvailableSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class ConsultantDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ConsultantMetaRepository consultantMetaRepository;
    private final ProfileRepository profileRepository;
    private final AvailableSlotRepository availableSlotRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {
        createConsultant("Kim Minsu", "minsu.kim@careerfit.net", ConsultantLevel.SENIOR,
                "10 years IT interview expert. Samsung, Naver experience.",
                "Java, Spring, AWS, System Design", 2014);

        createConsultant("Park Jieun", "jieun.park@careerfit.net", ConsultantLevel.EXECUTIVE,
                "15 years HR expert. Kakao, Coupang recruitment experience.",
                "Recruitment Strategy, Resume Consulting, Interview Coaching", 2009);

        createConsultant("Lee Junho", "junho.lee@careerfit.net", ConsultantLevel.JUNIOR,
                "5 years startup developer. Junior developer job specialist.",
                "Python, Django, React, Portfolio", 2019);

        createConsultant("Choi Seoyeon", "seoyeon.choi@careerfit.net", ConsultantLevel.SENIOR,
                "12 years financial industry interview expert.",
                "Financial Interview, Resume, Cover Letter", 2012);

        createConsultant("Jung Woojin", "woojin.jung@careerfit.net", ConsultantLevel.EXECUTIVE,
                "20 years executive experience. Executive interview specialist.",
                "Executive Interview, Career Change, Negotiation", 2004);

        createConsultant("Han Sohee", "sohee.han@careerfit.net", ConsultantLevel.JUNIOR,
                "3 years marketer. Marketing and planning job specialist.",
                "Marketing, Branding, Content Planning", 2021);

        log.info("Consultant data initialization completed");
    }

    private void createConsultant(String name, String email, ConsultantLevel level,
                                  String bio, String skills, Integer careerStartYear) {
        if (userRepository.findByEmail(email).isPresent()) {
            return;
        }

        User consultant = User.builder()
                .email(email)
                .name(name)
                .password(passwordEncoder.encode("consultant1234!"))
                .role(UserRole.CONSULTANT)
                .provider(AuthProvider.LOCAL)
                .enabled(true)
                .build();
        userRepository.save(consultant);

        ConsultantMeta meta = ConsultantMeta.builder()
                .consultant(consultant)
                .level(level)
                .basePrice(null)
                .build();
        consultantMetaRepository.save(meta);

        Profile profile = Profile.builder()
                .user(consultant)
                .bio(bio)
                .skills(skills)
                .career(getCareerText(careerStartYear))
                .careerStartYear(careerStartYear)
                .hourlyRate(getHourlyRate(level))
                .publicCalendar(true)
                .build();
        profileRepository.save(profile);

        // Add available time slots for next 7 days
        createAvailableSlots(consultant);

        log.info("Created consultant: {} ({})", name, level);
    }

    private void createAvailableSlots(User consultant) {
        LocalDate today = LocalDate.now();

        // Create slots for next 7 days
        for (int day = 0; day < 7; day++) {
            LocalDate date = today.plusDays(day);

            // Morning slots: 9:00-12:00 (1-hour slots)
            for (int hour = 9; hour < 12; hour++) {
                LocalDateTime start = LocalDateTime.of(date, LocalTime.of(hour, 0));
                LocalDateTime end = start.plusHours(1);

                AvailableSlot slot = AvailableSlot.builder()
                        .consultant(consultant)
                        .startAt(start)
                        .endAt(end)
                        .build();
                availableSlotRepository.save(slot);
            }

            // Afternoon slots: 14:00-18:00 (1-hour slots)
            for (int hour = 14; hour < 18; hour++) {
                LocalDateTime start = LocalDateTime.of(date, LocalTime.of(hour, 0));
                LocalDateTime end = start.plusHours(1);

                AvailableSlot slot = AvailableSlot.builder()
                        .consultant(consultant)
                        .startAt(start)
                        .endAt(end)
                        .build();
                availableSlotRepository.save(slot);
            }
        }

        log.info("Created available slots for consultant: {}", consultant.getName());
    }

    private String getCareerText(Integer startYear) {
        if (startYear == null) return null;
        int years = java.time.Year.now().getValue() - startYear;
        return years + " years experience";
    }

    private BigDecimal getHourlyRate(ConsultantLevel level) {
        return switch (level) {
            case JUNIOR -> BigDecimal.valueOf(50000);
            case SENIOR -> BigDecimal.valueOf(80000);
            case EXECUTIVE -> BigDecimal.valueOf(120000);
        };
    }
}
