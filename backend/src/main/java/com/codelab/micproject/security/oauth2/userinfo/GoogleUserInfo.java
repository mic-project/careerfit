// security/oauth2/userinfo/GoogleUserInfo.java
package com.codelab.micproject.security.oauth2.userinfo;

import java.util.Map;

public class GoogleUserInfo extends OAuth2UserInfo {
    public GoogleUserInfo(Map<String, Object> attributes) { super(attributes); }
    public String getId() { return (String) attributes.get("sub"); }
    public String getEmail() { return (String) attributes.get("email"); }
    public String getName() { return (String) attributes.get("name"); }
    public String getImageUrl() { return (String) attributes.get("picture"); }
}
