package org.jwcarman.bookmarks.web;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.Test;

class UpdateBookmarkRequestTest {

  @Test
  void missingTagsMeanNoTags() {
    assertThat(new UpdateBookmarkRequest("t", null).tags()).isEmpty();
  }

  @Test
  void keepsGivenValues() {
    var r = new UpdateBookmarkRequest("t", List.of("a"));
    assertThat(r.title()).isEqualTo("t");
    assertThat(r.tags()).containsExactly("a");
  }
}
