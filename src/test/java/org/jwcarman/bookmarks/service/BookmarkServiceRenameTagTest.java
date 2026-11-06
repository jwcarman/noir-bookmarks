package org.jwcarman.bookmarks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.BookmarkRepository;
import org.junit.jupiter.api.Test;

class BookmarkServiceRenameTagTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");
  static final Clock CLOCK = Clock.fixed(NOON, ZoneOffset.UTC);

  final InMemoryBookmarkRepository repository = new InMemoryBookmarkRepository();
  final BookmarkService service = new BookmarkService(repository, CLOCK);

  void seedThree() {
    repository.save(new Bookmark(1L, "https://a.example", "A", List.of("js", "web"), NOON));
    repository.save(new Bookmark(2L, "https://b.example", "B", List.of("java"), NOON));
    repository.save(new Bookmark(3L, "https://c.example", "C", List.of("web", "js"), NOON));
  }

  @Test
  void renamesInPlaceOnEveryTaggedBookmark() {
    seedThree();
    assertThat(service.renameTag("js", "javascript")).isEqualTo(2);
    assertThat(service.get(1).tags()).containsExactly("javascript", "web");
    assertThat(service.get(2).tags()).containsExactly("java");
    assertThat(service.get(3).tags()).containsExactly("web", "javascript");
    assertThat(repository.count()).isEqualTo(3);
  }

  @Test
  void cleansBothTags() {
    seedThree();
    assertThat(service.renameTag(" JS ", "JavaScript")).isEqualTo(2);
    assertThat(service.get(1).tags()).containsExactly("javascript", "web");
    assertThat(service.get(3).tags()).containsExactly("web", "javascript");
  }

  @Test
  void keepsIdUrlTitleAndCreationTime() {
    repository.save(new Bookmark(1L, "https://a.example", "A", List.of("js"), NOON));
    service.renameTag("js", "javascript");
    assertThat(service.get(1))
        .isEqualTo(new Bookmark(1L, "https://a.example", "A", List.of("javascript"), NOON));
  }

  @Test
  void mergeDropsLaterDuplicate() {
    repository.save(
        new Bookmark(1L, "https://a.example", "A", List.of("javascript", "web", "js"), NOON));
    assertThat(service.renameTag("js", "javascript")).isEqualTo(1);
    assertThat(service.get(1).tags()).containsExactly("javascript", "web");
  }

  @Test
  void mergeKeepsFirstOccurrencePosition() {
    repository.save(
        new Bookmark(1L, "https://a.example", "A", List.of("js", "web", "javascript"), NOON));
    assertThat(service.renameTag("js", "javascript")).isEqualTo(1);
    assertThat(service.get(1).tags()).containsExactly("javascript", "web");
  }

  @Test
  void returnsZeroWhenNoBookmarkHasTheTag() {
    seedThree();
    assertThat(service.renameTag("go", "golang")).isZero();
    assertThat(service.get(1).tags()).containsExactly("js", "web");
  }

  @Test
  void equalCleanedTagsSaveNothing() {
    BookmarkRepository mockRepo = mock(BookmarkRepository.class);
    var svc = new BookmarkService(mockRepo, CLOCK);
    assertThat(svc.renameTag("js", " JS ")).isZero();
    verify(mockRepo, never()).save(any());
    verifyNoInteractions(mockRepo);
  }

  @Test
  void equalCleanedTagsLeaveBookmarksAlone() {
    repository.save(new Bookmark(1L, "https://a.example", "A", List.of("js"), NOON));
    assertThat(service.renameTag("js", " JS ")).isZero();
    assertThat(service.get(1).tags()).containsExactly("js");
  }

  @Test
  void blankTagsAreRejected() {
    assertThatThrownBy(() -> service.renameTag(" ", "x"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("from");
    assertThatThrownBy(() -> service.renameTag("x", ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("to");
  }

  @Test
  void nullTagsAreRejected() {
    assertThatThrownBy(() -> service.renameTag(null, "x"))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> service.renameTag("x", null))
        .isInstanceOf(NullPointerException.class);
  }
}
