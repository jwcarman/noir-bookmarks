package org.jwcarman.bookmarks.service;

/** A bookmark with the same canonical URL already exists. */
public class DuplicateBookmarkException extends RuntimeException {

  private final String url;
  private final long existingId;

  /**
   * Creates the exception. Its message is {@code URL <url> is already saved as bookmark
   * <existingId>}.
   *
   * @param url the canonical URL
   * @param existingId the id of the bookmark that already has it
   */
  public DuplicateBookmarkException(String url, long existingId) {
    super("URL " + url + " is already saved as bookmark " + existingId);
    this.url = url;
    this.existingId = existingId;
  }

  /** Returns the canonical URL. */
  public String url() {
    return url;
  }

  /** Returns the id of the bookmark that already has the URL. */
  public long existingId() {
    return existingId;
  }
}
