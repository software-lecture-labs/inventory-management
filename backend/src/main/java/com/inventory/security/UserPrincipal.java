package com.inventory.security;

import com.inventory.model.Role;
import com.inventory.model.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

/**
 * Oturum açmış kullanıcının Spring Security tarafındaki temsili.
 * Controller'larda @AuthenticationPrincipal ile alınır.
 */
public record UserPrincipal(
        Long id,
        String email,
        String fullName,
        String password,
        Role role,
        boolean enabled,
        // true = tek kullanımlık şifreyle girdi; yeni şifre belirleyene kadar sadece şifre değiştirebilir
        boolean mustChangePassword
) implements UserDetails {

    public static UserPrincipal from(User user) {
        return new UserPrincipal(user.getId(), user.getEmail(), user.getFullName(), user.getPassword(),
                user.getRole(), user.isEnabled(), user.isMustChangePassword());
    }

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }
}
