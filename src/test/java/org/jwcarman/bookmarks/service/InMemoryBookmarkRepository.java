package org.jwcarman.bookmarks.service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.TreeMap;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.BookmarkRepository;

/** A {@link BookmarkRepository} in a map, for service tests. Ids start at 1. */
class InMemoryBookmarkRepository implements BookmarkRepository {

  private static final Comparator<Bookmark> NEWEST_FIRST =
      Comparator.comparing(Bookmark::createdAt).thenComparing(Bookmark::id).reversed();

  private final TreeMap<Long, Bookmark> bookmarks = new TreeMap<>();
  private long nextId = 1;

  @Override
  public Bookmark save(Bookmark bookmark) {
    var saved = bookmark.id() == null ? bookmark.withId(nextId++) : bookmark;
    bookmarks.put(saved.id(), saved);
    return saved;
  }

  @Override
  public Optional<Bookmark> findById(long id) {
    return Optional.ofNullable(bookmarks.get(id));
  }

  @Override
  public boolean deleteById(long id) {
    return bookmarks.remove(id) != null;
  }

  @Override
  public List<Bookmark> findAll(int page, int size) {
    return bookmarks.values().stream()
        .sorted(NEWEST_FIRST)
        .skip((long) page * size)
        .limit(size)
        .toList();
  }

  @Override
  public long count() {
    return bookmarks.size();
  }
}
