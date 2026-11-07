package org.jwcarman.bookmarks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import java.time.Instant;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.BookmarkRepository;
import org.junit.jupiter.api.Test;

class TagServiceTest {

  static final Instant T1 = Instant.parse("2026-11-01T12:00:00Z");
  static final Instant T2 = Instant.parse("2026-11-02T12:00:00Z");

  final InMemoryBookmarkRepository repository = new InMemoryBookmarkRepository();
  final TagService service = new TagService(repository);

  Bookmark seed(String url, Instant at, String... tags) {
    return repository.save(new Bookmark(null, url, "T", List.of(tags), at));
  }

  @Test
  void constructorRejectsNullRepository() {
    assertThatThrownBy(() -> new TagService(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("repository");
  }

  @Test
  void listByTagCleansTagAndReturnsNewestFirst() {
    Bookmark a = seed("https://a.example/", T1, "java");
    Bookmark b = seed("https://b.example/", T2, "java");
    seed("https://c.example/", T2, "kotlin");
    assertThat(service.listByTag(" JAVA ")).containsExactly(b, a);
  }

  @Test
  void listByTagRejectsBlankAndNull() {
    assertThatThrownBy(() -> service.listByTag("  "))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("tag must not be blank");
    assertThatThrownBy(() -> service.listByTag(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("tag");
  }

  @Test
  void renameReplacesInPlaceAndKeepsEverythingElse() {
    Bookmark saved =
        repository.save(
            new Bookmark(null, "https://a.example/", "T", List.of("a", "java", "b"), T1, "n"));
    assertThat(service.renameTag("Java", " JVM ")).isEqualTo(1);
    Bookmark stored = repository.findById(saved.id()).orElseThrow();
    assertThat(stored.tags()).containsExactly("a", "jvm", "b");
    assertThat(stored.title()).isEqualTo("T");
    assertThat(stored.notes()).isEqualTo("n");
    assertThat(stored.id()).isEqualTo(saved.id());
    assertThat(stored.url()).isEqualTo("https://a.example/");
    assertThat(stored.createdAt()).isEqualTo(T1);
    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void renameMergesDuplicateKeepingFirstPosition() {
    Bookmark saved = seed("https://a.example/", T1, "jvm", "x", "java");
    assertThat(service.renameTag("java", "jvm")).isEqualTo(1);
    assertThat(repository.findById(saved.id()).orElseThrow().tags()).containsExactly("jvm", "x");
  }

  @Test
  void renameCountsOnlyTaggedBookmarks() {
    seed("https://a.example/", T1, "java");
    Bookmark other = seed("https://b.example/", T1, "kotlin");
    seed("https://c.example/", T2, "web", "java");
    assertThat(service.renameTag("java", "jvm")).isEqualTo(2);
    assertThat(repository.findById(other.id()).orElseThrow().tags()).containsExactly("kotlin");
    assertThat(service.listByTag("jvm")).hasSize(2);
    assertThat(service.listByTag("java")).isEmpty();
  }

  @Test
  void renameToSameCleanedTagSavesNothing() {
    BookmarkRepository mockRepo = mock(BookmarkRepository.class);
    assertThat(new TagService(mockRepo).renameTag("java", "JAVA")).isZero();
    verifyNoInteractions(mockRepo);
  }

  @Test
  void renameToSameCleanedTagLeavesStoreUnchanged() {
    Bookmark saved = seed("https://a.example/", T1, "java");
    assertThat(service.renameTag("java", " JAVA ")).isZero();
    assertThat(repository.findById(saved.id()).orElseThrow()).isEqualTo(saved);
  }

  @Test
  void renameRejectsBlankAndNull() {
    BookmarkRepository mockRepo = mock(BookmarkRepository.class);
    TagService tags = new TagService(mockRepo);
    assertThatThrownBy(() -> tags.renameTag(" ", "x"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("from must not be blank");
    assertThatThrownBy(() -> tags.renameTag("x", ""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("to must not be blank");
    assertThatThrownBy(() -> tags.renameTag(null, "x"))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("from");
    assertThatThrownBy(() -> tags.renameTag("x", null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("to");
    verify(mockRepo, never()).save(any());
    verifyNoInteractions(mockRepo);
  }

  @Test
  void deleteTaggedCleansTagAndDeletesMatches() {
    seed("https://a.example/", T1, "java");
    seed("https://b.example/", T2, "java");
    Bookmark kotlin = seed("https://c.example/", T2, "kotlin");
    assertThat(service.deleteTagged(" Java ")).isEqualTo(2);
    assertThat(repository.findAll(0, 10)).containsExactly(kotlin);
  }

  @Test
  void deleteTaggedRejectsBlankAndNull() {
    assertThatThrownBy(() -> service.deleteTagged(""))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("tag must not be blank");
    assertThatThrownBy(() -> service.deleteTagged(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("tag");
  }
}
