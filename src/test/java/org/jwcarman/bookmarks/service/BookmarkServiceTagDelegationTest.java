package org.jwcarman.bookmarks.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.junit.jupiter.api.Test;

class BookmarkServiceTagDelegationTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  final InMemoryBookmarkRepository repository = new InMemoryBookmarkRepository();
  final BookmarkService service =
      new BookmarkService(repository, Clock.fixed(NOON, ZoneOffset.UTC));

  @Test
  void renameTagGoesThroughTagService() {
    Bookmark saved = service.create("https://a.example/", "A", List.of("java"));
    assertThat(service.renameTag("java", "jvm")).isEqualTo(1);
    assertThat(service.get(saved.id()).tags()).containsExactly("jvm");
  }

  @Test
  void listByTagAndDeleteTaggedGoThroughTagService() {
    Bookmark java = service.create("https://a.example/", "A", List.of("java"));
    service.create("https://b.example/", "B", List.of("kotlin"));
    assertThat(service.listByTag("JAVA")).containsExactly(java);
    assertThat(service.deleteTagged("kotlin")).isEqualTo(1);
    assertThat(repository.findAll(0, 10)).containsExactly(java);
  }
}
