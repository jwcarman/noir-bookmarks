package org.jwcarman.bookmarks.persistence;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to {@link BookmarkEntity}; used only by {@link JpaBookmarkRepository}. */
interface SpringDataBookmarks extends JpaRepository<BookmarkEntity, Long> {

  /** The entities whose tags contain {@code tag}, newest first, then higher id first. */
  List<BookmarkEntity> findByTagsContainingOrderByCreatedAtDescIdDesc(String tag);
}
