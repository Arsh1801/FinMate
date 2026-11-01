package com.finmate.common.config;

import com.finmate.common.util.DateUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.system(DateUtils.KOLKATA_ZONE);
    }
}
