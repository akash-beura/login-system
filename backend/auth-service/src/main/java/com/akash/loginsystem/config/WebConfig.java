package com.akash.loginsystem.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Explicitly configure static resource handling to ensure /api/** routes
 * are NOT treated as static resources and instead routed to Spring MVC controllers.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Static resources only in specific directories
        registry
            .addResourceHandler("/static/**", "/public/**", "/css/**", "/js/**", "/images/**")
            .addResourceLocations("classpath:/static/", "classpath:/public/")
            .setCachePeriod(31536000); // 1 year cache for assets
        
        // Ensure /api/** is NOT handled as static resources
        // All /api/** requests will be routed to Spring MVC controllers
    }
}
