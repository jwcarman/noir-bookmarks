package org.jwcarman.bookmarks.domain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * A saved link.
 *
 * @param id the id, or {@code null} until the bookmark is saved
 * @param url the link
 * @param title a human-readable title
 * @param tags the tags, in the order given
 * @param createdAt when the bookmark was created
 */
public record Bookmark(Long id, String url, String title, List<String> tags, Instant createdAt) {

  public Bookmark {
    Objects.requireNonNull(url, "url");
    Objects.requireNonNull(title, "title");
    tags = List.copyOf(Objects.requireNonNull(tags, "tags"));
    Objects.requireNonNull(createdAt, "createdAt");
  }

  /** Returns this bookmark with the given id. */
  public Bookmark withId(long id) {
    return new Bookmark(id, url, title, tags, createdAt);
  }
}
