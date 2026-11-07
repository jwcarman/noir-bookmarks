package org.jwcarman.bookmarks.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookmarkCollectionTest {

  @Test
  void stripsNameAndKeepsFirstOccurrenceOfIds() {
    var c = new BookmarkCollection(null, "  Work  ", List.of(3L, 1L, 3L, 2L));
    assertThat(c.id()).isNull();
    assertThat(c.name()).isEqualTo("Work");
    assertThat(c.bookmarkIds()).containsExactly(3L, 1L, 2L);
  }

  @Test
  void keepsId() {
    assertThat(new BookmarkCollection(5L, "x", List.of()).id()).isEqualTo(5L);
  }

  @Test
  void rejectsBlankName() {
    assertThatThrownBy(() -> new BookmarkCollection(null, "   ", List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> new BookmarkCollection(null, "", List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsNameLongerThan100() {
    assertThatThrownBy(() -> new BookmarkCollection(null, "a".repeat(101), List.of()))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void accepts100CharacterNameAfterStripping() {
    assertThat(new BookmarkCollection(null, " " + "a".repeat(100) + " ", List.of()).name())
        .hasSize(100);
    assertThat(new BookmarkCollection(null, "a", List.of()).name()).isEqualTo("a");
  }

  @Test
  void rejectsNulls() {
    assertThatThrownBy(() -> new BookmarkCollection(null, null, List.of()))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new BookmarkCollection(null, "x", null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new BookmarkCollection(null, "x", Arrays.asList(1L, null)))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void idsAreUnmodifiable() {
    var c = new BookmarkCollection(null, "x", List.of(1L));
    assertThatThrownBy(() -> c.bookmarkIds().add(9L))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
