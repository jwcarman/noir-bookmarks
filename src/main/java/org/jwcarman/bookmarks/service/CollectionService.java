package org.jwcarman.bookmarks.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.BookmarkCollection;
import org.jwcarman.bookmarks.domain.BookmarkRepository;
import org.jwcarman.bookmarks.domain.CollectionRepository;
import org.springframework.stereotype.Service;

/** Creates, renames and deletes collections and changes their members. */
@Service
public class CollectionService {

  private final CollectionRepository collections;
  private final BookmarkRepository bookmarks;

  /** Creates the service. */
  public CollectionService(CollectionRepository collections, BookmarkRepository bookmarks) {
    this.collections = collections;
    this.bookmarks = bookmarks;
  }

  /**
   * Creates an empty collection named {@code name} (stripped) and returns it.
   *
   * @throws NullPointerException if {@code name} is {@code null}
   * @throws IllegalArgumentException if the stripped name is empty or longer than 100 characters
   * @throws DuplicateCollectionException if a collection with that name, ignoring case, exists;
   *     nothing is saved
   */
  public CollectionView create(String name) {
    var collection = new BookmarkCollection(null, name, List.of());
    checkUnique(collection);
    return view(collections.save(collection));
  }

  /**
   * Returns the collection with {@code id} and its members, in the order they were added.
   *
   * @throws CollectionNotFoundException if there is none
   */
  public CollectionView get(long id) {
    return view(find(id));
  }

  /** Returns every collection, by name ignoring case, then by id. */
  public List<CollectionSummary> list() {
    return collections.findAll().stream()
        .map(c -> new CollectionSummary(c.id(), c.name(), c.bookmarkIds().size()))
        .toList();
  }

  /**
   * Renames the collection with {@code id} to {@code name} (stripped) and returns it. Renaming to
   * its own name with a different case is allowed.
   *
   * @throws NullPointerException if {@code name} is {@code null}
   * @throws IllegalArgumentException if the stripped name is empty or longer than 100 characters
   * @throws CollectionNotFoundException if there is no collection with {@code id}
   * @throws DuplicateCollectionException if another collection has that name, ignoring case
   */
  public CollectionView rename(long id, String name) {
    var renamed = new BookmarkCollection(id, name, find(id).bookmarkIds());
    checkUnique(renamed);
    return view(collections.save(renamed));
  }

  /**
   * Deletes the collection with {@code id}; its bookmarks are not deleted.
   *
   * @throws CollectionNotFoundException if there is none
   */
  public void delete(long id) {
    if (!collections.deleteById(id)) {
      throw new CollectionNotFoundException(id);
    }
  }

  /**
   * Adds the bookmark {@code bookmarkId} at the end of the collection {@code id} and returns the
   * collection. Adding a bookmark that is already a member changes nothing (it keeps its place).
   *
   * @throws CollectionNotFoundException if there is no collection with {@code id}
   * @throws BookmarkNotFoundException if there is no bookmark with {@code bookmarkId}
   */
  public CollectionView add(long id, long bookmarkId) {
    var collection = find(id);
    if (bookmarks.findById(bookmarkId).isEmpty()) {
      throw new BookmarkNotFoundException(bookmarkId);
    }
    if (collection.bookmarkIds().contains(bookmarkId)) {
      return view(collection);
    }
    List<Long> ids = new ArrayList<>(collection.bookmarkIds());
    ids.add(bookmarkId);
    return view(collections.save(new BookmarkCollection(id, collection.name(), ids)));
  }

  /**
   * Removes the bookmark {@code bookmarkId} from the collection {@code id} and returns the
   * collection; removing a bookmark that is not a member changes nothing. The bookmark itself is
   * not deleted.
   *
   * @throws CollectionNotFoundException if there is no collection with {@code id}
   */
  public CollectionView remove(long id, long bookmarkId) {
    var collection = find(id);
    if (!collection.bookmarkIds().contains(bookmarkId)) {
      return view(collection);
    }
    List<Long> ids = new ArrayList<>(collection.bookmarkIds());
    ids.remove(Long.valueOf(bookmarkId));
    return view(collections.save(new BookmarkCollection(id, collection.name(), ids)));
  }

  private BookmarkCollection find(long id) {
    return collections.findById(id).orElseThrow(() -> new CollectionNotFoundException(id));
  }

  private void checkUnique(BookmarkCollection collection) {
    Optional<BookmarkCollection> clash = collections.findByName(collection.name());
    if (clash.isPresent() && !clash.get().id().equals(collection.id())) {
      throw new DuplicateCollectionException(collection.name());
    }
  }

  private CollectionView view(BookmarkCollection collection) {
    List<Bookmark> members =
        collection.bookmarkIds().stream()
            .flatMap(bookmarkId -> bookmarks.findById(bookmarkId).stream())
            .toList();
    return new CollectionView(collection.id(), collection.name(), members);
  }
}
