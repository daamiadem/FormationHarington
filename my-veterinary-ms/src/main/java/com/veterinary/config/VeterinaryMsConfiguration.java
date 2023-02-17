package com.veterinary.config;


import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;

@Configuration
@ComponentScan(basePackages = {"com.veterinary"})
@EnableJpaRepositories(basePackages = {"com.veterinary.repository"})
public class VeterinaryMsConfiguration {
}
