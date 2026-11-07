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
class JpaBookmarkRepositoryNotesTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  @Autowired JpaBookmarkRepository repository;
  @Autowired TestEntityManager entityManager;

  Bookmark save(String url, String title, int minute, String notes) {
    return repository.save(
        new Bookmark(null, url, title, List.of(), NOON.plusSeconds(60L * minute), notes));
  }

  void flushAndClear() {
    entityManager.flush();
    entityManager.clear();
  }

  @Test
  void notesAreStoredAndReadBack() {
    var saved = save("https://a.example/", "A", 0, "line 1\nline 2");
    assertThat(saved.notes()).isEqualTo("line 1\nline 2");
    flushAndClear();
    assertThat(repository.findById(saved.id()).orElseThrow().notes()).isEqualTo("line 1\nline 2");
  }

  @Test
  void maxLengthNotesAreStored() {
    var notes = "n".repeat(1999) + "!";
    var saved = save("https://a.example/", "A", 0, notes);
    flushAndClear();
    assertThat(repository.findById(saved.id()).orElseThrow().notes()).isEqualTo(notes);
  }

  @Test
  void notesAreReadByEveryFinder() {
    var saved = repository.save(
        new Bookmark(null, "https://a.example/", "A", List.of("t"), NOON, "n"));
    flushAndClear();
    assertThat(repository.findAll(0, 10)).extracting(Bookmark::notes).containsExactly("n");
    assertThat(repository.findByTag("t")).extracting(Bookmark::notes).containsExactly("n");
    assertThat(repository.findByUrl("https://a.example/")).contains(saved);
  }

  @Test
  void searchMatchesNotesIgnoringCase() {
    var a = save("https://a.example/", "Alpha", 0, "About Kotlin coroutines");
    save("https://b.example/", "Beta", 1, "");
    flushAndClear();
    assertThat(repository.search("kotlin", Set.of(), 0, 10)).containsExactly(a);
    assertThat(repository.countSearch("kotlin", Set.of())).isEqualTo(1);
    assertThat(repository.search("KOTLIN", Set.of(), 0, 10)).containsExactly(a);
  }

  @Test
  void searchStillMatchesTitleAndUrl() {
    var a = save("https://a.example/x", "Alpha", 0, "zzz");
    var b = save("https://b.example/", "Beta", 1, "");
    flushAndClear();
    assertThat(repository.search("alpha", Set.of(), 0, 10)).containsExactly(a);
    assertThat(repository.search("b.example", Set.of(), 0, 10)).containsExactly(b);
    assertThat(repository.countSearch("example", Set.of())).isEqualTo(2);
  }

  @Test
  void percentInNotesIsLiteral() {
    var sure = save("https://a.example/", "A", 0, "100% sure");
    save("https://b.example/", "B", 1, "100 percent");
    flushAndClear();
    assertThat(repository.search("100%", Set.of(), 0, 10)).containsExactly(sure);
    assertThat(repository.countSearch("100%", Set.of())).isEqualTo(1);
  }

  @Test
  void underscoreAndBangInNotesAreLiteral() {
    var under = save("https://a.example/", "A", 0, "snake_case");
    var bang = save("https://b.example/", "B", 1, "wow!");
    save("https://c.example/", "C", 2, "snakeXcase wow");
    flushAndClear();
    assertThat(repository.search("_", Set.of(), 0, 10)).containsExactly(under);
    assertThat(repository.search("!", Set.of(), 0, 10)).containsExactly(bang);
  }

  @Test
  void notesMatchCombinesWithTags() {
    var tagged = repository.save(
        new Bookmark(null, "https://a.example/", "A", List.of("k"), NOON, "kotlin"));
    save("https://b.example/", "B", 1, "kotlin");
    flushAndClear();
    assertThat(repository.search("kotlin", Set.of("k"), 0, 10)).containsExactly(tagged);
    assertThat(repository.countSearch("kotlin", Set.of("k"))).isEqualTo(1);
  }
}
