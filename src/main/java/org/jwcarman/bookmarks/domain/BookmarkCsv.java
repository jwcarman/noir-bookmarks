package org.jwcarman.bookmarks.domain;

import java.util.List;
import java.util.Objects;

/** CSV rendering of bookmarks. */
public final class BookmarkCsv {

  private static final String HEADER = "id,url,title,tags,createdAt";
  private static final String EOL = "\r\n";

  private BookmarkCsv() {}

  /**
   * Renders {@code bookmarks}, in the given order, as CSV: a header line {@code
   * id,url,title,tags,createdAt}, then one line per bookmark. Every line, including the last, ends
   * with {@code "\r\n"}. {@code tags} is the tags joined with {@code ";"}; {@code createdAt} is
   * {@link java.time.Instant#toString()}. A field that contains a comma, a double quote, a carriage
   * return or a line feed is enclosed in double quotes, with each double quote inside it doubled;
   * other fields are written as they are.
   *
   * @throws NullPointerException if {@code bookmarks} or any element is {@code null}
   */
  public static String write(List<Bookmark> bookmarks) {
    Objects.requireNonNull(bookmarks, "bookmarks");
    StringBuilder sb = new StringBuilder(HEADER).append(EOL);
    for (Bookmark bookmark : bookmarks) {
      Objects.requireNonNull(bookmark, "bookmark");
      sb.append(field(String.valueOf(bookmark.id())))
          .append(',')
          .append(field(bookmark.url()))
          .append(',')
          .append(field(bookmark.title()))
          .append(',')
          .append(field(String.join(";", bookmark.tags())))
          .append(',')
          .append(field(bookmark.createdAt().toString()))
          .append(EOL);
    }
    return sb.toString();
  }

  private static String field(String value) {
    if (needsQuotes(value)) {
      return '"' + value.replace("\"", "\"\"") + '"';
    }
    return value;
  }

  private static boolean needsQuotes(String value) {
    return value.chars().anyMatch(c -> c == ',' || c == '"' || c == '\r' || c == '\n');
  }
}
