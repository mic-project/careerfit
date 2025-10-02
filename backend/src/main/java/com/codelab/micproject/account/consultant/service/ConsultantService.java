package com.codelab.micproject.account.consultant.service;

import com.codelab.micproject.account.consultant.domain.ConsultantLevel;
import com.codelab.micproject.account.consultant.domain.ConsultantMeta;
import com.codelab.micproject.account.consultant.dto.ConsultantCardDto;
import com.codelab.micproject.account.consultant.repository.ConsultantMetaRepository;
import com.codelab.micproject.account.profile.domain.Profile;
import com.codelab.micproject.account.profile.repository.ProfileRepository;
import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.domain.UserRole;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.review.repository.ReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
public class ConsultantService {

    private final UserRepository userRepository;
    private final ProfileRepository profileRepository;
    private final ConsultantMetaRepository metaRepository;
    private final ReviewRepository reviewRepository;

    /**
     * 공개 컨설턴트 리스트.
     * - 기본 집합: role=CONSULTANT && enabled=true 전원
     * - level / minRating 은 "요청 시"에만 필터
     * - sort: rating(기본) | price | name
     */
    @Transactional(readOnly = true)
    public List<ConsultantCardDto> list(String levelFilter, Double minRating, String sort) {
        var consultants = userRepository.findByRoleAndEnabled(UserRole.CONSULTANT, true);

        Stream<ConsultantCardDto> stream = consultants.stream().map(this::toCard);

        // level 필터
        if (levelFilter != null && !levelFilter.isBlank()) {
            String lv = levelFilter.trim().toUpperCase();
            stream = stream.filter(c -> lv.equals(c.level()));
        }

        // 평점 하한 필터 (avgRating null 안전)
        if (minRating != null) {
            stream = stream.filter(c -> {
                Double r = c.avgRating();
                return r != null && r >= minRating;
            });
        }

        // 정렬
        Comparator<ConsultantCardDto> cmp;
        if ("price".equalsIgnoreCase(sort)) {
            cmp = Comparator.comparing(ConsultantCardDto::price);
        } else if ("name".equalsIgnoreCase(sort)) {
            // 대소문자 무시 정렬
            cmp = Comparator.comparing(c -> {
                String n = c.name();
                return n == null ? "" : n.toLowerCase();
            });
        } else {
            // 기본: avgRating 내림차순 (null은 0으로 간주)
            cmp = Comparator.comparing((ConsultantCardDto c) -> {
                Double r = c.avgRating();
                return r == null ? 0.0 : r;
            }).reversed();
        }

        return stream.sorted(cmp).toList();
    }

    private ConsultantCardDto toCard(User u) {
        Profile p = profileRepository.findByUser(u).orElse(null);

        ConsultantMeta meta = metaRepository.findByConsultant(u).orElse(null);
        ConsultantLevel level = (meta != null && meta.getLevel() != null)
                ? meta.getLevel() : ConsultantLevel.JUNIOR;

        BigDecimal price = (meta != null && meta.getBasePrice() != null)
                ? meta.getBasePrice()
                : defaultPrice(level);

        Double avgObj = reviewRepository.avgRatingByConsultant(u); // null 가능
        double avg = (avgObj != null) ? avgObj : 0.0;
        long count = reviewRepository.countByConsultant(u);

        return new ConsultantCardDto(
                u.getId(),
                (u.getName() != null && !u.getName().isBlank()) ? u.getName() : u.getEmail(),
                level.name(),
                (p != null ? p.getBio() : null),
                price,
                avg,      // avgRating (Double)
                count     // reviewCount (Long)
        );
    }


    private BigDecimal defaultPrice(ConsultantLevel level) {
        return switch (level) {
            case JUNIOR -> BigDecimal.valueOf(30000);
            case SENIOR -> BigDecimal.valueOf(60000);
            case EXECUTIVE -> BigDecimal.valueOf(90000);
        };
    }
}
