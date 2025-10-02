// security/oauth2/userinfo/KakaoUserInfo.java
package com.codelab.micproject.security.oauth2.userinfo;

import java.util.Map;

/**
 * Kakao 응답 샘플 구조:
 * {
 *   "id": 12345,
 *   "kakao_account": { "email": "...", "profile": {"nickname":"...","profile_image_url":"..."} }
 * }
 */
@SuppressWarnings("unchecked")
public class KakaoUserInfo extends OAuth2UserInfo {
    public KakaoUserInfo(Map<String, Object> attributes) { super(attributes); }

    public String getId() { return String.valueOf(attributes.get("id")); }

    public String getEmail() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        return account == null ? null : (String) account.get("email");
    }

    public String getName() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        if (account == null) return null;
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");
        return profile == null ? null : (String) profile.get("nickname");
    }

    public String getImageUrl() {
        Map<String, Object> account = (Map<String, Object>) attributes.get("kakao_account");
        if (account == null) return null;
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");
        return profile == null ? null : (String) profile.get("profile_image_url");
    }
}
