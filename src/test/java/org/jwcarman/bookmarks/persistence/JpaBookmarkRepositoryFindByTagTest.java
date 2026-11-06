package org.jwcarman.bookmarks.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaBookmarkRepository.class)
class JpaBookmarkRepositoryFindByTagTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  @Autowired JpaBookmarkRepository repository;
  @Autowired TestEntityManager entityManager;

  Bookmark save(String url, Instant at, String... tags) {
    return repository.save(new Bookmark(null, url, "title", List.of(tags), at));
  }

  void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void returnsTaggedNewestFirst() {
    var a = save("https://a.example", NOON, "java", "spring");
    var b = save("https://b.example", NOON.plusSeconds(60), "java");
    save("https://c.example", NOON.plusSeconds(120), "kotlin");
    flushAndClear();

    assertThat(repository.findByTag("java")).containsExactly(b, a);
    assertThat(repository.findByTag("spring")).containsExactly(a);
    assertThat(repository.findByTag("python")).isEmpty();
    assertThat(repository.findByTag("Java")).isEmpty();
    assertThat(repository.findByTag("jav")).isEmpty();
  }

  @Test
  void sameInstantHigherIdFirst() {
    var first = save("https://1.example", NOON, "java");
    var second = save("https://2.example", NOON, "java", "x");
    flushAndClear();

    assertThat(first.id()).isLessThan(second.id());
    assertThat(repository.findByTag("java")).containsExactly(second, first);
  }

  @Test
  void emptyRepository() {
    assertThat(repository.findByTag("java")).isEmpty();
  }
}
