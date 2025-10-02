// security/oauth2/CustomOAuth2UserService.java
package com.codelab.micproject.security.oauth2;

import com.codelab.micproject.account.user.domain.AuthProvider;
import com.codelab.micproject.account.user.domain.User;
import com.codelab.micproject.account.user.domain.UserRole;
import com.codelab.micproject.account.user.repository.UserRepository;
import com.codelab.micproject.security.oauth2.userinfo.OAuth2UserInfo;
import com.codelab.micproject.security.oauth2.userinfo.UserInfoFactory;

import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.client.userinfo.*;
import org.springframework.security.oauth2.core.*;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import lombok.extern.slf4j.Slf4j;


import java.util.Map;

/**
 * 흐름:
 * 1) provider 식별(registrationId) -> OAuth2UserInfo로 표준화
 * 2) provider+providerId로 사용자 조회, 없으면 신규 가입
 * 3) 이메일 제공 시 email unique 유지 (정책에 따라 병합/충돌 처리)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class CustomOAuth2UserService extends DefaultOAuth2UserService {

    private final UserRepository userRepository;

    @Override
    public OAuth2User loadUser(OAuth2UserRequest req) throws OAuth2AuthenticationException {
        OAuth2User oAuth2User = super.loadUser(req);
        String registrationId = req.getClientRegistration().getRegistrationId(); // google/kakao/naver
        AuthProvider provider = AuthProvider.valueOf(registrationId.toUpperCase());

        Map<String, Object> attributes = oAuth2User.getAttributes();
        OAuth2UserInfo info = UserInfoFactory.from(provider, attributes);
        if (info.getId() == null) throw new OAuth2AuthenticationException(new OAuth2Error("id_null"), "Provider id missing");

        // 존재여부만 판별
        var existing = userRepository.findByProviderAndProviderId(provider, info.getId()).orElse(null);

        log.debug("OAUTH2 user: provider={}, providerId={}, email={}, exists={}",
                provider, info.getId(), info.getEmail(), existing != null);


        // DB 저장은 절대 하지 않음. Principal에 필요한 최소 정보만 전달
        return new SocialTempPrincipal(
                provider, info.getId(), info.getEmail(), info.getName(), info.getImageUrl(),
                existing != null, attributes
        );
    }
}
