package org.jwcarman.bookmarks.web;

import java.util.List;
import org.jwcarman.bookmarks.service.CursorPage;

/**
 * A cursor page as the API returns it: {@code {"items":[...],"next":"..."}}, with {@code "next":
 * null} on the last page.
 */
public record CursorPageResponse(List<BookmarkResponse> items, String next) {

  /** Converts {@code page}, every item with {@code BookmarkResponse.from}. */
  static CursorPageResponse from(CursorPage page) {
    return new CursorPageResponse(
        page.items().stream().map(BookmarkResponse::from).toList(), page.next());
  }
}
