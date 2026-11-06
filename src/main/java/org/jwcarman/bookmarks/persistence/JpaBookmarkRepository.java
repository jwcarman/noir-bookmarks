package org.jwcarman.bookmarks.persistence;

import java.util.List;
import java.util.Optional;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.BookmarkRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/** A {@link BookmarkRepository} backed by JPA. */
@Repository
@Transactional
public class JpaBookmarkRepository implements BookmarkRepository {

  private static final Sort NEWEST_FIRST = Sort.by(Sort.Direction.DESC, "createdAt", "id");

  private final SpringDataBookmarks bookmarks;

  JpaBookmarkRepository(SpringDataBookmarks bookmarks) {
    this.bookmarks = bookmarks;
  }

  @Override
  public Bookmark save(Bookmark bookmark) {
    return bookmarks.save(BookmarkEntity.from(bookmark)).toBookmark();
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<Bookmark> findById(long id) {
    return bookmarks.findById(id).map(BookmarkEntity::toBookmark);
  }

  @Override
  public boolean deleteById(long id) {
    if (!bookmarks.existsById(id)) {
      return false;
    }
    bookmarks.deleteById(id);
    return true;
  }

  @Override
  @Transactional(readOnly = true)
  public List<Bookmark> findAll(int page, int size) {
    return bookmarks.findAll(PageRequest.of(page, size, NEWEST_FIRST)).stream()
        .map(BookmarkEntity::toBookmark)
        .toList();
  }

  @Override
  @Transactional(readOnly = true)
  public long count() {
    return bookmarks.count();
  }
}
