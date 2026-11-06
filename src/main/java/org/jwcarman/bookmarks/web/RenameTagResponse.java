package org.jwcarman.bookmarks.web;

/**
 * The result of a tag rename.
 *
 * @param renamed how many bookmarks changed
 */
public record RenameTagResponse(int renamed) {

  public RenameTagResponse {
    // no normalization needed
  }
}
