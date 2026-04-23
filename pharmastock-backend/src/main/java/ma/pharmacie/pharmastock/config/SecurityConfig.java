package ma.pharmacie.pharmastock.config;

import lombok.RequiredArgsConstructor;
import ma.pharmacie.pharmastock.security.JwtAuthFilter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final UserDetailsService userDetailsService;

    @Value("${app.cors.allowed-origins:http://localhost:3000}")
    private String allowedOrigins;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/v1/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/swagger-ui.html", "/v3/api-docs/**", "/api/docs/**").permitAll()

                        .requestMatchers(HttpMethod.GET, "/api/v1/medicaments/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/stock/**").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/v1/dashboard/**").authenticated()

                        .requestMatchers("/api/v1/ventes/**")
                        .hasAnyRole("ADMIN", "PHARMACIEN", "CAISSIER")

                        .requestMatchers("/api/v1/commandes/**", "/api/v1/receptions/**", "/api/v1/inventaires/**")
                        .hasAnyRole("ADMIN", "PHARMACIEN", "GESTIONNAIRE_STOCK")

                        .requestMatchers("/api/v1/alertes/**")
                        .hasAnyRole("ADMIN", "PHARMACIEN", "GESTIONNAIRE_STOCK")

                        .requestMatchers("/api/v1/rapports/**")
                        .hasAnyRole("ADMIN", "PHARMACIEN")

                        .requestMatchers("/api/v1/utilisateurs/**")
                        .hasRole("ADMIN")

                        .requestMatchers("/api/v1/audit/**")
                        .hasAnyRole("ADMIN", "PHARMACIEN")

                        .requestMatchers(HttpMethod.POST, "/api/v1/medicaments/**")
                        .hasAnyRole("ADMIN", "PHARMACIEN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/medicaments/**")
                        .hasAnyRole("ADMIN", "PHARMACIEN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/medicaments/**")
                        .hasAnyRole("ADMIN", "PHARMACIEN")

                        .anyRequest().authenticated()
                )
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();

        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();

        config.setAllowedOrigins(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS", "PATCH"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(10);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}