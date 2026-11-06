package org.jwcarman.bookmarks.persistence;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;

/** The JPA form of a {@link Bookmark}. */
@Entity
@Table(name = "bookmark")
class BookmarkEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 2048)
  private String url;

  @Column(nullable = false, length = 200)
  private String title;

  @ElementCollection(fetch = FetchType.EAGER)
  @CollectionTable(name = "bookmark_tag", joinColumns = @JoinColumn(name = "bookmark_id"))
  @OrderColumn(name = "position")
  @Column(name = "tag", nullable = false)
  private List<String> tags = new ArrayList<>();

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  protected BookmarkEntity() {}

  static BookmarkEntity from(Bookmark bookmark) {
    var entity = new BookmarkEntity();
    entity.id = bookmark.id();
    entity.url = bookmark.url();
    entity.title = bookmark.title();
    entity.tags = new ArrayList<>(bookmark.tags());
    entity.createdAt = bookmark.createdAt();
    return entity;
  }

  Bookmark toBookmark() {
    return new Bookmark(id, url, title, tags, createdAt);
  }
}
