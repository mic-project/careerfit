package com.codelab.micproject.security.oauth2;

import com.codelab.micproject.account.user.domain.User; // 프로젝트 경로로 통일
import com.codelab.micproject.account.user.domain.UserRole;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Collection;
import java.util.List;
import java.util.Map;

@Getter
public class UserPrincipal implements OAuth2User, UserDetails {
    private final Long id;
    private final String email;
    private final String name;
    private final UserRole role;
    private final Map<String, Object> attributes;

    public UserPrincipal(User user, Map<String, Object> attributes) {
        this.id = user.getId();
        this.email = user.getEmail();
        this.name = user.getName();
        this.role = user.getRole();
        this.attributes = attributes;
    }

    // ★ 편의 접근자: me.id(), me.email() 형태로 사용 가능
    public Long id() { return id; }
    public String email() { return email; }

    // 권한 부여 (스프링 시큐리티가 ROLE_ 접두어를 기대)
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        String roleName = (role != null ? role.name() : "USER");
        return List.of(new SimpleGrantedAuthority("ROLE_" + roleName));
    }

    @Override public Map<String, Object> getAttributes() { return attributes; }
    @Override public String getName() { return String.valueOf(id); }

    // UserDetails 구현
    @Override public String getUsername() { return String.valueOf(id); }
    @Override public String getPassword() { return null; } // OAuth2 사용자는 비밀번호 없음
    @Override public boolean isAccountNonExpired() { return true; }
    @Override public boolean isAccountNonLocked() { return true; }
    @Override public boolean isCredentialsNonExpired() { return true; }
    @Override public boolean isEnabled() { return true; }
}
