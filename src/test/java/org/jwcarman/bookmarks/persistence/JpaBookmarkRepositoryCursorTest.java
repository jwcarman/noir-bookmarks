package org.jwcarman.bookmarks.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.Cursor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaBookmarkRepository.class)
class JpaBookmarkRepositoryCursorTest {

  @Autowired JpaBookmarkRepository repository;
  @Autowired TestEntityManager entityManager;

  Bookmark save(String title, String createdAt, String... tags) {
    return repository.save(
        new Bookmark(
            null,
            "https://example.com/" + title.replace(' ', '-'),
            title,
            List.of(tags),
            Instant.parse(createdAt)));
  }

  void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void findAfterWalksNewestFirstWithIdTieBreak() {
    var d = save("D", "2024-01-01T00:00:00Z");
    var b = save("B", "2024-01-02T00:00:00Z");
    var c = save("C", "2024-01-02T00:00:00Z");
    var a = save("A", "2024-01-03T00:00:00Z");
    flushAndClear();
    assertThat(b.id()).isLessThan(c.id());

    assertThat(repository.findAfter(null, 2)).containsExactly(a, c);
    assertThat(repository.findAfter(Cursor.of(a), 2)).containsExactly(c, b);
    assertThat(repository.findAfter(Cursor.of(c), 2)).containsExactly(b, d);
    assertThat(repository.findAfter(Cursor.of(b), 10)).containsExactly(d);
    assertThat(repository.findAfter(Cursor.of(d), 2)).isEmpty();
    assertThat(repository.findAfter(null, 10)).containsExactly(a, c, b, d);
  }

  @Test
  void findAfterADeletedBookmark() {
    var d = save("D", "2024-01-01T00:00:00Z");
    var b = save("B", "2024-01-02T00:00:00Z");
    var c = save("C", "2024-01-02T00:00:00Z");
    save("A", "2024-01-03T00:00:00Z");
    flushAndClear();

    assertThat(repository.deleteById(c.id())).isTrue();
    flushAndClear();

    assertThat(repository.findAfter(new Cursor(c.createdAt(), c.id()), 10)).containsExactly(b, d);
  }

  @Test
  void searchAfterPagesThroughMatchesOnly() {
    var java3 = save("Java 3", "2024-01-01T00:00:00Z");
    var java2 = save("Java 2", "2024-01-02T00:00:00Z", "x");
    save("Kotlin", "2024-01-03T00:00:00Z", "x");
    var java1 = save("Java 1", "2024-01-04T00:00:00Z", "x");
    flushAndClear();

    assertThat(repository.searchAfter("java", Set.of("x"), null, 1)).containsExactly(java1);
    assertThat(repository.searchAfter("java", Set.of("x"), Cursor.of(java1), 1))
        .containsExactly(java2);
    assertThat(repository.searchAfter("java", Set.of("x"), Cursor.of(java2), 1)).isEmpty();
    assertThat(repository.searchAfter("java", Set.of(), Cursor.of(java2), 5))
        .containsExactly(java3);
    assertThat(repository.searchAfter("", Set.of(), null, 10)).hasSize(4);
  }
}
