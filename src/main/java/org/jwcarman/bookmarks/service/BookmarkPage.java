package org.jwcarman.bookmarks.service;

import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;

/**
 * One page of bookmarks.
 *
 * @param items the bookmarks on this page, newest first
 * @param page the zero-based page number
 * @param size the requested page size
 * @param total how many bookmarks there are in all
 */
public record BookmarkPage(List<Bookmark> items, int page, int size, long total) {

  public BookmarkPage {
    items = List.copyOf(items);
  }
}
