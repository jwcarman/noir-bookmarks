package org.jwcarman.bookmarks.persistence;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import java.util.ArrayList;
import java.util.List;

/**
 * The JPA form of a collection. Members live in the {@code collection_bookmark} table, which
 * references the bookmark table and keeps their order in a {@code position} column.
 */
@Entity
@Table(name = "bookmark_collection")
class CollectionEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false, length = 100)
  private String name;

  @ManyToMany
  @JoinTable(
      name = "collection_bookmark",
      joinColumns = @JoinColumn(name = "collection_id"),
      inverseJoinColumns = @JoinColumn(name = "bookmark_id"))
  @OrderColumn(name = "position")
  private List<BookmarkEntity> bookmarks = new ArrayList<>();

  protected CollectionEntity() {}

  CollectionEntity(String name, List<BookmarkEntity> bookmarks) {
    this.name = name;
    this.bookmarks = bookmarks;
  }

  Long id() {
    return id;
  }

  String name() {
    return name;
  }

  List<BookmarkEntity> bookmarks() {
    return bookmarks;
  }

  void rename(String name) {
    this.name = name;
  }
}
