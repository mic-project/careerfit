package com.codelab.micproject.account.user.service;

import com.codelab.micproject.account.consultant.service.ProfileImageStorage;
import com.codelab.micproject.account.user.domain.TicketUsageLog;
import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.dto.TicketUpdateRequest;
import com.codelab.micproject.account.user.dto.UserResponse;
import com.codelab.micproject.account.user.dto.UserUpdateRequest;
import com.codelab.micproject.account.user.repository.TicketUsageLogRepository;
import com.codelab.micproject.account.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final TicketUsageLogRepository ticketUsageLogRepository;
    private final PasswordEncoder passwordEncoder;
    private final ProfileImageStorage profileImageStorage;

    /* 조회 */
    @Transactional(readOnly = true)
    public UserResponse getMe(Long userId) {
        User u = userRepository.findById(userId).orElseThrow();
        return UserResponse.from(u);
    }

    /* JSON PATCH: 파일 없이 필드만 */
    @Transactional
    public UserResponse updateMe(Long userId, UserUpdateRequest req) {
        User u = userRepository.findById(userId).orElseThrow();

        if (req.getName() != null)  u.setName(req.getName());
        if (req.getPhone() != null) validatePhone(req.getPhone(), true);
        if (req.getPhone() != null) u.setPhone(req.getPhone());

        if (req.getPassword() != null && !req.getPassword().isBlank()) {
            u.setPassword(passwordEncoder.encode(req.getPassword()));
        }
        if (req.getProfileImage() != null) {
            u.setProfileImage(req.getProfileImage()); // URL 직접 세팅하고 싶을 때만
        }
        return UserResponse.from(u);
    }

    /* MULTIPART PATCH/POST/PUT: 파일 포함 가능 */
    @Transactional
    public UserResponse updateMe(Long userId, String name, String phone,
                                 String password, MultipartFile profileImage) {
        User u = userRepository.findById(userId).orElseThrow();

        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("이름은 필수입니다.");
        }
        validatePhone(phone, false);

        u.setName(name);
        u.setPhone(phone);

        if (password != null && !password.isBlank()) {
            u.setPassword(passwordEncoder.encode(password));
        }
        if (profileImage != null && !profileImage.isEmpty()) {
            String url = profileImageStorage.store(userId, profileImage);
            u.setProfileImage(url);
        }
        return UserResponse.from(u);
    }

    /* 이용권 수정 */
    @Transactional
    public UserResponse updateTicket(Long userId, TicketUpdateRequest request) {
        User u = userRepository.findById(userId).orElseThrow();
        if (request.getTicketCount() != null)     u.setTicketCount(request.getTicketCount());
        if (request.getTicketStartDate() != null) u.setTicketStartDate(request.getTicketStartDate());
        if (request.getTicketEndDate() != null)   u.setTicketEndDate(request.getTicketEndDate());
        return UserResponse.from(u);
    }

    /* 이용권 차감 + 로그 */
    @Transactional
    public UserResponse useTicket(Long userId) {
        User u = userRepository.findById(userId).orElseThrow();

        if (u.getTicketEndDate() != null && u.getTicketEndDate().isBefore(LocalDate.now())) {
            throw new IllegalStateException("이용권이 만료되었습니다.");
        }
        if (u.getTicketCount() == null || u.getTicketCount() <= 0) {
            throw new IllegalStateException("잔여 이용권이 없습니다.");
        }

        u.setTicketCount(u.getTicketCount() - 1);
        TicketUsageLog log = TicketUsageLog.builder()
                .user(u)
                .remainingTickets(u.getTicketCount())
                .build();
        ticketUsageLogRepository.save(log);

        return UserResponse.from(u);
    }

    /* 유틸 */
    private void validatePhone(String phone, boolean allowNull) {
        if (allowNull && phone == null) return;
        if (phone == null || phone.length() < 10 || phone.length() > 11) {
            throw new IllegalArgumentException("전화번호는 숫자 10~11자리여야 합니다.");
        }
    }
}