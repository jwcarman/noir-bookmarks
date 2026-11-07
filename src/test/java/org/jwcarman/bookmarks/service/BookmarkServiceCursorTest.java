package org.jwcarman.bookmarks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.Cursor;

class BookmarkServiceCursorTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  final List<Integer> limits = new ArrayList<>();
  final List<String> texts = new ArrayList<>();
  final List<Set<String>> tagSets = new ArrayList<>();
  final InMemoryBookmarkRepository repository =
      new InMemoryBookmarkRepository() {
        // findAfter delegates to searchAfter, so recording here covers both
        @Override
        public List<Bookmark> searchAfter(
            String text, Set<String> tags, Cursor after, int limit) {
          texts.add(text);
          tagSets.add(tags);
          limits.add(limit);
          return super.searchAfter(text, tags, after, limit);
        }
      };

  final BookmarkService service = new BookmarkService(repository, Clock.systemUTC());

  Bookmark create(int minute, String title, String... tags) {
    return new BookmarkService(
            repository, Clock.fixed(NOON.plusSeconds(60L * minute), ZoneOffset.UTC))
        .create("https://example.com/" + minute, title, List.of(tags));
  }

  List<Bookmark> createNumbered(int n) {
    List<Bookmark> created = new ArrayList<>();
    for (int i = 1; i <= n; i++) {
      created.add(create(i, "B" + i));
    }
    return created;
  }

  @Test
  void pagesThroughAllWithNextCursor() {
    var b = createNumbered(5);

    var p1 = service.listAfter(null, 2);
    assertThat(p1.items()).containsExactly(b.get(4), b.get(3));
    assertThat(p1.next()).isEqualTo(Cursor.of(b.get(3)).encode());

    var p2 = service.listAfter(p1.next(), 2);
    assertThat(p2.items()).containsExactly(b.get(2), b.get(1));
    assertThat(p2.next()).isEqualTo(Cursor.of(b.get(1)).encode());

    var p3 = service.listAfter(p2.next(), 2);
    assertThat(p3.items()).containsExactly(b.get(0));
    assertThat(p3.next()).isNull();
    assertThat(limits).containsOnly(3);
  }

  @Test
  void exactlyFullLastPageHasNoNext() {
    var b = createNumbered(2);
    var page = service.listAfter(null, 2);
    assertThat(page.items()).containsExactly(b.get(1), b.get(0));
    assertThat(page.next()).isNull();
  }

  @Test
  void limitOfOne() {
    var b = createNumbered(2);
    var page = service.listAfter(null, 1);
    assertThat(page.items()).containsExactly(b.get(1));
    assertThat(page.next()).isEqualTo(Cursor.of(b.get(1)).encode());
  }

  @Test
  void emptyStoreGivesEmptyPage() {
    var page = service.listAfter(null, 20);
    assertThat(page.items()).isEmpty();
    assertThat(page.next()).isNull();
  }

  @Test
  void itemsAreUnmodifiable() {
    createNumbered(3);
    assertThatThrownBy(() -> service.listAfter(null, 2).items().clear())
        .isInstanceOf(UnsupportedOperationException.class);
    assertThatThrownBy(() -> service.listAfter(null, 5).items().clear())
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void rejectsBadLimitsAndCursors() {
    assertThatThrownBy(() -> service.listAfter(null, 0))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("limit must be from 1 to 100: 0");
    assertThatThrownBy(() -> service.listAfter(null, 101))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.listAfter("!!", 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(service.listAfter(null, 100).items()).isEmpty();
    assertThat(service.listAfter(null, 1).items()).isEmpty();
    assertThat(limits).containsExactly(101, 2);
  }

  @Test
  void pageDoesNotShiftWhenANewerBookmarkIsAdded() {
    var b = createNumbered(4);
    var p1 = service.listAfter(null, 2);
    assertThat(p1.items()).containsExactly(b.get(3), b.get(2));

    create(10, "B5");

    assertThat(service.listAfter(p1.next(), 2).items()).containsExactly(b.get(1), b.get(0));
  }

  @Test
  void pageDoesNotShiftWhenTheCursorBookmarkIsDeleted() {
    var b = createNumbered(4);
    var p1 = service.listAfter(null, 2);

    service.delete(b.get(2).id());

    assertThat(service.listAfter(p1.next(), 2).items()).containsExactly(b.get(1), b.get(0));
  }

  @Test
  void searchAfterCleansInputsAndPages() {
    var java1 = create(1, "Java 1", "x");
    var java2 = create(2, "Java 2");
    create(3, "Kotlin");

    var p1 = service.searchAfter(" JAVA ", Set.of(), null, 1);
    assertThat(p1.items()).containsExactly(java2);
    assertThat(p1.next()).isNotNull();

    var p2 = service.searchAfter(" JAVA ", Set.of(), p1.next(), 1);
    assertThat(p2.items()).containsExactly(java1);
    assertThat(p2.next()).isNull();

    assertThat(service.searchAfter("java", Set.of(" X ", " "), null, 10).items())
        .containsExactly(java1);
    assertThat(texts).containsExactly("JAVA", "JAVA", "java");
    assertThat(tagSets.get(2)).isEqualTo(Set.of("x"));
    assertThat(limits).containsExactly(2, 2, 11);
  }

  @Test
  void searchAfterRejectsBadInput() {
    assertThatThrownBy(() -> service.searchAfter(null, Set.of(), null, 10))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> service.searchAfter("", null, null, 10))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> service.searchAfter("", Set.of(), null, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.searchAfter("", Set.of(), null, 101))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.searchAfter("", Set.of(), "!!", 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(service.searchAfter("", Set.of(), null, 100).next()).isNull();
  }
}
