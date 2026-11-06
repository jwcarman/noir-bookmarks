package org.jwcarman.bookmarks.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.junit.jupiter.api.Test;

class UrlNormalizerTest {

  @ParameterizedTest
  @CsvSource(
      delimiter = '|',
      value = {
        // R1
        "HTTPS://Example.COM/Path|https://example.com/Path",
        "http://WWW.Example.org/a/B|http://www.example.org/a/B",
        "HtTp://ABCXYZ.com/Q|http://abcxyz.com/Q",
        // R2
        "http://example.com:80/a|http://example.com/a",
        "https://example.com:443/a|https://example.com/a",
        "https://example.com:8443/a|https://example.com:8443/a",
        "http://example.com:443/a|http://example.com:443/a",
        "https://example.com:80/a|https://example.com:80/a",
        "http://example.com:8080|http://example.com:8080/",
        "http://example.com:80?q=1|http://example.com/?q=1",
        "http://example.com:800/a|http://example.com:800/a",
        // R3
        "https://example.com/a?b=C&d=e#Top|https://example.com/a?b=C&d=e",
        "https://example.com/a#|https://example.com/a",
        "https://example.com/a%20b?q=%2F|https://example.com/a%20b?q=%2F",
        "https://example.com#frag|https://example.com/",
        "https://example.com/a?x=/y/#f|https://example.com/a?x=/y/",
        "https://example.com?q=A|https://example.com/?q=A",
        // R4
        "https://example.com|https://example.com/",
        "https://example.com/|https://example.com/",
        "https://example.com/docs/|https://example.com/docs",
        "https://example.com/docs//|https://example.com/docs/",
        "HTTPS://Example.com:443/docs/?x=1#y|https://example.com/docs?x=1",
        // misc
        "http://User:Pw@Example.COM:80/a|http://User:Pw@example.com/a",
        "http://[::1]:80/a|http://[::1]/a",
        "http://[::1]/a|http://[::1]/a",
        "http://[::1]:8080/a|http://[::1]:8080/a",
        "http://ÄBC.com/a|http://Äbc.com/a",
      })
  void normalizes(String input, String expected) {
    assertThat(UrlNormalizer.normalize(input)).isEqualTo(expected);
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "ftp://example.com/a",
        "example.com/a",
        "https://",
        "https://:443/a",
        "https:///a",
        "https://?q",
        "https://#f",
        "not a url",
        "",
        "http://exa mple.com/",
        "http://example.com/a b",
        "http://example.com/\u007f",
        "http://example.com/\t",
        "http://example.com:8x/a",
        "http://example.com:a:8/x",
        "http://a:b/x",
        "httpſ://example.com/",
        "https://user@/a",
        "mailto://x",
      })
  void rejectsInvalid(String input) {
    assertThatIllegalArgumentException()
        .isThrownBy(() -> UrlNormalizer.normalize(input))
        .withMessageContaining("http");
  }

  @Test
  void rejectsNull() {
    assertThatNullPointerException().isThrownBy(() -> UrlNormalizer.normalize(null));
  }

  @Test
  void keepsEmptyPortAsGiven() {
    assertThat(UrlNormalizer.normalize("http://example.com:/a")).isEqualTo("http://example.com:/a");
  }

  @Test
  void spaceBoundaryCharactersAreAllowed() {
    assertThat(UrlNormalizer.normalize("http://example.com/!~")).isEqualTo("http://example.com/!~");
  }
}
