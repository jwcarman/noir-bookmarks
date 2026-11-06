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
 * @param notes free-text notes, stored as given: never {@code null}, at most 2000 characters
 */
public record Bookmark(
    Long id, String url, String title, List<String> tags, Instant createdAt, String notes) {

  /** The largest number of characters {@link #notes()} may have. */
  public static final int MAX_NOTES_LENGTH = 2000;

  private static final int MAX_TAGS = 10;

  /**
   * Creates a bookmark. {@code url}, {@code title}, {@code tags}, every tag and {@code createdAt}
   * must be non-null. The tags are cleaned, in this order: each tag is trimmed of leading and
   * trailing whitespace ({@link String#strip()}) and lowercased with {@link java.util.Locale#ROOT};
   * empty results are dropped; a tag equal to an earlier one is dropped, so the first occurrence
   * keeps its position. The cleaned list is unmodifiable and has at most 10 tags. {@code notes}
   * must be non-null and at most 2000 characters; it is stored exactly as given (not stripped).
   *
   * @throws NullPointerException if {@code url}, {@code title}, {@code tags}, any tag, {@code
   *     createdAt} or {@code notes} is {@code null}
   * @throws IllegalArgumentException if more than 10 tags remain after cleaning, or {@code notes}
   *     is longer than 2000 characters
   */
  public Bookmark {
    Objects.requireNonNull(url, "url");
    Objects.requireNonNull(title, "title");
    tags = cleanTags(Objects.requireNonNull(tags, "tags"));
    Objects.requireNonNull(createdAt, "createdAt");
    checkNotes(notes);
  }

  /** Creates a bookmark with empty notes. */
  public Bookmark(Long id, String url, String title, List<String> tags, Instant createdAt) {
    this(id, url, title, tags, createdAt, "");
  }

  private static void checkNotes(String notes) {
    Objects.requireNonNull(notes, "notes");
    if (notes.length() > MAX_NOTES_LENGTH) {
      throw new IllegalArgumentException(
          "notes must have at most " + MAX_NOTES_LENGTH + " characters, got " + notes.length());
    }
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

  /** Returns this bookmark with the given id; every other component, notes included, is kept. */
  public Bookmark withId(long id) {
    return new Bookmark(id, url, title, tags, createdAt, notes);
  }
}
