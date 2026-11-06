package org.jwcarman.bookmarks.web;

/**
 * The result of deleting a tag's bookmarks.
 *
 * @param deleted how many bookmarks were deleted
 */
public record DeleteTagResponse(int deleted) {

  public DeleteTagResponse {
    // no normalization needed
  }
}
