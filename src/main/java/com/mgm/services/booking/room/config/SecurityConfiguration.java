package com.mgm.services.booking.room.config;

import java.time.Duration;
import java.util.Arrays;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configuration.WebSecurityConfigurerAdapter;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import com.mgm.services.booking.room.constant.ServiceConstant;
import com.mgm.services.booking.room.properties.ApplicationProperties;

import lombok.extern.log4j.Log4j2;

/**
 * Configuration class to set security settings for the application end points.
 *
 */
@Configuration
@EnableWebSecurity
@EnableRetry
@Log4j2
public class SecurityConfiguration extends WebSecurityConfigurerAdapter {

    /*
     * (non-Javadoc)
     * 
     * @see org.springframework.security.config.annotation.web.configuration.
     * WebSecurityConfigurerAdapter#configure(org.springframework.security.
     * config.annotation.web.builders.HttpSecurity)
     */
    @Override
    protected void configure(HttpSecurity http) throws Exception {

        // Disabling csrf and allowing all requests
        http.authorizeRequests().anyRequest().permitAll();
        http.csrf().disable();
        http.cors();
        http.sessionManagement().sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED);
        // Enabling content security policy to mitigate XSS and data injection
        http.headers().xssProtection().and().contentSecurityPolicy("script-src 'self'");
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(ApplicationProperties appProps) {
        log.info("Allowed Origins: {}", Arrays.asList(appProps.getCorsOrigin()));
        CorsConfiguration configuration = new CorsConfiguration();
        configuration.setAllowedOrigins(Arrays.asList(appProps.getCorsOrigin()));
        configuration.setAllowedMethods(Arrays.asList("*"));
        configuration.setAllowCredentials(true);
        configuration.setAllowedHeaders(Arrays.asList("*"));
        configuration.setMaxAge(Duration.ofSeconds(ServiceConstant.SESSION_EXPIRY));
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);
        return source;
    }
}
