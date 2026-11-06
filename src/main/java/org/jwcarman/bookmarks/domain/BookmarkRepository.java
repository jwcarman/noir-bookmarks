package org.jwcarman.bookmarks.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/** Where bookmarks are stored. */
public interface BookmarkRepository {

  /** Saves {@code bookmark}, assigning an id when it has none, and returns the saved bookmark. */
  Bookmark save(Bookmark bookmark);

  /** Returns the bookmark with {@code id}, if there is one. */
  Optional<Bookmark> findById(long id);

  /** Deletes the bookmark with {@code id}; returns whether there was one. */
  boolean deleteById(long id);

  /**
   * Returns one page of bookmarks, newest first ({@code createdAt} descending, then {@code id}
   * descending).
   *
   * @param page the zero-based page number
   * @param size the page size
   */
  List<Bookmark> findAll(int page, int size);

  /** Returns how many bookmarks are stored. */
  long count();

  /**
   * Returns the bookmark whose {@code url} equals {@code url} exactly (case-sensitive), if there is
   * one. This default looks through {@link #findAll(int, int)} pages of 100, from page 0, until it
   * finds a match or gets a page with fewer than 100 bookmarks; when several match it returns the
   * first one found.
   *
   * @throws NullPointerException if {@code url} is {@code null}
   */
  default Optional<Bookmark> findByUrl(String url) {
    Objects.requireNonNull(url, "url");
    final int pageSize = 100;
    for (int page = 0; ; page++) {
      List<Bookmark> bookmarks = findAll(page, pageSize);
      Optional<Bookmark> match = bookmarks.stream().filter(b -> url.equals(b.url())).findFirst();
      if (match.isPresent() || bookmarks.size() < pageSize) {
        return match;
      }
    }
  }
}
