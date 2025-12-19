package cn.edu.sdu.java.server.configs;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfiguration {

    private final AuthenticationProvider authenticationProvider;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http.csrf(csrf -> csrf.disable());

        // 关键：启用 corsConfigurationSource()
        http.cors(cors -> cors.configurationSource(corsConfigurationSource()));

        http.authorizeHttpRequests(auth -> auth
                // 关键：放行预检请求
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()

                // 放行登录/注册/验证码等
                .requestMatchers("/auth/**").permitAll()
                .requestMatchers("/gen/**").permitAll()
                .requestMatchers("/error").permitAll()

                // 你的业务接口需要登录
                .requestMatchers("/api/**").authenticated()

                .anyRequest().permitAll()
        );

        http.sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS));

        http.authenticationProvider(authenticationProvider)
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // ✅ 你前端是 3000，就必须加上 3000
        // 开发阶段你也可以直接用 allowedOriginPatterns("*") 更省事
        configuration.setAllowedOrigins(List.of(
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://localhost:8005",
                "http://127.0.0.1:8005"
        ));

        // 预检一定会用到 OPTIONS
        configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));

        // 建议开发阶段放开所有头
        configuration.setAllowedHeaders(List.of("*"));

        // 如果你要在前端读到 Authorization 之类的响应头，就加这个
        configuration.setExposedHeaders(List.of("Authorization"));

        // 如果前端要带 cookie（一般 jwt 不需要），才开 true
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
