package org.jwcarman.bookmarks.domain;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

/**
 * A saved link.
 *
 * @param id the id, or {@code null} until the bookmark is saved
 * @param url the link
 * @param title a human-readable title
 * @param tags the cleaned tags: stripped, lowercased, without blanks or duplicates, at most 10
 * @param createdAt when the bookmark was created
 */
public record Bookmark(Long id, String url, String title, List<String> tags, Instant createdAt) {

  private static final int MAX_TAGS = 10;

  /**
   * Creates a bookmark. {@code url}, {@code title}, {@code tags}, every tag and {@code createdAt}
   * must be non-null. The tags are cleaned, in this order: each tag is trimmed of leading and
   * trailing whitespace ({@link String#strip()}) and lowercased with {@link java.util.Locale#ROOT};
   * empty results are dropped; a tag equal to an earlier one is dropped, so the first occurrence
   * keeps its position. The cleaned list is unmodifiable and has at most 10 tags.
   *
   * @throws NullPointerException if {@code url}, {@code title}, {@code tags}, any tag or {@code
   *     createdAt} is {@code null}
   * @throws IllegalArgumentException if more than 10 tags remain after cleaning
   */
  public Bookmark {
    Objects.requireNonNull(url, "url");
    Objects.requireNonNull(title, "title");
    tags = cleanTags(Objects.requireNonNull(tags, "tags"));
    Objects.requireNonNull(createdAt, "createdAt");
  }

  private static List<String> cleanTags(List<String> tags) {
    LinkedHashSet<String> cleaned = new LinkedHashSet<>();
    for (String tag : tags) {
      String clean = Objects.requireNonNull(tag, "tag").strip().toLowerCase(Locale.ROOT);
      if (!clean.isEmpty()) {
        cleaned.add(clean);
      }
    }
    if (cleaned.size() > MAX_TAGS) {
      throw new IllegalArgumentException(
          "at most " + MAX_TAGS + " tags are allowed, got " + cleaned.size());
    }
    return List.copyOf(cleaned);
  }

  /** Returns this bookmark with the given id. */
  public Bookmark withId(long id) {
    return new Bookmark(id, url, title, tags, createdAt);
  }
}
