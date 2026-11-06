package org.jwcarman.bookmarks.web;

import java.util.List;
import org.jwcarman.bookmarks.service.BookmarkPage;

/** One page of bookmarks as the API returns it. */
public record BookmarkPageResponse(List<BookmarkResponse> items, int page, int size, long total) {

  static BookmarkPageResponse from(BookmarkPage p) {
    return new BookmarkPageResponse(
        p.items().stream().map(BookmarkResponse::from).toList(), p.page(), p.size(), p.total());
  }
}
