package org.jwcarman.bookmarks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BookmarkServiceUpdateTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  final InMemoryBookmarkRepository repository = new InMemoryBookmarkRepository();
  final BookmarkService service =
      new BookmarkService(repository, Clock.fixed(NOON.plusSeconds(3600), ZoneOffset.UTC));

  @BeforeEach
  void seed() {
    repository.save(new Bookmark(7L, "https://a.example", "Old", List.of("java"), NOON));
  }

  @Test
  void updateReplacesTitleAndCleansTags() {
    var updated = service.update(7, "New", List.of(" Spring ", "JAVA", "spring"));
    assertThat(updated).isEqualTo(
        new Bookmark(7L, "https://a.example", "New", List.of("spring", "java"), NOON));
    assertThat(updated.tags()).containsExactly("spring", "java");
    assertThat(service.get(7)).isEqualTo(updated);
    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void updateWithNoTagsClearsTags() {
    var updated = service.update(7, "New", List.of());
    assertThat(updated.tags()).isEmpty();
    assertThat(service.get(7).tags()).isEmpty();
    assertThat(service.get(7).title()).isEqualTo("New");
  }

  @Test
  void updateMissingThrows() {
    assertThatThrownBy(() -> service.update(99, "x", List.of()))
        .isInstanceOfSatisfying(BookmarkNotFoundException.class, e -> assertThat(e.id()).isEqualTo(99));
  }

  @Test
  void updateRejectsNulls() {
    assertThatThrownBy(() -> service.update(7, null, List.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> service.update(7, "x", null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> service.update(99, null, List.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> service.update(99, "x", null))
        .isInstanceOf(NullPointerException.class);
    assertThat(service.get(7).title()).isEqualTo("Old");
  }
}
