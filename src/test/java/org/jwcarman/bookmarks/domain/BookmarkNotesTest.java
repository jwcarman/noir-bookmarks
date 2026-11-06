package org.jwcarman.bookmarks.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookmarkNotesTest {

  static Bookmark withNotes(String notes) {
    return new Bookmark(null, "https://a.example/", "A", List.of(), Instant.EPOCH, notes);
  }

  @Test
  void notesAreStoredAsGiven() {
    assertThat(withNotes("  read later  ").notes()).isEqualTo("  read later  ");
  }

  @Test
  void notesOfMaxLengthAreAccepted() {
    var notes = "n".repeat(2000);
    assertThat(withNotes(notes).notes()).isEqualTo(notes);
    assertThat(Bookmark.MAX_NOTES_LENGTH).isEqualTo(2000);
  }

  @Test
  void tooLongNotesAreRejected() {
    assertThatThrownBy(() -> withNotes("n".repeat(2001)))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("2000")
        .hasMessageContaining("2001");
  }

  @Test
  void nullNotesAreRejected() {
    assertThatThrownBy(() -> withNotes(null))
        .isInstanceOf(NullPointerException.class)
        .hasMessage("notes");
  }

  @Test
  void emptyNotesAreAccepted() {
    assertThat(withNotes("").notes()).isEmpty();
  }

  @Test
  void withIdKeepsNotes() {
    var b = withNotes("n").withId(5);
    assertThat(b.id()).isEqualTo(5L);
    assertThat(b.notes()).isEqualTo("n");
    assertThat(b.url()).isEqualTo("https://a.example/");
  }

  @Test
  void fiveArgumentConstructorGivesEmptyNotes() {
    var b = new Bookmark(1L, "https://a.example/", "A", List.of(" X "), Instant.EPOCH);
    assertThat(b.notes()).isEmpty();
    assertThat(b.tags()).containsExactly("x");
    assertThat(b).isEqualTo(new Bookmark(1L, "https://a.example/", "A", List.of("x"), Instant.EPOCH, ""));
  }
}
