package org.jwcarman.bookmarks.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

/** The body of {@code POST /bookmarks}. Missing {@code tags} mean no tags. */
public record CreateBookmarkRequest(
    @NotBlank @Size(max = 2048) String url,
    @NotBlank @Size(max = 200) String title,
    List<@NotBlank String> tags) {

  public CreateBookmarkRequest {
    tags = tags == null ? List.of() : tags;
  }
}
