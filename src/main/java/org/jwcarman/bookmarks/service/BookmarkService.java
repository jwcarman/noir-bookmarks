package org.jwcarman.bookmarks.service;

import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
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
   * Replaces the title and tags of the bookmark with {@code id} and returns the saved result; the
   * id, URL and creation time never change. Tags are cleaned as when a bookmark is created.
   *
   * @throws NullPointerException if {@code title} or {@code tags} is {@code null}
   * @throws BookmarkNotFoundException if there is no bookmark with {@code id}
   */
  public Bookmark update(long id, String title, List<String> tags) {
    Objects.requireNonNull(title, "title");
    Objects.requireNonNull(tags, "tags");
    Bookmark existing = get(id);
    return repository.save(
        new Bookmark(existing.id(), existing.url(), title, tags, existing.createdAt()));
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
    checkPaging(page, size);
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

  /**
   * Replaces tag {@code from} with tag {@code to} on every bookmark tagged {@code from}, and returns
   * how many bookmarks changed. Both tags are cleaned first the way bookmarks clean their tags
   * (stripped, lowercased with {@code Locale.ROOT}). On each changed bookmark, {@code to} takes the
   * position {@code from} had; if the bookmark already had {@code to}, the first occurrence keeps
   * its position and the duplicate is dropped. The id, URL, title and creation time never change.
   * When the cleaned tags are equal nothing is saved and the result is 0.
   *
   * @throws NullPointerException if {@code from} or {@code to} is {@code null}
   * @throws IllegalArgumentException if {@code from} or {@code to} is blank
   */
  public int renameTag(String from, String to) {
    String cleanFrom = cleanTag(from, "from");
    String cleanTo = cleanTag(to, "to");
    if (cleanFrom.equals(cleanTo)) {
      return 0;
    }
    List<Bookmark> tagged = repository.findByTag(cleanFrom);
    for (Bookmark bookmark : tagged) {
      List<String> tags =
          bookmark.tags().stream().map(tag -> tag.equals(cleanFrom) ? cleanTo : tag).toList();
      repository.save(
          new Bookmark(
              bookmark.id(), bookmark.url(), bookmark.title(), tags, bookmark.createdAt()));
    }
    return tagged.size();
  }

  private static String cleanTag(String tag, String name) {
    String clean = Objects.requireNonNull(tag, name).strip().toLowerCase(Locale.ROOT);
    if (clean.isEmpty()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return clean;
  }

  /**
   * Returns one page of the bookmarks matching {@code text} and {@code tags}, newest first, with the
   * total number of matches. {@code text} is stripped; each tag is cleaned the way bookmarks clean
   * their tags (stripped, lowercased with {@code Locale.ROOT}) and blank tags are ignored. Paging
   * rules are those of {@link #list}.
   *
   * @throws NullPointerException if {@code text} or {@code tags} is {@code null}
   * @throws IllegalArgumentException if {@code page} or {@code size} is out of range
   */
  public BookmarkPage search(String text, Set<String> tags, int page, int size) {
    String cleanText = Objects.requireNonNull(text, "text").strip();
    Set<String> cleanTags = cleanTags(Objects.requireNonNull(tags, "tags"));
    checkPaging(page, size);
    return new BookmarkPage(
        repository.search(cleanText, cleanTags, page, size),
        page,
        size,
        repository.countSearch(cleanText, cleanTags));
  }

  private static Set<String> cleanTags(Set<String> tags) {
    return tags.stream()
        .map(tag -> Objects.requireNonNull(tag, "tag").strip().toLowerCase(Locale.ROOT))
        .filter(tag -> !tag.isEmpty())
        .collect(Collectors.toUnmodifiableSet());
  }

  private static void checkPaging(int page, int size) {
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
  }
}
