package org.jwcarman.bookmarks.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.junit.jupiter.api.Test;

class NotesRequestResponseTest {

  @Test
  void createRequestDefaults() {
    var r = new CreateBookmarkRequest("u", "t", null, null);
    assertThat(r.tags()).isEmpty();
    assertThat(r.notes()).isEmpty();
    assertThat(new CreateBookmarkRequest("u", "t", List.of("a")).notes()).isEmpty();
    var full = new CreateBookmarkRequest("u", "t", List.of("a"), "n");
    assertThat(full.notes()).isEqualTo("n");
    assertThat(full.tags()).containsExactly("a");
    assertThat(full.url()).isEqualTo("u");
  }

  @Test
  void updateRequestDefaults() {
    var r = new UpdateBookmarkRequest("t", null, null);
    assertThat(r.tags()).isEmpty();
    assertThat(r.notes()).isEmpty();
    assertThat(new UpdateBookmarkRequest("t", List.of("a")).notes()).isEmpty();
    assertThat(new UpdateBookmarkRequest("t", List.of(), "n").notes()).isEqualTo("n");
  }

  @Test
  void responseCopiesNotes() {
    var b = new Bookmark(3L, "https://a.example/", "A", List.of("x"), Instant.EPOCH, "n");
    assertThat(BookmarkResponse.from(b))
        .isEqualTo(
            new BookmarkResponse(3L, "https://a.example/", "A", List.of("x"), Instant.EPOCH, "n"));
    assertThat(new BookmarkResponse(3L, "u", "t", List.of(), Instant.EPOCH).notes()).isEmpty();
  }
}
