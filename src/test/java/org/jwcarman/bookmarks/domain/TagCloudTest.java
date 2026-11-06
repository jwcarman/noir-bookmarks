package org.jwcarman.bookmarks.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class TagCloudTest {

  private static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

  private static Bookmark bookmark(String... tags) {
    return new Bookmark(null, "https://a.example", "A", Arrays.asList(tags), NOW);
  }

  @Test
  void countsTagsMostUsedFirst() {
    TagCloud cloud =
        TagCloud.of(
            List.of(bookmark("java", "spring"), bookmark("java"), bookmark("kotlin", "spring")));
    assertThat(cloud.entries())
        .containsExactly(
            new TagCloud.Entry("java", 2),
            new TagCloud.Entry("spring", 2),
            new TagCloud.Entry("kotlin", 1));
  }

  @Test
  void breaksTiesByTagAscending() {
    TagCloud cloud = TagCloud.of(List.of(bookmark("b"), bookmark("a"), bookmark("c", "a")));
    assertThat(cloud.entries())
        .containsExactly(
            new TagCloud.Entry("a", 2), new TagCloud.Entry("b", 1), new TagCloud.Entry("c", 1));
  }

  @Test
  void higherCountWinsOverAlphabeticalOrder() {
    TagCloud cloud =
        TagCloud.of(List.of(bookmark("z", "a"), bookmark("z"), bookmark("z", "m"), bookmark("m")));
    assertThat(cloud.entries())
        .containsExactly(
            new TagCloud.Entry("z", 3), new TagCloud.Entry("m", 2), new TagCloud.Entry("a", 1));
  }

  @Test
  void emptyListGivesEmptyEntries() {
    assertThat(TagCloud.of(List.of()).entries()).isEmpty();
  }

  @Test
  void bookmarksWithoutTagsGiveEmptyEntries() {
    assertThat(TagCloud.of(List.of(bookmark(), bookmark())).entries()).isEmpty();
  }

  @Test
  void nullBookmarksThrow() {
    assertThatThrownBy(() -> TagCloud.of(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void entriesAreUnmodifiable() {
    TagCloud cloud = TagCloud.of(List.of(bookmark("java")));
    assertThatThrownBy(() -> cloud.entries().add(new TagCloud.Entry("x", 1)))
        .isInstanceOf(UnsupportedOperationException.class);
    TagCloud empty = TagCloud.of(List.of());
    assertThatThrownBy(() -> empty.entries().add(new TagCloud.Entry("x", 1)))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void constructorCopiesEntries() {
    List<TagCloud.Entry> source = new ArrayList<>(List.of(new TagCloud.Entry("a", 1)));
    TagCloud cloud = new TagCloud(source);
    source.add(new TagCloud.Entry("b", 1));
    assertThat(cloud.entries()).containsExactly(new TagCloud.Entry("a", 1));
    assertThatThrownBy(() -> cloud.entries().add(new TagCloud.Entry("x", 1)))
        .isInstanceOf(UnsupportedOperationException.class);
  }
}
