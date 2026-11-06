package org.jwcarman.bookmarks.domain;

import java.util.List;
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
}
