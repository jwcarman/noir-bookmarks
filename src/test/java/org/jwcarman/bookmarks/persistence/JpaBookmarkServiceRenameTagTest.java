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
class JpaBookmarkServiceRenameTagTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  @Autowired JpaBookmarkRepository repository;
  @Autowired TestEntityManager entityManager;

  @Test
  void renamePersistsTags() {
    var service = new BookmarkService(repository, Clock.fixed(NOON, ZoneOffset.UTC));
    var a = repository.save(
        new Bookmark(null, "https://a.example", "A", List.of("js", "web", "javascript"), NOON));
    var b = repository.save(new Bookmark(null, "https://b.example", "B", List.of("java"), NOON));
    entityManager.flush();
    entityManager.clear();

    assertThat(service.renameTag("JS", "javascript")).isEqualTo(1);
    entityManager.flush();
    entityManager.clear();

    assertThat(service.get(a.id()))
        .isEqualTo(new Bookmark(a.id(), "https://a.example", "A", List.of("javascript", "web"), NOON));
    assertThat(service.get(b.id()).tags()).containsExactly("java");
    assertThat(repository.count()).isEqualTo(2);
  }
}
