// security/oauth2/userinfo/UserInfoFactory.java
package com.codelab.micproject.security.oauth2.userinfo;

import com.codelab.micproject.account.user.domain.AuthProvider;

import java.util.Map;

public class UserInfoFactory {
    public static OAuth2UserInfo from(AuthProvider provider, Map<String, Object> attributes) {
        return switch (provider) {
            case GOOGLE -> new GoogleUserInfo(attributes);
            case KAKAO  -> new KakaoUserInfo(attributes);
            case NAVER  -> new NaverUserInfo(attributes);
            default     -> throw new IllegalArgumentException("Unsupported provider: " + provider);
        };
    }
}
