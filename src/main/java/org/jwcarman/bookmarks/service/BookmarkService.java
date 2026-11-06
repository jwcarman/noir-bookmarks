package org.jwcarman.bookmarks.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.BookmarkRepository;
import org.jwcarman.bookmarks.domain.UrlNormalizer;
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

  /**
   * Saves a new bookmark created now, and returns it with its id. The URL is stored in canonical
   * form, {@code UrlNormalizer.normalize(url)}; the time is truncated to microseconds.
   *
   * @throws IllegalArgumentException if {@code url} is not an absolute http or https URL (from
   *     {@code UrlNormalizer})
   * @throws DuplicateBookmarkException if a bookmark with the same canonical URL already exists;
   *     nothing is saved
   */
  public Bookmark create(String url, String title, List<String> tags) {
    String canonical = UrlNormalizer.normalize(url);
    repository
        .findByUrl(canonical)
        .ifPresent(
            existing -> {
              throw new DuplicateBookmarkException(canonical, existing.id());
            });
    var now = Instant.now(clock).truncatedTo(ChronoUnit.MICROS);
    return repository.save(new Bookmark(null, canonical, title, tags, now));
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
   * @throws IllegalArgumentException if {@code page} or {@code size} is out of range, or the page
   *     starts beyond {@link Integer#MAX_VALUE} bookmarks
   */
  public BookmarkPage list(int page, int size) {
    if (page < 0) {
      throw new IllegalArgumentException("page must be at least 0: " + page);
    }
    if (size < 1 || size > MAX_PAGE_SIZE) {
      throw new IllegalArgumentException(
          "size must be from 1 to " + MAX_PAGE_SIZE + ": " + size);
    }
    if ((long) page * size > Integer.MAX_VALUE) {
      throw new IllegalArgumentException("page " + page + " of size " + size + " is too far");
    }
    return new BookmarkPage(repository.findAll(page, size), page, size, repository.count());
  }

  /**
   * Returns the bookmarks tagged {@code tag}, newest first. The tag is cleaned the way bookmarks
   * clean their tags (stripped and lowercased with {@code Locale.ROOT}) before the lookup.
   *
   * @throws NullPointerException if {@code tag} is {@code null}
   * @throws IllegalArgumentException if {@code tag} is blank
   */
  public List<Bookmark> listByTag(String tag) {
    String clean = Objects.requireNonNull(tag, "tag").strip().toLowerCase(Locale.ROOT);
    if (clean.isEmpty()) {
      throw new IllegalArgumentException("tag must not be blank");
    }
    return repository.findByTag(clean);
  }
}
