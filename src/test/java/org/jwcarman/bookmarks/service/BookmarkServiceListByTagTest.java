package org.jwcarman.bookmarks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.jwcarman.bookmarks.domain.Bookmark;

class BookmarkServiceListByTagTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  final List<String> lookups = new ArrayList<>();
  final InMemoryBookmarkRepository repository =
      new InMemoryBookmarkRepository() {
        @Override
        public List<Bookmark> findByTag(String tag) {
          lookups.add(tag);
          return super.findByTag(tag);
        }
      };

  BookmarkService serviceAt(Instant now) {
    return new BookmarkService(repository, Clock.fixed(now, ZoneOffset.UTC));
  }

  final BookmarkService service = serviceAt(NOON);

  @Test
  void cleansTagBeforeLookup() {
    var a = service.create("https://a.example", "A", List.of("java"));
    service.create("https://b.example", "B", List.of("kotlin"));

    assertThat(service.listByTag("  JAVA ")).containsExactly(a);
    assertThat(lookups).containsExactly("java");
  }

  @Test
  void unknownTagIsEmpty() {
    service.create("https://a.example", "A", List.of("java"));
    assertThat(service.listByTag("go")).isEmpty();
    assertThat(lookups).containsExactly("go");
  }

  @Test
  void returnsNewestFirst() {
    var older = serviceAt(NOON).create("https://1.example", "1", List.of("java"));
    var newer = serviceAt(NOON.plusSeconds(60)).create("https://2.example", "2", List.of("java"));
    assertThat(service.listByTag("java")).containsExactly(newer, older);
  }

  @Test
  void blankTagThrows() {
    assertThatThrownBy(() -> service.listByTag("   "))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.listByTag("")).isInstanceOf(IllegalArgumentException.class);
    assertThat(lookups).isEmpty();
  }

  @Test
  void nullTagThrows() {
    assertThatThrownBy(() -> service.listByTag(null)).isInstanceOf(NullPointerException.class);
    assertThat(lookups).isEmpty();
  }
}
