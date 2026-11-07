package org.jwcarman.bookmarks.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.BookmarkRepository;
import org.jwcarman.bookmarks.domain.Cursor;

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
  public List<Bookmark> findByTag(String tag) {
    return bookmarks.values().stream()
        .filter(b -> b.tags().contains(tag))
        .sorted(NEWEST_FIRST)
        .toList();
  }

  @Override
  public int deleteByTag(String tag) {
    int before = bookmarks.size();
    bookmarks.values().removeIf(b -> b.tags().contains(tag));
    return before - bookmarks.size();
  }

  @Override
  public long count() {
    return bookmarks.size();
  }

  @Override
  public List<Bookmark> search(String text, Set<String> tags, int page, int size) {
    return bookmarks.values().stream()
        .filter(b -> matches(b, text, tags))
        .sorted(NEWEST_FIRST)
        .skip((long) page * size)
        .limit(size)
        .toList();
  }

  @Override
  public long countSearch(String text, Set<String> tags) {
    return bookmarks.values().stream().filter(b -> matches(b, text, tags)).count();
  }

  @Override
  public List<Bookmark> findAfter(Cursor after, int limit) {
    return searchAfter("", Set.of(), after, limit);
  }

  @Override
  public List<Bookmark> searchAfter(String text, Set<String> tags, Cursor after, int limit) {
    return bookmarks.values().stream()
        .filter(b -> matches(b, text, tags))
        .filter(b -> isAfter(b, after))
        .sorted(NEWEST_FIRST)
        .limit(limit)
        .toList();
  }

  private static boolean isAfter(Bookmark b, Cursor after) {
    if (after == null) {
      return true;
    }
    int byTime = b.createdAt().compareTo(after.createdAt());
    return byTime < 0 || (byTime == 0 && b.id() < after.id());
  }

  private static boolean matches(Bookmark b, String text, Set<String> tags) {
    String needle = text.toLowerCase(Locale.ROOT);
    return (b.title().toLowerCase(Locale.ROOT).contains(needle)
            || b.url().toLowerCase(Locale.ROOT).contains(needle)
            || b.notes().toLowerCase(Locale.ROOT).contains(needle))
        && b.tags().containsAll(tags);
  }
}
