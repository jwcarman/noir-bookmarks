package org.jwcarman.bookmarks.domain;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;

/**
 * A named, ordered group of bookmarks.
 *
 * @param id the id, or {@code null} until the collection is saved
 * @param name the name: stripped, 1 to 100 characters
 * @param bookmarkIds the ids of the member bookmarks, in the order they were added, without
 *     duplicates; unmodifiable
 */
public record BookmarkCollection(Long id, String name, List<Long> bookmarkIds) {

  private static final int MAX_NAME_LENGTH = 100;

  /**
   * Creates a collection. {@code name} is stripped ({@link String#strip()}); {@code bookmarkIds} is
   * copied, keeping the first occurrence of a repeated id.
   *
   * @throws NullPointerException if {@code name}, {@code bookmarkIds} or any id is {@code null}
   * @throws IllegalArgumentException if the stripped name is empty or longer than 100 characters
   */
  public BookmarkCollection {
    name = cleanName(name);
    bookmarkIds = List.copyOf(new LinkedHashSet<>(List.copyOf(bookmarkIds)));
  }

  private static String cleanName(String name) {
    String stripped = Objects.requireNonNull(name, "name").strip();
    if (stripped.isEmpty()) {
      throw new IllegalArgumentException("name must not be blank");
    }
    if (stripped.length() > MAX_NAME_LENGTH) {
      throw new IllegalArgumentException(
          "name must have at most " + MAX_NAME_LENGTH + " characters, got " + stripped.length());
    }
    return stripped;
  }
}
