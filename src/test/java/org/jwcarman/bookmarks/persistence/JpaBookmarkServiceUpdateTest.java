package org.jwcarman.bookmarks.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.service.BookmarkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaBookmarkRepository.class)
class JpaBookmarkServiceUpdateTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  @Autowired JpaBookmarkRepository repository;
  @Autowired TestEntityManager entityManager;

  @Test
  void updatePersistsTitleAndTags() {
    var service = new BookmarkService(repository, Clock.fixed(NOON, ZoneOffset.UTC));
    var saved = repository.save(new Bookmark(null, "https://a.example", "Old", List.of("java"), NOON));
    entityManager.flush();
    entityManager.clear();

    var updated = service.update(saved.id(), "New", List.of(" Spring ", "JAVA", "spring"));
    entityManager.flush();
    entityManager.clear();

    var expected = new Bookmark(saved.id(), "https://a.example", "New", List.of("spring", "java"), NOON);
    assertThat(updated).isEqualTo(expected);
    assertThat(service.get(saved.id())).isEqualTo(expected);
    assertThat(repository.count()).isEqualTo(1);
  }
}
