package org.jwcarman.bookmarks.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
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
  private final EntityManager entityManager;

  JpaBookmarkRepository(SpringDataBookmarks bookmarks, EntityManager entityManager) {
    this.bookmarks = bookmarks;
    this.entityManager = entityManager;
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

  /** Finds through {@code SpringDataBookmarks.findByTagsContainingOrderByCreatedAtDescIdDesc}. */
  @Override
  @Transactional(readOnly = true)
  public List<Bookmark> findByTag(String tag) {
    return bookmarks.findByTagsContainingOrderByCreatedAtDescIdDesc(tag).stream()
        .map(BookmarkEntity::toBookmark)
        .toList();
  }

  /** Deletes with JPA; the bookmarks' tags are deleted with them. */
  @Override
  public int deleteByTag(String tag) {
    List<BookmarkEntity> tagged = bookmarks.findByTagsContainingOrderByCreatedAtDescIdDesc(tag);
    bookmarks.deleteAll(tagged);
    bookmarks.flush();
    return tagged.size();
  }

  @Override
  @Transactional(readOnly = true)
  public long count() {
    return bookmarks.count();
  }

  /** Searches with a database query; does not load every bookmark into memory. */
  @Override
  @Transactional(readOnly = true)
  public List<Bookmark> search(String text, Set<String> tags, int page, int size) {
    var filter = new SearchFilter(text, tags);
    TypedQuery<BookmarkEntity> query =
        entityManager.createQuery(
            "select b from BookmarkEntity b"
                + filter.where()
                + " order by b.createdAt desc, b.id desc",
            BookmarkEntity.class);
    filter.bind(query);
    query.setFirstResult(Math.multiplyExact(page, size));
    query.setMaxResults(size);
    return query.getResultList().stream().map(BookmarkEntity::toBookmark).toList();
  }

  /** Counts with a database query. */
  @Override
  @Transactional(readOnly = true)
  public long countSearch(String text, Set<String> tags) {
    var filter = new SearchFilter(text, tags);
    TypedQuery<Long> query =
        entityManager.createQuery(
            "select count(b) from BookmarkEntity b" + filter.where(), Long.class);
    filter.bind(query);
    return query.getSingleResult();
  }

  /** The where clause and parameters of a search. */
  private static final class SearchFilter {

    private final String pattern;
    private final List<String> tags;

    SearchFilter(String text, Set<String> tags) {
      this.pattern = text.isEmpty() ? null : "%" + escape(text.toLowerCase(Locale.ROOT)) + "%";
      this.tags = new ArrayList<>(tags);
    }

    private static String escape(String text) {
      return text.replace("!", "!!").replace("%", "!%").replace("_", "!_");
    }

    String where() {
      List<String> conditions = new ArrayList<>();
      if (pattern != null) {
        conditions.add(
            "(lower(b.title) like :pattern escape '!' or lower(b.url) like :pattern escape '!')");
      }
      for (int i = 0; i < tags.size(); i++) {
        conditions.add(":tag" + i + " member of b.tags");
      }
      return conditions.isEmpty() ? "" : " where " + String.join(" and ", conditions);
    }

    void bind(TypedQuery<?> query) {
      if (pattern != null) {
        query.setParameter("pattern", pattern);
      }
      for (int i = 0; i < tags.size(); i++) {
        query.setParameter("tag" + i, tags.get(i));
      }
    }
  }
}
