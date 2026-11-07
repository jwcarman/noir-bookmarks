package org.jwcarman.bookmarks.domain;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/** Where bookmarks are stored. */
public interface BookmarkRepository {

  /** Saves {@code bookmark}, assigning an id when it has none, and returns the saved bookmark. */
  Bookmark save(Bookmark bookmark);

  /** Returns the bookmark with {@code id}, if there is one. */
  Optional<Bookmark> findById(long id);

  /** Deletes the bookmark with {@code id}; returns whether there was one. */
  boolean deleteById(long id);

  /**
   * Returns one page of bookmarks, newest first ({@code createdAt} descending, then {@code id}
   * descending).
   *
   * @param page the zero-based page number
   * @param size the page size
   */
  List<Bookmark> findAll(int page, int size);

  /**
   * Deletes every bookmark that has {@code tag} among its tags (exact, case-sensitive match) and
   * returns how many were deleted.
   */
  int deleteByTag(String tag);

  /** Returns how many bookmarks are stored. */
  long count();

  /**
   * Returns every bookmark that has {@code tag} among its tags (exact, case-sensitive match),
   * newest first: {@code createdAt} descending, then {@code id} descending. Empty when none has it.
   */
  List<Bookmark> findByTag(String tag);

  /**
   * Returns one page of the bookmarks that match: {@code text} occurs in the title, the URL or the
   * notes, ignoring case (an empty {@code text} matches every bookmark), and the bookmark has every
   * tag in {@code tags} (an empty set matches every bookmark). Newest first: {@code createdAt}
   * descending, then {@code id} descending.
   *
   * @param page the zero-based page number
   * @param size the page size
   */
  List<Bookmark> search(String text, Set<String> tags, int page, int size);

  /** Returns how many bookmarks {@link #search} matches for {@code text} and {@code tags}. */
  long countSearch(String text, Set<String> tags);

  /**
   * Returns at most {@code limit} bookmarks in the newest-first order ({@code createdAt}
   * descending, then {@code id} descending) that come strictly after {@code after} in that order:
   * {@code createdAt} earlier than {@code after.createdAt()}, or equal with an {@code id} lower than
   * {@code after.id()}. From the newest bookmark when {@code after} is {@code null}. The cursor's
   * bookmark need not exist any more.
   */
  List<Bookmark> findAfter(Cursor after, int limit);

  /**
   * Like {@link #findAfter}, over only the bookmarks {@link #search} matches for {@code text} and
   * {@code tags}.
   */
  List<Bookmark> searchAfter(String text, Set<String> tags, Cursor after, int limit);

  /**
   * Returns the bookmark whose {@code url} equals {@code url} exactly (case-sensitive), if there is
   * one. This default looks through {@link #findAll(int, int)} pages of 100, from page 0, until it
   * finds a match or gets a page with fewer than 100 bookmarks; when several match it returns the
   * first one found.
   *
   * @throws NullPointerException if {@code url} is {@code null}
   */
  default Optional<Bookmark> findByUrl(String url) {
    Objects.requireNonNull(url, "url");
    final int pageSize = 100;
    for (int page = 0; ; page++) {
      List<Bookmark> bookmarks = findAll(page, pageSize);
      Optional<Bookmark> match = bookmarks.stream().filter(b -> url.equals(b.url())).findFirst();
      if (match.isPresent() || bookmarks.size() < pageSize) {
        return match;
      }
    }
  }
}
