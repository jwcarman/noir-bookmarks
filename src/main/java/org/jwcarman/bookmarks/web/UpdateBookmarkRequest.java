package org.jwcarman.bookmarks.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/**
 * The body of {@code PUT /bookmarks/{id}}. Missing {@code tags} mean no tags.
 *
 * @param title the new title: required, not blank, at most 200 characters
 * @param tags the new tags: at most 50 entries, each not blank and at most 50 characters
 */
public record UpdateBookmarkRequest(
    @NotBlank @Size(max = 200) String title,
    @Size(max = 50) List<@NotBlank @Size(max = 50) String> tags) {

  public UpdateBookmarkRequest {
    tags = tags == null ? List.of() : tags;
  }
}
