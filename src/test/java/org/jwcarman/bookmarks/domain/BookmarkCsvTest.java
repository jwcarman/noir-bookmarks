package org.jwcarman.bookmarks.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookmarkCsvTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");
  static final String HEADER = "id,url,title,tags,createdAt\r\n";

  static Bookmark titled(String title) {
    return new Bookmark(3L, "https://c.example", title, List.of(), NOON);
  }

  static String line(Bookmark b) {
    return BookmarkCsv.write(List.of(b)).substring(HEADER.length());
  }

  @Test
  void plainBookmark() {
    var b = new Bookmark(1L, "https://a.example", "Alpha", List.of("java", "spring"), NOON);
    assertThat(BookmarkCsv.write(List.of(b)))
        .isEqualTo(HEADER + "1,https://a.example,Alpha,java;spring,2026-11-06T12:00:00Z\r\n");
  }

  @Test
  void noTags() {
    var b = new Bookmark(2L, "https://b.example", "B", List.of(), NOON);
    assertThat(line(b)).isEqualTo("2,https://b.example,B,,2026-11-06T12:00:00Z\r\n");
  }

  @Test
  void emptyList() {
    assertThat(BookmarkCsv.write(List.of())).isEqualTo(HEADER);
  }

  @Test
  void keepsOrder() {
    var b1 = new Bookmark(1L, "https://a.example", "A", List.of(), NOON);
    var b2 = new Bookmark(2L, "https://b.example", "B", List.of(), NOON);
    assertThat(BookmarkCsv.write(List.of(b2, b1)))
        .isEqualTo(
            HEADER
                + "2,https://b.example,B,,2026-11-06T12:00:00Z\r\n"
                + "1,https://a.example,A,,2026-11-06T12:00:00Z\r\n");
  }

  @Test
  void commaIsQuoted() {
    assertThat(line(titled("Hello, world")))
        .isEqualTo("3,https://c.example,\"Hello, world\",,2026-11-06T12:00:00Z\r\n");
  }

  @Test
  void quotesAreDoubled() {
    assertThat(line(titled("Say \"hi\"")))
        .isEqualTo("3,https://c.example,\"Say \"\"hi\"\"\",,2026-11-06T12:00:00Z\r\n");
  }

  @Test
  void lineFeedIsQuoted() {
    assertThat(line(titled("two\nlines")))
        .isEqualTo("3,https://c.example,\"two\nlines\",,2026-11-06T12:00:00Z\r\n");
  }

  @Test
  void carriageReturnIsQuoted() {
    assertThat(line(titled("two\rlines")))
        .isEqualTo("3,https://c.example,\"two\rlines\",,2026-11-06T12:00:00Z\r\n");
  }

  @Test
  void otherSpecialLookingCharactersAreNotQuoted() {
    assertThat(line(titled("a;b 'c' \tx")))
        .isEqualTo("3,https://c.example,a;b 'c' \tx,,2026-11-06T12:00:00Z\r\n");
  }

  @Test
  void urlWithCommaIsQuoted() {
    var b = new Bookmark(4L, "https://a.example/?q=a,b", "T", List.of("x"), NOON);
    assertThat(line(b)).isEqualTo("4,\"https://a.example/?q=a,b\",T,x,2026-11-06T12:00:00Z\r\n");
  }

  @Test
  void nullListThrows() {
    assertThatThrownBy(() -> BookmarkCsv.write(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void nullElementThrows() {
    assertThatThrownBy(() -> BookmarkCsv.write(Arrays.asList(titled("x"), null)))
        .isInstanceOf(NullPointerException.class);
  }
}
