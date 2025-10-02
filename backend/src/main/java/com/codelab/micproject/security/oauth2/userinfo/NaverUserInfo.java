package com.codelab.micproject.security.oauth2.userinfo;

import java.util.Map;

public class NaverUserInfo extends OAuth2UserInfo {
    private final Map<String, Object> response;

    @SuppressWarnings("unchecked")
    public NaverUserInfo(Map<String, Object> attributes) {
        super(attributes);
        // 네이버: { "resultcode":"00", "message":"success", "response":{...} }
        this.response = (Map<String, Object>) attributes.get("response");
    }

    @Override
    public String getId() {
        return response == null ? null : String.valueOf(response.get("id"));
    }

    @Override
    public String getEmail() {
        return response == null ? null : (String) response.get("email");
    }

    @Override
    public String getName() {
        if (response == null) return null;
        if (response.get("name") != null) return (String) response.get("name");
        return (String) response.get("nickname");
    }

    @Override
    public String getImageUrl() {
        return response == null ? null : (String) response.get("profile_image");
    }
}
