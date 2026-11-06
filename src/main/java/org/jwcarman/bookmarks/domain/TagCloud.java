package org.jwcarman.bookmarks.domain;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/**
 * Tag usage counts.
 *
 * @param entries every tag used by the bookmarks with its count, most used first, ties broken by
 *     tag in ascending (natural String) order
 */
public record TagCloud(List<Entry> entries) {

  private static final Comparator<Entry> ORDER =
      Comparator.comparingInt(Entry::count).reversed().thenComparing(Entry::tag);

  /** Makes {@code entries} an unmodifiable copy. */
  public TagCloud {
    entries = List.copyOf(entries);
  }

  /** One tag and how many bookmarks have it. */
  public record Entry(String tag, int count) {}

  /**
   * Counts the tags of {@code bookmarks}.
   *
   * @throws NullPointerException if {@code bookmarks} is {@code null}
   */
  public static TagCloud of(List<Bookmark> bookmarks) {
    Objects.requireNonNull(bookmarks, "bookmarks");
    Map<String, Integer> counts = new TreeMap<>();
    for (Bookmark bookmark : bookmarks) {
      for (String tag : bookmark.tags()) {
        counts.merge(tag, 1, Integer::sum);
      }
    }
    return new TagCloud(
        counts.entrySet().stream()
            .map(e -> new Entry(e.getKey(), e.getValue()))
            .sorted(ORDER)
            .toList());
  }
}
