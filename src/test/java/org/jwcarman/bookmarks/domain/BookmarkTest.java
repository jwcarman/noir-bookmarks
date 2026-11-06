package org.jwcarman.bookmarks.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.IntStream;
import org.junit.jupiter.api.Test;

class BookmarkTest {

  private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

  private static Bookmark bookmark(List<String> tags) {
    return new Bookmark(null, "https://a.example", "A", tags, NOW);
  }

  private static List<String> range(int from, int toExclusive) {
    return new ArrayList<>(IntStream.range(from, toExclusive).mapToObj(i -> "t" + i).toList());
  }

  @Test
  void trimsAndLowercasesTags() {
    assertThat(bookmark(List.of(" Java ", "SPRING")).tags()).containsExactly("java", "spring");
    assertThat(bookmark(List.of("\tKotlin\n")).tags()).containsExactly("kotlin");
  }

  @Test
  void lowercasesWithRootLocale() {
    assertThat(bookmark(List.of("TITLE")).tags()).containsExactly("title");
  }

  @Test
  void dropsBlankTags() {
    assertThat(bookmark(List.of("a", "", "  ", "b")).tags()).containsExactly("a", "b");
    assertThat(bookmark(List.of(" ")).tags()).isEmpty();
    assertThat(bookmark(List.of()).tags()).isEmpty();
  }

  @Test
  void dropsDuplicatesKeepingFirstPosition() {
    assertThat(bookmark(List.of("Java", "spring", " java", "JAVA", "Spring")).tags())
        .containsExactly("java", "spring");
    assertThat(bookmark(List.of("b", "a", "B")).tags()).containsExactly("b", "a");
  }

  @Test
  void rejectsMoreThanTenTags() {
    List<String> tags = range(0, 11);
    assertThatThrownBy(() -> bookmark(tags)).isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void allowsTenTagsAfterCleaning() {
    List<String> tags = range(0, 10);
    tags.add("T9");
    tags.add(" ");
    assertThat(bookmark(tags).tags()).containsExactlyElementsOf(range(0, 10));
    assertThat(bookmark(range(0, 10)).tags()).hasSize(10);
  }

  @Test
  void tagsAreUnmodifiable() {
    List<String> tags = bookmark(List.of("Java")).tags();
    assertThatThrownBy(() -> tags.add("x")).isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void tagsAreCopied() {
    List<String> source = new ArrayList<>(List.of("a"));
    Bookmark b = bookmark(source);
    source.add("b");
    assertThat(b.tags()).containsExactly("a");
  }

  @Test
  void rejectsNulls() {
    List<String> withNull = Arrays.asList("a", null);
    assertThatThrownBy(() -> bookmark(withNull)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> bookmark(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Bookmark(null, null, "A", List.of(), NOW))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Bookmark(null, "u", null, List.of(), NOW))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> new Bookmark(null, "u", "A", List.of(), null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void withIdKeepsOtherComponents() {
    Bookmark b = new Bookmark(null, "https://a.example", "A", List.of("Java"), NOW).withId(7L);
    assertThat(b).isEqualTo(new Bookmark(7L, "https://a.example", "A", List.of("java"), NOW));
  }
}
