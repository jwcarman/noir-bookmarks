package org.jwcarman.bookmarks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.junit.jupiter.api.Test;

class BookmarkServiceNotesTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  final InMemoryBookmarkRepository repository = new InMemoryBookmarkRepository();
  final BookmarkService service =
      new BookmarkService(repository, Clock.fixed(NOON, ZoneOffset.UTC));

  @Test
  void createSavesNotes() {
    var b = service.create("https://a.example/", "A", List.of(), "why");
    assertThat(b.notes()).isEqualTo("why");
    assertThat(b.createdAt()).isEqualTo(NOON);
    assertThat(repository.findById(b.id()).orElseThrow().notes()).isEqualTo("why");
  }

  @Test
  void createWithNotesNormalizesUrl() {
    var b = service.create("HTTPS://A.example", "A", List.of("X"), "n");
    assertThat(b.url()).isEqualTo("https://a.example/");
    assertThat(b.tags()).containsExactly("x");
  }

  @Test
  void createWithoutNotesGivesEmptyNotes() {
    var b = service.create("https://a.example/", "A", List.of());
    assertThat(repository.findById(b.id()).orElseThrow().notes()).isEmpty();
  }

  @Test
  void createWithNullNotesThrowsAndSavesNothing() {
    assertThatThrownBy(() -> service.create("https://a.example/", "A", List.of(), null))
        .isInstanceOf(NullPointerException.class);
    assertThat(repository.count()).isZero();
  }

  @Test
  void createWithNullNotesThrowsEvenForDuplicates() {
    service.create("https://a.example/", "A", List.of());
    assertThatThrownBy(() -> service.create("https://a.example/", "A", List.of(), null))
        .isInstanceOf(NullPointerException.class);
    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void createWithTooLongNotesThrowsAndSavesNothing() {
    assertThatThrownBy(
            () -> service.create("https://a.example/", "A", List.of(), "x".repeat(2001)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(repository.count()).isZero();
  }

  @Test
  void createWithNotesRejectsDuplicates() {
    service.create("https://a.example/", "A", List.of(), "one");
    assertThatThrownBy(() -> service.create("https://a.example", "B", List.of(), "two"))
        .isInstanceOf(DuplicateBookmarkException.class);
    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void updateWithNotesReplacesThem() {
    var saved = service.create("https://a.example/", "A", List.of("y"), "old");
    var result = service.update(saved.id(), "T", List.of("x"), "new");
    var expected = new Bookmark(saved.id(), "https://a.example/", "T", List.of("x"), NOON, "new");
    assertThat(result).isEqualTo(expected);
    assertThat(repository.findById(saved.id())).contains(expected);
  }

  @Test
  void updateWithoutNotesKeepsThem() {
    var saved = service.create("https://a.example/", "A", List.of(), "keep");
    var result = service.update(saved.id(), "T", List.of());
    assertThat(result.notes()).isEqualTo("keep");
    assertThat(result.title()).isEqualTo("T");
    assertThat(repository.findById(saved.id()).orElseThrow().notes()).isEqualTo("keep");
  }

  @Test
  void updateWithTooLongNotesThrowsAndChangesNothing() {
    var saved = service.create("https://a.example/", "A", List.of(), "old");
    assertThatThrownBy(() -> service.update(saved.id(), "T", List.of(), "x".repeat(2001)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(repository.findById(saved.id())).contains(saved);
  }

  @Test
  void updateWithNotesRejectsNulls() {
    var saved = service.create("https://a.example/", "A", List.of(), "old");
    long id = saved.id();
    assertThatThrownBy(() -> service.update(id, "T", List.of(), null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("notes");
    assertThatThrownBy(() -> service.update(id, null, List.of(), "n"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("title");
    assertThatThrownBy(() -> service.update(id, "T", null, "n"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("tags");
    assertThat(repository.findById(id)).contains(saved);
  }

  @Test
  void updateWithNotesOfMissingBookmarkThrows() {
    assertThatThrownBy(() -> service.update(99, "T", List.of(), "n"))
        .isInstanceOf(BookmarkNotFoundException.class);
    assertThat(repository.count()).isZero();
  }

  @Test
  void renameTagKeepsNotes() {
    var saved = service.create("https://a.example/", "A", List.of("java"), "keep");
    assertThat(service.renameTag("java", "jvm")).isEqualTo(1);
    var stored = repository.findById(saved.id()).orElseThrow();
    assertThat(stored.tags()).containsExactly("jvm");
    assertThat(stored.notes()).isEqualTo("keep");
  }

  @Test
  void searchMatchesNotesIgnoringCase() {
    var a = service.create("https://a.example/", "Alpha", List.of(), "About Kotlin coroutines");
    service.create("https://b.example/", "Beta", List.of(), "");
    var page = service.search("KOTLIN", Set.of(), 0, 10);
    assertThat(page.items()).containsExactly(a);
    assertThat(page.total()).isEqualTo(1);
  }
}
