package org.jwcarman.bookmarks.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.service.BookmarkService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaBookmarkRepository.class)
class JpaBookmarkRepositoryDeleteByTagTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  @Autowired JpaBookmarkRepository repository;
  @Autowired TestEntityManager entityManager;

  Bookmark a;
  Bookmark b;
  Bookmark c;

  @BeforeEach
  void seed() {
    a = repository.save(new Bookmark(null, "https://a.example", "A", List.of("java", "web"), NOON));
    b = repository.save(new Bookmark(null, "https://b.example", "B", List.of("java"), NOON));
    c = repository.save(new Bookmark(null, "https://c.example", "C", List.of("go"), NOON));
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void deletesExactlyTheTaggedBookmarks() {
    assertThat(repository.deleteByTag("java")).isEqualTo(2);
    entityManager.clear();
    assertThat(repository.count()).isEqualTo(1);
    assertThat(repository.findById(c.id())).contains(c);
    assertThat(repository.findById(a.id())).isEmpty();
    assertThat(repository.findById(b.id())).isEmpty();
  }

  @Test
  void matchIsCaseSensitive() {
    assertThat(repository.deleteByTag("JAVA")).isZero();
    assertThat(repository.count()).isEqualTo(3);
  }

  @Test
  void unknownTagDeletesNothing() {
    assertThat(repository.deleteByTag("rust")).isZero();
    assertThat(repository.count()).isEqualTo(3);
  }

  @Test
  void tagsAreDeletedWithTheirBookmarks() {
    repository.deleteByTag("java");
    entityManager.clear();
    assertThat(repository.findByTag("web")).isEmpty();
    Number tagRows =
        (Number)
            entityManager
                .getEntityManager()
                .createNativeQuery("select count(*) from bookmark_tag")
                .getSingleResult();
    assertThat(tagRows.longValue()).isEqualTo(1);
  }

  @Test
  void serviceCleansTagBeforeDeleting() {
    var service = new BookmarkService(repository, Clock.fixed(NOON, ZoneOffset.UTC));
    assertThat(service.deleteTagged(" Java ")).isEqualTo(2);
    entityManager.clear();
    assertThat(repository.findAll(0, 10)).containsExactly(c);
  }
}
