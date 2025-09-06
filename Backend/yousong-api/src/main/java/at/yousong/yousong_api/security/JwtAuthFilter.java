package at.yousong.yousong_api.security;

import at.yousong.yousong_api.user.BenutzerRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final BenutzerRepository benutzerRepository;

    public JwtAuthFilter(JwtUtil jwtUtil, BenutzerRepository benutzerRepository) {
        this.jwtUtil = jwtUtil;
        this.benutzerRepository = benutzerRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {

        String auth = request.getHeader("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            String token = auth.substring(7);
            try {
                String username = jwtUtil.validateAndExtractUsername(token);
                // Existiert der User?
                benutzerRepository.findByUsername(username).ifPresent(u -> {
                    var authToken = new UsernamePasswordAuthenticationToken(
                            username, null, AuthorityUtils.NO_AUTHORITIES
                    );
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                });
            } catch (Exception ignored) { /* ungültiges Token -> als anonym weiter */ }
        }
        chain.doFilter(request, response);
    }
}
