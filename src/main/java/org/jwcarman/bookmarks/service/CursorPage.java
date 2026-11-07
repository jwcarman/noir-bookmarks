package org.jwcarman.bookmarks.service;

import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;

/**
 * One page of bookmarks in the newest-first order.
 *
 * @param items the bookmarks on this page, newest first
 * @param next the encoded cursor of the last item when more bookmarks follow it, or {@code null}
 *     when this is the last page
 */
public record CursorPage(List<Bookmark> items, String next) {}
