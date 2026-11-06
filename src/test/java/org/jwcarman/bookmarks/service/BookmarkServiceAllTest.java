package org.jwcarman.bookmarks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.spy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.junit.jupiter.api.Test;

class BookmarkServiceAllTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  final InMemoryBookmarkRepository repository = spy(new InMemoryBookmarkRepository());
  final BookmarkService service =
      new BookmarkService(repository, Clock.fixed(NOON, ZoneOffset.UTC));

  void fill(int n) {
    for (int i = 0; i < n; i++) {
      repository.save(
          new Bookmark(null, "https://e.example/" + i, "T" + i, List.of(), NOON.plusSeconds(i)));
    }
  }

  @Test
  void readsEveryPageNewestFirst() {
    fill(250);
    List<Bookmark> all = service.all();
    assertThat(all).hasSize(250);
    for (int i = 0; i < 250; i++) {
      assertThat(all.get(i).id()).isEqualTo(250L - i);
    }
    var order = inOrder(repository);
    order.verify(repository).findAll(0, 100);
    order.verify(repository).findAll(1, 100);
    order.verify(repository).findAll(2, 100);
    order.verifyNoMoreInteractions();
  }

  @Test
  void exactlyOnePage() {
    fill(100);
    assertThat(service.all()).hasSize(100);
    var order = inOrder(repository);
    order.verify(repository).findAll(0, 100);
    order.verify(repository).findAll(1, 100);
    order.verifyNoMoreInteractions();
  }

  @Test
  void emptyRepository() {
    assertThat(service.all()).isEmpty();
  }
}
