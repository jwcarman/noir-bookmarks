package org.jwcarman.bookmarks.web;

import java.util.List;
import org.jwcarman.bookmarks.service.CollectionView;

/**
 * A collection as the API returns it.
 *
 * @param id the collection's id
 * @param name the collection's name
 * @param bookmarks the members, each as in {@code GET /bookmarks/{id}}
 */
record CollectionResponse(long id, String name, List<BookmarkResponse> bookmarks) {

  static CollectionResponse from(CollectionView view) {
    return new CollectionResponse(
        view.id(), view.name(), view.bookmarks().stream().map(BookmarkResponse::from).toList());
  }
}
