// security/oauth2/userinfo/OAuth2UserInfo.java
package com.codelab.micproject.security.oauth2.userinfo;

import java.util.Map;

public abstract class OAuth2UserInfo {
    protected final Map<String, Object> attributes;
    protected OAuth2UserInfo(Map<String, Object> attributes) { this.attributes = attributes; }
    public abstract String getId();
    public abstract String getEmail();
    public abstract String getName();
    public abstract String getImageUrl();
    public Map<String, Object> getRaw() { return attributes; }
}
