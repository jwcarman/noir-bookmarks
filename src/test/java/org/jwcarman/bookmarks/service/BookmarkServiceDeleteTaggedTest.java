package org.jwcarman.bookmarks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.BookmarkRepository;
import org.junit.jupiter.api.Test;

class BookmarkServiceDeleteTaggedTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");
  static final Clock CLOCK = Clock.fixed(NOON, ZoneOffset.UTC);

  final InMemoryBookmarkRepository repository = new InMemoryBookmarkRepository();
  final BookmarkService service = new BookmarkService(repository, CLOCK);

  @Test
  void cleansTagAndDeletesTaggedBookmarks() {
    repository.save(new Bookmark(1L, "https://a.example", "A", List.of("java"), NOON));
    repository.save(new Bookmark(2L, "https://b.example", "B", List.of("web", "java"), NOON));
    var go = repository.save(new Bookmark(3L, "https://c.example", "C", List.of("go"), NOON));
    assertThat(service.deleteTagged(" Java ")).isEqualTo(2);
    assertThat(service.all()).containsExactly(go);
  }

  @Test
  void returnsZeroWhenNothingTagged() {
    repository.save(new Bookmark(1L, "https://a.example", "A", List.of("go"), NOON));
    assertThat(service.deleteTagged("java")).isZero();
    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void passesCleanedTagAndReturnsRepositoryCount() {
    BookmarkRepository mockRepo = mock(BookmarkRepository.class);
    when(mockRepo.deleteByTag("java")).thenReturn(7);
    var svc = new BookmarkService(mockRepo, CLOCK);
    assertThat(svc.deleteTagged("\tJAVA ")).isEqualTo(7);
    verify(mockRepo).deleteByTag("java");
  }

  @Test
  void blankTagIsRejected() {
    BookmarkRepository mockRepo = mock(BookmarkRepository.class);
    var svc = new BookmarkService(mockRepo, CLOCK);
    assertThatThrownBy(() -> svc.deleteTagged("  ")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> svc.deleteTagged("")).isInstanceOf(IllegalArgumentException.class);
    verifyNoInteractions(mockRepo);
  }

  @Test
  void nullTagIsRejected() {
    assertThatThrownBy(() -> service.deleteTagged(null))
        .isInstanceOf(NullPointerException.class);
  }
}
