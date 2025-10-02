// config/WebMvcConfig.java
package com.codelab.micproject.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.*;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    @Override
    public void addViewControllers(ViewControllerRegistry r) {
        r.addViewController("/health").setViewName("forward:/");
    }
}
