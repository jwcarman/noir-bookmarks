package org.jwcarman.bookmarks.service;

import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.BookmarkRepository;
import org.springframework.stereotype.Service;

/** Creates, reads, deletes and lists bookmarks. */
@Service
public class BookmarkService {

  /** The largest page size {@link #list} accepts. */
  public static final int MAX_PAGE_SIZE = 100;

  private final BookmarkRepository repository;
  private final Clock clock;

  public BookmarkService(BookmarkRepository repository, Clock clock) {
    this.repository = Objects.requireNonNull(repository, "repository");
    this.clock = Objects.requireNonNull(clock, "clock");
  }

  /** Saves a new bookmark created now, and returns it with its id. */
  public Bookmark create(String url, String title, List<String> tags) {
    return repository.save(new Bookmark(null, url, title, tags, Instant.now(clock)));
  }

  /**
   * Returns the bookmark with {@code id}.
   *
   * @throws BookmarkNotFoundException if there is none
   */
  public Bookmark get(long id) {
    return repository.findById(id).orElseThrow(() -> new BookmarkNotFoundException(id));
  }

  /**
   * Deletes the bookmark with {@code id}.
   *
   * @throws BookmarkNotFoundException if there is none
   */
  public void delete(long id) {
    if (!repository.deleteById(id)) {
      throw new BookmarkNotFoundException(id);
    }
  }

  /**
   * Returns one page of bookmarks, newest first.
   *
   * @param page the zero-based page number, at least 0
   * @param size the page size, from 1 to {@value #MAX_PAGE_SIZE}
   * @throws IllegalArgumentException if {@code page} or {@code size} is out of range
   */
  public BookmarkPage list(int page, int size) {
    if (page < 0) {
      throw new IllegalArgumentException("page must be at least 0: " + page);
    }
    if (size < 1 || size > MAX_PAGE_SIZE) {
      throw new IllegalArgumentException(
          "size must be from 1 to " + MAX_PAGE_SIZE + ": " + size);
    }
    return new BookmarkPage(repository.findAll(page, size), page, size, repository.count());
  }
}
