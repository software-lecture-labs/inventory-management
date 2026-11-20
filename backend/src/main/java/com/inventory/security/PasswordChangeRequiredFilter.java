package com.inventory.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.inventory.exception.ApiError;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

/**
 * Yöneticinin verdiği tek kullanımlık şifreyle giriş yapan kullanıcı, yeni şifresini belirleyene kadar
 * sadece kendi bilgisini okuyabilir ve şifresini değiştirebilir. Diğer tüm API istekleri 403 döner.
 * Bu kural arayüzden bağımsızdır; doğrudan API'ye istek atılsa da geçerlidir.
 */
public class PasswordChangeRequiredFilter extends OncePerRequestFilter {

    private static final Set<String> ALLOWED = Set.of("/api/auth/me", "/api/auth/change-password");

    private final ObjectMapper objectMapper;

    public PasswordChangeRequiredFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null
                && auth.getPrincipal() instanceof UserPrincipal principal
                && principal.mustChangePassword()
                && !ALLOWED.contains(request.getRequestURI())) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.setCharacterEncoding("UTF-8");
            objectMapper.writeValue(response.getOutputStream(), ApiError.of(HttpServletResponse.SC_FORBIDDEN,
                    "Devam etmek için önce yeni şifrenizi belirlemelisiniz"));
            return;
        }
        filterChain.doFilter(request, response);
    }
}
