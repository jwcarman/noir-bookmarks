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
class JpaBookmarkServiceNotesTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  @Autowired JpaBookmarkRepository repository;
  @Autowired TestEntityManager entityManager;

  BookmarkService service() {
    return new BookmarkService(repository, Clock.fixed(NOON, ZoneOffset.UTC));
  }

  void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void updateReplacesNotesAndPlainUpdateKeepsThem() {
    var service = service();
    var saved = service.create("https://a.example/", "A", List.of(), "old");
    flushAndClear();

    service.update(saved.id(), "T", List.of("x"), "new");
    flushAndClear();
    assertThat(service.get(saved.id()))
        .isEqualTo(new Bookmark(saved.id(), "https://a.example/", "T", List.of("x"), NOON, "new"));

    service.update(saved.id(), "U", List.of());
    flushAndClear();
    assertThat(service.get(saved.id()).notes()).isEqualTo("new");
  }

  @Test
  void renameTagKeepsNotes() {
    var service = service();
    var saved = service.create("https://a.example/", "A", List.of("java"), "keep");
    flushAndClear();

    assertThat(service.renameTag("java", "jvm")).isEqualTo(1);
    flushAndClear();
    var stored = service.get(saved.id());
    assertThat(stored.tags()).containsExactly("jvm");
    assertThat(stored.notes()).isEqualTo("keep");
  }
}
