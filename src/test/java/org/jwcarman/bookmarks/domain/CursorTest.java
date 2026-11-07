package org.jwcarman.bookmarks.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class CursorTest {

  static final Instant T = Instant.parse("2024-01-01T10:00:00.123456789Z");

  @Test
  void roundTripsWithNanosecondsAndIsUrlSafe() {
    var cursor = new Cursor(T, 42);
    String token = cursor.encode();
    assertThat(token).matches("[A-Za-z0-9_-]+");
    assertThat(Cursor.decode(token)).isEqualTo(cursor);
    assertThat(Cursor.decode(token).createdAt().getNano()).isEqualTo(123456789);
  }

  @Test
  void roundTripsEdgeValues() {
    for (var cursor :
        List.of(
            new Cursor(T, 0),
            new Cursor(T, Long.MAX_VALUE),
            new Cursor(T, Long.MIN_VALUE),
            new Cursor(Instant.EPOCH, 5),
            new Cursor(Instant.parse("1969-12-31T23:59:59Z"), 5),
            new Cursor(Instant.MIN, 1),
            new Cursor(Instant.MAX, 1))) {
      String token = cursor.encode();
      assertThat(token).matches("[A-Za-z0-9_-]+");
      assertThat(Cursor.decode(token)).isEqualTo(cursor);
    }
  }

  @Test
  void distinctCursorsHaveDistinctTokens() {
    assertThat(new Cursor(T, 1).encode()).isNotEqualTo(new Cursor(T, 2).encode());
    assertThat(new Cursor(T, 1).encode()).isNotEqualTo(new Cursor(T.plusNanos(1), 1).encode());
    assertThat(new Cursor(T, 1).encode())
        .isNotEqualTo(new Cursor(T.plusSeconds(1), 1).encode());
  }

  @ParameterizedTest
  @ValueSource(strings = {"", "abc", "!!", "AAAAAAAAAAAAAAAAAAAAAAAAAA", "AAAAAAAAAAAAAAAAAAAAAAAAAAAA"})
  void rejectsInvalidTokens(String token) {
    assertThatThrownBy(() -> Cursor.decode(token))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("invalid cursor");
  }

  @Test
  void rejectsTokenWithCharactersAppended() {
    String token = new Cursor(T, 42).encode();
    assertThatThrownBy(() -> Cursor.decode(token + "x"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsPaddedOrNonCanonicalTokens() {
    String token = new Cursor(T, 42).encode();
    assertThatThrownBy(() -> Cursor.decode(token + "="))
        .isInstanceOf(IllegalArgumentException.class);
    // the last character carries 2 unused bits; setting them makes a non-canonical token
    char last = token.charAt(token.length() - 1);
    String alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";
    char other = alphabet.charAt(alphabet.indexOf(last) ^ 1);
    String tampered = token.substring(0, token.length() - 1) + other;
    assertThatThrownBy(() -> Cursor.decode(tampered))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsOutOfRangeNanosAndSeconds() {
    // seconds 0, nanos 1_000_000_000 (0x3B9ACA00), id 0
    byte[] bytes = new byte[20];
    bytes[8] = 0x3B;
    bytes[9] = (byte) 0x9A;
    bytes[10] = (byte) 0xCA;
    bytes[11] = 0x00;
    String badNanos = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    assertThatThrownBy(() -> Cursor.decode(badNanos)).isInstanceOf(IllegalArgumentException.class);

    byte[] huge = new byte[20];
    huge[0] = 0x7F;
    String badSeconds = java.util.Base64.getUrlEncoder().withoutPadding().encodeToString(huge);
    assertThatThrownBy(() -> Cursor.decode(badSeconds))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void decodeNullThrowsNpe() {
    assertThatThrownBy(() -> Cursor.decode(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void nullCreatedAtThrowsNpe() {
    assertThatThrownBy(() -> new Cursor(null, 1)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void ofPointsAtTheBookmark() {
    var bookmark = new Bookmark(7L, "https://a.example/", "A", List.of(), T);
    assertThat(Cursor.of(bookmark)).isEqualTo(new Cursor(T, 7));
  }

  @Test
  void ofRejectsUnsavedAndNull() {
    var unsaved = new Bookmark(null, "https://a.example/", "A", List.of(), T);
    assertThatThrownBy(() -> Cursor.of(unsaved)).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> Cursor.of(null)).isInstanceOf(NullPointerException.class);
  }
}
