package com.pobadora.midterm.Config;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity


public class SecurityConfig {

        @Bean
        public SecurityFilterChain defaultSecurityChain(HttpSecurity http) throws Exception{
            return http
                    .authorizeHttpRequests(authorize -> authorize.anyRequest().authenticated())
                    .oauth2Login(oauth -> oauth.defaultSuccessUrl("/user-info", true))
                    .logout(logout -> logout.logoutSuccessUrl("/"))
                    .formLogin(formLogin -> formLogin.defaultSuccessUrl("/user-info", true))
                    .build();
        }
    }
