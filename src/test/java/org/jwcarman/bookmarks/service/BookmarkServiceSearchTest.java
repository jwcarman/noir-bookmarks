package org.jwcarman.bookmarks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.jwcarman.bookmarks.domain.Bookmark;

class BookmarkServiceSearchTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  final List<String> texts = new ArrayList<>();
  final List<Set<String>> tagSets = new ArrayList<>();
  final InMemoryBookmarkRepository repository =
      new InMemoryBookmarkRepository() {
        @Override
        public List<Bookmark> search(String text, Set<String> tags, int page, int size) {
          texts.add(text);
          tagSets.add(tags);
          return super.search(text, tags, page, size);
        }
      };

  BookmarkService serviceAt(Instant now) {
    return new BookmarkService(repository, Clock.fixed(now, ZoneOffset.UTC));
  }

  final BookmarkService service = serviceAt(NOON);

  @Test
  void cleansInputAndReportsTotal() {
    var a = service.create("https://a.example", "Java tips", List.of("java"));
    serviceAt(NOON.plusSeconds(60)).create("https://b.example", "Java news", List.of("news"));

    var result = service.search("  java ", Set.of(" JAVA ", " "), 0, 10);

    assertThat(result.items()).containsExactly(a);
    assertThat(result.total()).isEqualTo(1);
    assertThat(result.page()).isZero();
    assertThat(result.size()).isEqualTo(10);
    assertThat(texts).containsExactly("java");
    assertThat(tagSets).containsExactly(Set.of("java"));
  }

  @Test
  void pagesAndCountsAllMatches() {
    for (int i = 1; i <= 5; i++) {
      serviceAt(NOON.plusSeconds(60L * i)).create("https://" + i + ".example", "t" + i, List.of());
    }
    var result = service.search("t", Set.of(), 1, 2);
    assertThat(result.items()).extracting(Bookmark::title).containsExactly("t3", "t2");
    assertThat(result.total()).isEqualTo(5);
    assertThat(result.page()).isEqualTo(1);
    assertThat(result.size()).isEqualTo(2);
  }

  @Test
  void emptyTextAndTagsMatchAll() {
    service.create("https://a.example", "A", List.of());
    service.create("https://b.example", "B", List.of("x"));
    var result = service.search("   ", new HashSet<>(Set.of("", "  ")), 0, 10);
    assertThat(result.items()).hasSize(2);
    assertThat(result.total()).isEqualTo(2);
    assertThat(texts).containsExactly("");
    assertThat(tagSets).containsExactly(Set.of());
  }

  @Test
  void pagingOutOfRangeThrows() {
    assertThatThrownBy(() -> service.search("x", Set.of(), -1, 10))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.search("x", Set.of(), 0, 0))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.search("x", Set.of(), 0, 101))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.search("x", Set.of(), Integer.MAX_VALUE, 100))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(texts).isEmpty();
  }

  @Test
  void boundaryPagingAccepted() {
    assertThat(service.search("x", Set.of(), 0, 1).items()).isEmpty();
    assertThat(service.search("x", Set.of(), 0, 100).total()).isZero();
    assertThat(service.search("x", Set.of(), Integer.MAX_VALUE, 1).items()).isEmpty();
  }

  @Test
  void nullArgumentsThrow() {
    assertThatThrownBy(() -> service.search(null, Set.of(), 0, 10))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> service.search("x", null, 0, 10))
        .isInstanceOf(NullPointerException.class);
    assertThat(texts).isEmpty();
  }
}
