package org.jwcarman.bookmarks;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

@SpringBootTest
class BookmarksApplicationTest {

  @Autowired ApplicationContext context;

  @Test
  void contextLoads() {
    assertThat(context.getBeansOfType(Clock.class)).hasSize(1);
  }
}
