package com.photoday.photoday.security.configuration;

import com.photoday.photoday.security.filter.JwtAuthenticationFilter;
import com.photoday.photoday.security.filter.JwtVerificationFilter;
import com.photoday.photoday.security.handler.*;
import com.photoday.photoday.security.jwt.JwtProvider;
import com.photoday.photoday.security.principaldetails.PrincipalDetailsService;
import com.photoday.photoday.security.redis.service.RedisService;
import com.photoday.photoday.security.utils.CustomAuthorityUtils;
import com.photoday.photoday.security.utils.UserDataResponder;
import com.photoday.photoday.user.service.UserService;
import com.photoday.photoday.util.TempPassword;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.client.web.OAuth2LoginAuthenticationFilter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@RequiredArgsConstructor
public class SecurityConfiguration {
    private final JwtProvider jwtProvider;
    private final CustomAuthorityUtils customAuthorityUtils;
    private final RedisService redisService;
    private final PrincipalDetailsService principalDetailsService;
    private final UserDataResponder userDataResponder;
    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final TempPassword tempPassword;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .headers().frameOptions().sameOrigin()
                .and()
                .csrf().disable()
                .cors(withDefaults())
                .sessionManagement().sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                .and()
                .formLogin().disable()
                .httpBasic().disable()
                .exceptionHandling()
                .authenticationEntryPoint(new UserAuthenticationEntryPoint())
                .accessDeniedHandler(new UserAccessDeniedHandler())
                .and()
                .apply(new CustomFilterConfigurer())
                .and()
                .authorizeHttpRequests(authorize -> authorize
                        .antMatchers(HttpMethod.POST, "/*/users").permitAll()
                        .antMatchers(HttpMethod.POST, "/*/users/update").authenticated()
                        .antMatchers(HttpMethod.GET, "/*/users/**").permitAll()
                        .antMatchers(HttpMethod.DELETE, "/*/users").authenticated()
                        .antMatchers(HttpMethod.PATCH, "/*/follows/*").authenticated()
                        .antMatchers(HttpMethod.GET, "/*/follows/**").permitAll()
                        .antMatchers(HttpMethod.POST, "/*/images").authenticated()
                        .antMatchers(HttpMethod.PATCH, "/*/images/**").authenticated()
                        .antMatchers(HttpMethod.GET, "/*/images/bookmarks/**").authenticated()
                        .antMatchers(HttpMethod.GET, "/*/images/*").permitAll()
                        .antMatchers(HttpMethod.DELETE, "/*/images/**").authenticated()
                        .antMatchers(HttpMethod.POST, "/*/images/*/reports").authenticated()
                        .antMatchers(HttpMethod.PATCH, "/*/images/*/likes").authenticated()
                        .antMatchers(HttpMethod.GET, "/*/tags/search/**").permitAll()
                        .anyRequest().permitAll()
                )
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(new OAuth2SuccessHandler(jwtProvider, userService, passwordEncoder, tempPassword))
                        .failureHandler(new OAuth2FailureHandler()));

        return http.build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.addAllowedOrigin("http://localhost:3000");
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        configuration.addAllowedHeader("*");
        configuration.addExposedHeader("*");
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }

    public class CustomFilterConfigurer extends AbstractHttpConfigurer<CustomFilterConfigurer, HttpSecurity> {
        @Override
        public void configure(HttpSecurity builder) {
            AuthenticationManager authenticationManager = builder.getSharedObject(AuthenticationManager.class);

            JwtAuthenticationFilter jwtAuthenticationFilter = new JwtAuthenticationFilter(authenticationManager, userService, jwtProvider, redisService, userDataResponder);
            jwtAuthenticationFilter.setFilterProcessesUrl("/api/auth/login");
            jwtAuthenticationFilter.setAuthenticationSuccessHandler(new UserAuthenticationSuccessHandler());
            jwtAuthenticationFilter.setAuthenticationFailureHandler(new UserAuthenticationFailureHandler());

            JwtVerificationFilter jwtVerificationFilter = new JwtVerificationFilter(jwtProvider, customAuthorityUtils, principalDetailsService, userService);

            builder
                    .addFilter(jwtAuthenticationFilter)
                    .addFilterAfter(jwtVerificationFilter, OAuth2LoginAuthenticationFilter.class);

        }
    }
}
