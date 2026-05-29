package com.ohgiraffers.backend.config;

import java.util.List;

import com.ohgiraffers.backend.auth.JwtService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configuration.WebSecurityCustomizer;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

@Configuration
public class SecurityConfig {
    @Bean
    WebSecurityCustomizer webSecurityCustomizer() {
        return web -> web.ignoring().requestMatchers(PathPatternRequestMatcher.pathPattern("/api/auth/**"));
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService) throws Exception {
        http.csrf(csrf -> csrf.disable())
                .cors(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(PathPatternRequestMatcher.pathPattern("/error")).permitAll()
                        .requestMatchers(PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/actuator/health/**")).permitAll()
                        .requestMatchers(PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/actuator/info")).permitAll()
                        .requestMatchers(PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/actuator/prometheus")).permitAll()
                        .requestMatchers(PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/ai/health")).permitAll()
                        .requestMatchers(PathPatternRequestMatcher.pathPattern(HttpMethod.POST, "/api/analyses")).permitAll()
                        .requestMatchers(PathPatternRequestMatcher.pathPattern(HttpMethod.GET, "/api/hospitals/search")).permitAll()
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthFilter(jwtService), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    private OncePerRequestFilter jwtAuthFilter(JwtService jwtService) {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request, jakarta.servlet.http.HttpServletResponse response, jakarta.servlet.FilterChain filterChain) throws java.io.IOException, jakarta.servlet.ServletException {
                String authorization = request.getHeader("Authorization");
                if (authorization != null && authorization.startsWith("Bearer ")) {
                    try {
                        String username = jwtService.verify(authorization.substring(7), "access").getSubject();
                        var auth = new UsernamePasswordAuthenticationToken(username, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
                        org.springframework.security.core.context.SecurityContextHolder.getContext().setAuthentication(auth);
                    } catch (Exception ignored) {
                        org.springframework.security.core.context.SecurityContextHolder.clearContext();
                    }
                }
                filterChain.doFilter(request, response);
            }
        };
    }
}
