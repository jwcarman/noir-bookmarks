package org.jwcarman.bookmarks.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** The body of {@code POST /bookmarks}. Missing {@code tags} mean no tags; a tag has at most 50 characters. */
public record CreateBookmarkRequest(
    @NotBlank @Size(max = 2048) String url,
    @NotBlank @Size(max = 200) String title,
    @Size(max = 50) List<@NotBlank @Size(max = 50) String> tags) {

  public CreateBookmarkRequest {
    tags = tags == null ? List.of() : tags;
  }
}
