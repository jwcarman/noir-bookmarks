package org.jwcarman.bookmarks.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(JpaBookmarkRepository.class)
class JpaBookmarkRepositoryTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  @Autowired JpaBookmarkRepository repository;

  Bookmark save(String url, Instant at) {
    return repository.save(new Bookmark(null, url, "title", List.of(), at));
  }

  @Test
  void saveAssignsIdAndFindReturnsIt() {
    var saved = repository.save(new Bookmark(null, "https://a.example", "A", List.of("x"), NOON));
    assertThat(saved.id()).isNotNull();
    assertThat(repository.findById(saved.id())).contains(saved);
    assertThat(repository.findById(saved.id() + 1000)).isEmpty();
  }

  @Test
  void tagsKeepTheirOrder() {
    var saved =
        repository.save(new Bookmark(null, "https://a.example", "A", List.of("b", "a", "c"), NOON));
    assertThat(repository.findById(saved.id()).orElseThrow().tags()).containsExactly("b", "a", "c");
  }

  @Test
  void deleteByIdReportsWhetherItExisted() {
    var saved = save("https://a.example", NOON);
    assertThat(repository.deleteById(saved.id())).isTrue();
    assertThat(repository.deleteById(saved.id())).isFalse();
    assertThat(repository.findById(saved.id())).isEmpty();
  }

  @Test
  void findAllPagesNewestFirstThenHigherId() {
    var older = save("https://1.example", NOON);
    var sameA = save("https://2.example", NOON.plusSeconds(60));
    var sameB = save("https://3.example", NOON.plusSeconds(60));

    assertThat(repository.findAll(0, 2)).containsExactly(sameB, sameA);
    assertThat(repository.findAll(1, 2)).containsExactly(older);
    assertThat(repository.findAll(2, 2)).isEmpty();
    assertThat(repository.count()).isEqualTo(3);
  }
}
