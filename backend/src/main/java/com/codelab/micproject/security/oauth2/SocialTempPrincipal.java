// security/oauth2/SocialTempPrincipal.java
package com.codelab.micproject.security.oauth2;

import com.codelab.micproject.account.user.domain.AuthProvider;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class SocialTempPrincipal implements OAuth2User {
    private final AuthProvider provider;
    private final String providerId;
    private final String email;
    private final String name;
    private final String imageUrl;
    private final boolean exists;
    private final Map<String, Object> attributes;

    public SocialTempPrincipal(AuthProvider provider, String providerId, String email, String name, String imageUrl, boolean exists, Map<String, Object> attributes) {
        this.provider = provider;
        this.providerId = providerId;
        this.email = email;
        this.name = name;
        this.imageUrl = imageUrl;
        this.exists = exists;
        this.attributes = attributes;
    }

    @Override public Map<String, Object> getAttributes() { return attributes; }
    @Override public Collection<? extends GrantedAuthority> getAuthorities() { return List.of(); }
    @Override public String getName() { return String.valueOf(provider) + ":" + providerId; }
}
