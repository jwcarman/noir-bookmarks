package org.jwcarman.bookmarks.web;

import java.time.Instant;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;

/** A bookmark as the API returns it. */
public record BookmarkResponse(long id, String url, String title, List<String> tags, Instant createdAt) {

  static BookmarkResponse from(Bookmark b) {
    return new BookmarkResponse(b.id(), b.url(), b.title(), b.tags(), b.createdAt());
  }
}
