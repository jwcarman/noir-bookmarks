package org.jwcarman.bookmarks;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** The application clock; services take a {@link Clock} so tests can fix the time. */
@Configuration
class ClockConfig {

  @Bean
  Clock clock() {
    return Clock.systemUTC();
  }
}
