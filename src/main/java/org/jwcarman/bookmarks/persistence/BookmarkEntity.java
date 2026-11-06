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

/** The JPA form of a {@link Bookmark}; has a {@code notes} column (not null, length 2000). */
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
  @Column(name = "tag", nullable = false, length = 50)
  private List<String> tags = new ArrayList<>();

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(nullable = false, length = Bookmark.MAX_NOTES_LENGTH)
  private String notes;

  protected BookmarkEntity() {}

  /** Copies every component of {@code bookmark}, notes included. */
  static BookmarkEntity from(Bookmark bookmark) {
    var entity = new BookmarkEntity();
    entity.id = bookmark.id();
    entity.url = bookmark.url();
    entity.title = bookmark.title();
    entity.tags = new ArrayList<>(bookmark.tags());
    entity.createdAt = bookmark.createdAt();
    entity.notes = bookmark.notes();
    return entity;
  }

  /** Returns the bookmark this entity holds, notes included. */
  Bookmark toBookmark() {
    return new Bookmark(id, url, title, tags, createdAt, notes);
  }
}
