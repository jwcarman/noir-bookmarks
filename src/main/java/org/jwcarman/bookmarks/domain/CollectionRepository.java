package org.jwcarman.bookmarks.domain;

import java.util.List;
import java.util.Optional;

/** Where collections are stored. */
public interface CollectionRepository {

  /**
   * Saves {@code collection} (name and members), assigning an id when it has none, and returns the
   * saved collection.
   */
  BookmarkCollection save(BookmarkCollection collection);

  /** Returns the collection with {@code id}, if there is one. */
  Optional<BookmarkCollection> findById(long id);

  /**
   * Returns the collection whose name equals {@code name} ignoring case ({@code Locale.ROOT}), if
   * there is one.
   */
  Optional<BookmarkCollection> findByName(String name);

  /** Returns every collection, by name ignoring case, then by id. */
  List<BookmarkCollection> findAll();

  /** Deletes the collection with {@code id} (never its bookmarks); returns whether there was one. */
  boolean deleteById(long id);
}
