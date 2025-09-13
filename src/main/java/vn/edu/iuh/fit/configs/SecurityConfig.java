package vn.edu.iuh.fit.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
public class SecurityConfig {
    private final JwtFilter jwtFilter;

    public SecurityConfig(JwtFilter jwtFilter) {
        this.jwtFilter = jwtFilter;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .cors(Customizer.withDefaults())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                        // USERS
                        .requestMatchers(HttpMethod.GET, "/api/v1/users/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/users/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/users/**").permitAll()
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/users/**").permitAll()

                        // ROLES
                        .requestMatchers(HttpMethod.GET, "/api/v1/roles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/roles/**").permitAll()
                        .requestMatchers(HttpMethod.PUT, "/api/v1/roles/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/roles/**").hasRole("ADMIN")

                        // PROPERTIES
                        .requestMatchers(HttpMethod.GET, "/api/v1/properties/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/properties/**").hasAnyRole("LANDLORD", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/properties/**").hasAnyRole("LANDLORD", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/properties/**").hasAnyRole("LANDLORD", "ADMIN")

                        // FURNISHINGS
                        .requestMatchers(HttpMethod.GET, "/api/v1/furnishings/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/furnishings/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/furnishings/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/furnishings/**").hasRole("ADMIN")

                        // ROOM TYPES
                        .requestMatchers(HttpMethod.GET, "/api/v1/room-types/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/room-types/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/room-types/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/room-types/**").hasRole("ADMIN")

                        // ENUMS
                        .requestMatchers(HttpMethod.GET, "/api/v1/enums/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/enums/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/enums/**").hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/enums/**").hasRole("ADMIN")

                        // FAVORITES
                        .requestMatchers(HttpMethod.GET, "/api/v1/favorites/**").hasAnyRole( "TENANT", "ADMIN")
                        .requestMatchers(HttpMethod.POST, "/api/v1/favorites/**").hasAnyRole( "TENANT", "ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/favorites/**").hasAnyRole( "TENANT", "ADMIN")


                        // Custom roles
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")
                        .requestMatchers("/landlord/**").hasAnyRole("LANDLORD", "ADMIN")
                        .requestMatchers("/tenant/**").hasAnyRole("TENANT", "ADMIN")

                        //STATS
                        .requestMatchers("/api/v1/admin/**").hasRole("ADMIN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:3000")); // FE origin
        config.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        config.setAllowedHeaders(List.of("*"));                      // hoặc liệt kê: Content-Type, Authorization, ...
        config.setExposedHeaders(List.of("Authorization"));          // nếu cần đọc header này
        config.setAllowCredentials(false); // nếu dùng "*" phải false, còn nếu cần cookie thì true + không dùng "*"
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
