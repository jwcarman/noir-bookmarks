package org.jwcarman.bookmarks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.junit.jupiter.api.Test;

class BookmarkServiceTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  final InMemoryBookmarkRepository repository = new InMemoryBookmarkRepository();

  BookmarkService serviceAt(Instant now) {
    return new BookmarkService(repository, Clock.fixed(now, ZoneOffset.UTC));
  }

  final BookmarkService service = serviceAt(NOON);

  @Test
  void createStampsTimeAndAssignsId() {
    var b = service.create("https://a.example", "A", List.of("x"));
    assertThat(b.id()).isEqualTo(1L);
    assertThat(b.createdAt()).isEqualTo(NOON);
    assertThat(b.tags()).containsExactly("x");
    assertThat(service.get(1)).isEqualTo(b);
  }

  @Test
  void getMissingThrows() {
    assertThatThrownBy(() -> service.get(99))
        .isInstanceOfSatisfying(BookmarkNotFoundException.class, e -> assertThat(e.id()).isEqualTo(99))
        .hasMessage("Bookmark 99 not found");
  }

  @Test
  void deleteRemovesAndMissingThrows() {
    var b = service.create("https://a.example", "A", List.of());
    service.delete(b.id());
    assertThatThrownBy(() -> service.get(b.id())).isInstanceOf(BookmarkNotFoundException.class);
    assertThatThrownBy(() -> service.delete(b.id())).isInstanceOf(BookmarkNotFoundException.class);
  }

  @Test
  void listPagesNewestFirst() {
    var first = serviceAt(NOON).create("https://1.example", "1", List.of());
    var second = serviceAt(NOON.plus(Duration.ofMinutes(1))).create("https://2.example", "2", List.of());
    var third = serviceAt(NOON.plus(Duration.ofMinutes(2))).create("https://3.example", "3", List.of());

    var page0 = service.list(0, 2);
    assertThat(page0.items()).containsExactly(third, second);
    assertThat(page0.total()).isEqualTo(3);
    assertThat(page0.page()).isZero();
    assertThat(page0.size()).isEqualTo(2);
    assertThat(service.list(1, 2).items()).containsExactly(first);
  }

  @Test
  void listPastTheEndIsEmpty() {
    for (int i = 0; i < 3; i++) {
      service.create("https://" + i + ".example", "t", List.of());
    }
    var page = service.list(5, 10);
    assertThat(page.items()).isEmpty();
    assertThat(page.total()).isEqualTo(3);
  }

  @Test
  void listOrdersByCreatedAtThenId() {
    var a = service.create("https://a.example", "A", List.of());
    var b = service.create("https://b.example", "B", List.of());
    assertThat(service.list(0, 10).items()).containsExactly(b, a);
  }

  @Test
  void listRejectsBadPaging() {
    assertThatThrownBy(() -> service.list(-1, 10)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.list(0, 0)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.list(0, 101)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void bookmarkRejectsNullsAndCopiesTags() {
    var tags = new java.util.ArrayList<>(List.of("x"));
    var b = new Bookmark(null, "https://a.example", "A", tags, NOON);
    tags.add("y");
    assertThat(b.tags()).containsExactly("x");
    assertThatThrownBy(() -> new Bookmark(null, null, "A", List.of(), NOON))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Bookmark(null, "u", "A", null, NOON))
        .isInstanceOf(NullPointerException.class);
  }
}
