package org.jwcarman.bookmarks.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * The body of {@code POST /tags/{tag}/rename}.
 *
 * @param to the new tag: required, not blank, at most 50 characters
 */
public record RenameTagRequest(@NotBlank @Size(max = 50) String to) {

  public RenameTagRequest {
    // validated by Bean Validation at the web layer
  }
}
