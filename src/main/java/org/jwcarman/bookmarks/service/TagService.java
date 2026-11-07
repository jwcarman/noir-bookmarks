package org.jwcarman.bookmarks.service;

import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.BookmarkRepository;
import org.springframework.stereotype.Service;

/** Tag-wide operations over the bookmarks in a repository. */
@Service
public class TagService {

  private final BookmarkRepository repository;

  /**
   * Creates the service.
   *
   * @throws NullPointerException if {@code repository} is {@code null}
   */
  public TagService(BookmarkRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  /**
   * Returns the bookmarks tagged {@code tag}, newest first. The tag is cleaned the way bookmarks
   * clean their tags (stripped and lowercased with {@code Locale.ROOT}) before the lookup.
   *
   * @throws NullPointerException if {@code tag} is {@code null}
   * @throws IllegalArgumentException if {@code tag} is blank
   */
  public List<Bookmark> listByTag(String tag) {
    return repository.findByTag(cleanTag(tag, "tag"));
  }

  /**
   * Replaces tag {@code from} with tag {@code to} on every bookmark tagged {@code from}, and returns
   * how many bookmarks changed. Both tags are cleaned first the way bookmarks clean their tags
   * (stripped, lowercased with {@code Locale.ROOT}). On each changed bookmark, {@code to} takes the
   * position {@code from} had; if the bookmark already had {@code to}, the first occurrence keeps
   * its position and the duplicate is dropped. The id, URL, title, creation time and notes never
   * change. When the cleaned tags are equal nothing is saved and the result is 0.
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
              bookmark.id(),
              bookmark.url(),
              bookmark.title(),
              tags,
              bookmark.createdAt(),
              bookmark.notes()));
    }
    return tagged.size();
  }

  /**
   * Deletes every bookmark tagged {@code tag} and returns how many were deleted. The tag is cleaned
   * the way bookmarks clean their tags (stripped, lowercased with {@code Locale.ROOT}) first.
   *
   * @throws NullPointerException if {@code tag} is {@code null}
   * @throws IllegalArgumentException if {@code tag} is blank
   */
  public int deleteTagged(String tag) {
    return repository.deleteByTag(cleanTag(tag, "tag"));
  }

  private static String cleanTag(String tag, String name) {
    String clean = Objects.requireNonNull(tag, name).strip().toLowerCase(Locale.ROOT);
    if (clean.isEmpty()) {
      throw new IllegalArgumentException(name + " must not be blank");
    }
    return clean;
  }
}
