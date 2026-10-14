package app.infrastructure.security;

import java.io.IOException;
import java.util.List;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import app.domain.ports.out.JwtServicePort;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Authenticates incoming requests from a {@code Authorization: Bearer <token>}
 * header by validating the JWT and populating the Spring {@code SecurityContext}.
 *
 * <p>Stateless by design: no server-side session is created. A missing, invalid
 * or expired token simply leaves the context anonymous, so protected endpoints
 * are rejected by the security filter chain with 401.</p>
 *
 * <p>Registered as an explicit bean by {@code SecurityConfig} rather than via
 * component scanning, so it is only part of the full security chain and never
 * auto-created inside {@code @WebMvcTest} slices.</p>
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String HEADER = "Authorization";
    private static final String PREFIX = "Bearer ";

    private final JwtServicePort jwtServicePort;

    public JwtAuthenticationFilter(JwtServicePort jwtServicePort) {
        this.jwtServicePort = jwtServicePort;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String header = request.getHeader(HEADER);
        if (header != null && header.startsWith(PREFIX)
                && SecurityContextHolder.getContext().getAuthentication() == null) {
            String token = header.substring(PREFIX.length());
            jwtServicePort.parseToken(token).ifPresent(claims -> {
                var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + claims.role()));
                var authentication = new UsernamePasswordAuthenticationToken(claims.subject(), null, authorities);
                SecurityContextHolder.getContext().setAuthentication(authentication);
            });
        }
        chain.doFilter(request, response);
    }
}
