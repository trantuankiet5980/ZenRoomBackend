package vn.edu.iuh.fit.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {
    private final JwtFilter jwtFilter;

    // Danh sách các endpoint public
    private static final String[] PUBLIC_ENDPOINTS = {
            "/api/v1/auth/**",
            "/public/**",
            "/api/v1/users/**" // Nếu bạn muốn cho GET và POST public luôn thì đưa vào đây
    };

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Dùng biến PUBLIC_ENDPOINTS cho các endpoint public
                        .requestMatchers(PUBLIC_ENDPOINTS).permitAll()

                        // USERS
                        // Nếu chỉ muốn một số method public, thì cần viết riêng
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").hasRole("ADMIN")

                        // ROLES
                        .requestMatchers("/api/v1/roles/**").hasRole("ADMIN")

                        // Custom roles
                        .requestMatchers("/admin/**").hasRole("ADMIN")
                        .requestMatchers("/landlord/**").hasAnyRole("LANDLORD", "ADMIN")
                        .requestMatchers("/tenant/**").hasAnyRole("TENANT", "ADMIN")

                        .anyRequest().authenticated()
                )
                .oauth2ResourceServer(oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())))
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public JwtAuthenticationConverter jwtAuthenticationConverter() {
        JwtGrantedAuthoritiesConverter grantedAuthoritiesConverter = new JwtGrantedAuthoritiesConverter();
        grantedAuthoritiesConverter.setAuthorityPrefix("ROLE_");
        grantedAuthoritiesConverter.setAuthoritiesClaimName("roles");

        JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
        jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(grantedAuthoritiesConverter);
        return jwtAuthenticationConverter;
    }
}
