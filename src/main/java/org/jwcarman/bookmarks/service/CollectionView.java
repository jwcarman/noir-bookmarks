package org.jwcarman.bookmarks.service;

import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;

/**
 * A collection with its member bookmarks.
 *
 * @param id the collection's id
 * @param name the collection's name
 * @param bookmarks the members, in the order they were added
 */
public record CollectionView(long id, String name, List<Bookmark> bookmarks) {}
