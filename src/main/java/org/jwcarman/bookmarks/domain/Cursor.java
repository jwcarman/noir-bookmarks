package org.jwcarman.bookmarks.domain;

import java.nio.ByteBuffer;
import java.time.DateTimeException;
import java.time.Instant;
import java.util.Base64;
import java.util.Objects;

/**
 * A position in the newest-first order ({@code createdAt} descending, then {@code id} descending):
 * the last bookmark of a page. The next page holds the bookmarks that come strictly after it in that
 * order.
 *
 * @param createdAt the creation time of the last bookmark of the page
 * @param id the id of the last bookmark of the page
 */
public record Cursor(Instant createdAt, long id) {

  /** Epoch seconds (8 bytes), nanoseconds (4 bytes) and id (8 bytes). */
  private static final int TOKEN_BYTES = Long.BYTES + Integer.BYTES + Long.BYTES;

  /**
   * Creates a cursor.
   *
   * @throws NullPointerException if {@code createdAt} is {@code null}
   */
  public Cursor {
    Objects.requireNonNull(createdAt, "createdAt");
  }

  /**
   * Returns the cursor that points at {@code bookmark}.
   *
   * @throws NullPointerException if {@code bookmark} is {@code null}
   * @throws IllegalArgumentException if {@code bookmark} has no id
   */
  public static Cursor of(Bookmark bookmark) {
    Objects.requireNonNull(bookmark, "bookmark");
    if (bookmark.id() == null) {
      throw new IllegalArgumentException("bookmark has no id");
    }
    return new Cursor(bookmark.createdAt(), bookmark.id());
  }

  /**
   * Encodes this cursor as an opaque, non-empty token made only of the characters {@code A-Z},
   * {@code a-z}, {@code 0-9}, {@code -} and {@code _}, so it can be used in a URL as is. {@code
   * Cursor.decode(c.encode())} equals {@code c} for every cursor {@code c}, including the full
   * nanosecond precision of {@code createdAt}.
   */
  public String encode() {
    byte[] bytes =
        ByteBuffer.allocate(TOKEN_BYTES)
            .putLong(createdAt.getEpochSecond())
            .putInt(createdAt.getNano())
            .putLong(id)
            .array();
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  /**
   * Decodes a token made by {@link #encode()}.
   *
   * @throws NullPointerException if {@code token} is {@code null}
   * @throws IllegalArgumentException if {@code token} is not a token {@link #encode()} produces
   *     (for example {@code ""}, {@code "abc"}, {@code "!!"}, or a token with characters appended)
   */
  public static Cursor decode(String token) {
    Objects.requireNonNull(token, "token");
    Cursor cursor = parse(token);
    if (!cursor.encode().equals(token)) {
      throw invalid(token);
    }
    return cursor;
  }

  private static Cursor parse(String token) {
    try {
      byte[] bytes = Base64.getUrlDecoder().decode(token);
      if (bytes.length != TOKEN_BYTES) {
        throw invalid(token);
      }
      ByteBuffer buffer = ByteBuffer.wrap(bytes);
      long seconds = buffer.getLong();
      int nanos = buffer.getInt();
      return new Cursor(Instant.ofEpochSecond(seconds, nanos), buffer.getLong());
    } catch (IllegalArgumentException | DateTimeException e) {
      throw invalid(token);
    }
  }

  private static IllegalArgumentException invalid(String token) {
    return new IllegalArgumentException("invalid cursor: " + token);
  }
}
