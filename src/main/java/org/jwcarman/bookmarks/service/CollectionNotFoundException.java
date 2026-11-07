package org.jwcarman.bookmarks.service;

/** No collection has the requested id. */
public class CollectionNotFoundException extends RuntimeException {

  private final long id;

  /** Creates the exception; its message is {@code Collection <id> not found}. */
  public CollectionNotFoundException(long id) {
    super("Collection " + id + " not found");
    this.id = id;
  }

  /** Returns the id that was not found. */
  public long id() {
    return id;
  }
}
