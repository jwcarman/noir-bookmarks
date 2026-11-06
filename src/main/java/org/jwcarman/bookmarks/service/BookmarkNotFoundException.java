package org.jwcarman.bookmarks.service;

/** No bookmark has the requested id. */
public class BookmarkNotFoundException extends RuntimeException {

  private final long id;

  public BookmarkNotFoundException(long id) {
    super("Bookmark " + id + " not found");
    this.id = id;
  }

  /** Returns the id that was not found. */
  public long id() {
    return id;
  }
}
