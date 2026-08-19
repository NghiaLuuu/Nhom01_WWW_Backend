package vn.edu.iuh.nhom01_www.common.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.User;

import java.util.Collection;

public class CustomUserDetails extends User {
    private final Long version;

    public CustomUserDetails(String username, String password, Collection<? extends GrantedAuthority> authorities, Long version) {
        super(username, password, authorities);
        this.version = version;
    }

    public CustomUserDetails(String username, String password, boolean enabled, boolean accountNonExpired, boolean credentialsNonExpired, boolean accountNonLocked, Collection<? extends GrantedAuthority> authorities, Long version) {
        super(username, password, enabled, accountNonExpired, credentialsNonExpired, accountNonLocked, authorities);
        this.version = version;
    }

    public Long getVersion() {
        return version;
    }
}
