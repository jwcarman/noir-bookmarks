package org.jwcarman.bookmarks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.Test;

class DuplicateBookmarkServiceTest {

  final InMemoryBookmarkRepository repository = new InMemoryBookmarkRepository();
  final BookmarkService service =
      new BookmarkService(
          repository, Clock.fixed(Instant.parse("2026-11-06T12:00:00Z"), ZoneOffset.UTC));

  @Test
  void createStoresCanonicalUrl() {
    var b = service.create("HTTPS://Example.com:443/docs/#intro", "Docs", List.of());
    assertThat(b.url()).isEqualTo("https://example.com/docs");
    assertThat(repository.findByUrl("https://example.com/docs")).contains(b);
    assertThat(service.get(b.id()).url()).isEqualTo("https://example.com/docs");
  }

  @Test
  void duplicateCanonicalUrlIsRejected() {
    var first = service.create("https://example.com/docs", "Docs", List.of());
    assertThat(first.id()).isEqualTo(1L);
    assertThatThrownBy(() -> service.create("HTTPS://EXAMPLE.com/docs/", "Again", List.of("x")))
        .isInstanceOfSatisfying(
            DuplicateBookmarkException.class,
            e -> {
              assertThat(e.url()).isEqualTo("https://example.com/docs");
              assertThat(e.existingId()).isEqualTo(1L);
            })
        .hasMessage("URL https://example.com/docs is already saved as bookmark 1");
    assertThat(service.list(0, 10).total()).isEqualTo(1);
    assertThat(service.get(1).title()).isEqualTo("Docs");
  }

  @Test
  void differentQueryIsNotDuplicate() {
    service.create("https://example.com/docs", "Docs", List.of());
    var b = service.create("https://example.com/docs?page=2", "Page 2", List.of());
    assertThat(b.id()).isEqualTo(2L);
    assertThat(b.url()).isEqualTo("https://example.com/docs?page=2");
    assertThat(service.list(0, 10).total()).isEqualTo(2);
  }

  @Test
  void duplicateOfSecondBookmarkNamesIt() {
    service.create("https://a.example/", "A", List.of());
    service.create("https://b.example/", "B", List.of());
    assertThatThrownBy(() -> service.create("https://B.example", "B2", List.of()))
        .isInstanceOfSatisfying(
            DuplicateBookmarkException.class, e -> assertThat(e.existingId()).isEqualTo(2L));
  }

  @Test
  void invalidUrlIsRejectedBeforeSaving() {
    assertThatThrownBy(() -> service.create("ftp://example.com/a", "A", List.of()))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(service.list(0, 10).total()).isZero();
  }

  @Test
  void nullUrlIsRejected() {
    assertThatThrownBy(() -> service.create(null, "A", List.of()))
        .isInstanceOf(NullPointerException.class);
    assertThat(service.list(0, 10).total()).isZero();
  }

  @Test
  void exceptionExposesFields() {
    var e = new DuplicateBookmarkException("https://x.example/", 42);
    assertThat(e.url()).isEqualTo("https://x.example/");
    assertThat(e.existingId()).isEqualTo(42L);
    assertThat(e).hasMessage("URL https://x.example/ is already saved as bookmark 42");
    assertThat(e).isInstanceOf(RuntimeException.class);
  }
}
