package org.jwcarman.bookmarks.service;

/** Another collection already has the name, ignoring case. */
public class DuplicateCollectionException extends RuntimeException {

  /**
   * Creates the exception; its message is {@code A collection named <name> already exists}, with
   * {@code name} as requested (stripped).
   */
  public DuplicateCollectionException(String name) {
    super("A collection named " + name + " already exists");
  }
}
