package org.jwcarman.bookmarks.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaBookmarkRepository.class)
class JpaBookmarkRepositorySearchTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  @Autowired JpaBookmarkRepository repository;
  @Autowired TestEntityManager entityManager;

  Bookmark save(String title, String url, int minute, String... tags) {
    return repository.save(
        new Bookmark(null, url, title, List.of(tags), NOON.plusSeconds(60L * minute)));
  }

  void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void textMatchesTitleOrUrlIgnoringCase() {
    var a = save("Spring Boot guide", "https://spring.io/guides", 0, "java");
    var b = save("Kotlin notes", "https://kotlinlang.org/docs", 1, "kotlin");
    var c = save("Coffee", "https://example.com/SPRING-beans", 2);
    flushAndClear();

    assertThat(repository.search("spring", Set.of(), 0, 10)).containsExactly(c, a);
    assertThat(repository.countSearch("spring", Set.of())).isEqualTo(2);
    assertThat(repository.search("SPRING", Set.of(), 0, 10)).containsExactly(c, a);
    assertThat(repository.search("notes", Set.of(), 0, 10)).containsExactly(b);
    assertThat(repository.search("", Set.of(), 0, 10)).containsExactly(c, b, a);
    assertThat(repository.countSearch("", Set.of())).isEqualTo(3);
    assertThat(repository.search("nothing", Set.of(), 0, 10)).isEmpty();
    assertThat(repository.countSearch("nothing", Set.of())).isZero();
  }

  @Test
  void wildcardCharactersAreLiteral() {
    var pct = save("100% pure", "https://a.example", 0);
    var under = save("snake_case", "https://b.example", 1);
    save("plain", "https://c.example", 2);
    var bang = save("wow!", "https://d.example", 3);
    flushAndClear();

    assertThat(repository.search("%", Set.of(), 0, 10)).containsExactly(pct);
    assertThat(repository.search("_", Set.of(), 0, 10)).containsExactly(under);
    assertThat(repository.search("!", Set.of(), 0, 10)).containsExactly(bang);
    assertThat(repository.countSearch("%", Set.of())).isEqualTo(1);
  }

  @Test
  void everyTagMustBePresent() {
    var a = save("A", "https://a.example", 0, "java", "spring");
    var b = save("B", "https://b.example", 1, "java");
    var c = save("C", "https://c.example", 2, "spring");
    flushAndClear();

    assertThat(repository.search("", Set.of("java", "spring"), 0, 10)).containsExactly(a);
    assertThat(repository.countSearch("", Set.of("java", "spring"))).isEqualTo(1);
    assertThat(repository.search("", Set.of("java"), 0, 10)).containsExactly(b, a);
    assertThat(repository.countSearch("", Set.of("java"))).isEqualTo(2);
    assertThat(repository.search("", Set.of("spring"), 0, 10)).containsExactly(c, a);
    assertThat(repository.search("", Set.of("go"), 0, 10)).isEmpty();
    assertThat(repository.countSearch("", Set.of("go"))).isZero();
  }

  @Test
  void textAndTagsCombine() {
    save("Java tips", "https://a.example", 0, "java");
    var b = save("Java news", "https://b.example", 1, "news", "java");
    save("Other news", "https://c.example", 2, "news");
    flushAndClear();

    assertThat(repository.search("java", Set.of("news"), 0, 10)).containsExactly(b);
    assertThat(repository.countSearch("java", Set.of("news"))).isEqualTo(1);
  }

  @Test
  void pagedNewestFirst() {
    for (int i = 1; i <= 5; i++) {
      save("t" + i, "https://" + i + ".example", i);
    }
    flushAndClear();

    assertThat(repository.search("t", Set.of(), 1, 2))
        .extracting(Bookmark::title)
        .containsExactly("t3", "t2");
    assertThat(repository.search("t", Set.of(), 0, 2))
        .extracting(Bookmark::title)
        .containsExactly("t5", "t4");
    assertThat(repository.search("t", Set.of(), 2, 2))
        .extracting(Bookmark::title)
        .containsExactly("t1");
    assertThat(repository.search("t", Set.of(), 3, 2)).isEmpty();
    assertThat(repository.countSearch("t", Set.of())).isEqualTo(5);
  }

  @Test
  void sameInstantHigherIdFirst() {
    var first = save("x", "https://1.example", 0);
    var second = save("x", "https://2.example", 0);
    flushAndClear();

    assertThat(repository.search("x", Set.of(), 0, 10)).containsExactly(second, first);
  }
}
