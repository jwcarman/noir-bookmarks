package org.jwcarman.bookmarks.persistence;

import org.springframework.data.jpa.repository.JpaRepository;

/** Spring Data access to {@link BookmarkEntity}; used only by {@link JpaBookmarkRepository}. */
interface SpringDataBookmarks extends JpaRepository<BookmarkEntity, Long> {}
