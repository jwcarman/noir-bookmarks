package org.jwcarman.bookmarks.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class BookmarkRepositoryFindByUrlTest {

  static final Instant NOW = Instant.parse("2026-11-06T12:00:00Z");

  /** Returns fixed pages and records the requested pages. */
  static class PagedRepository implements BookmarkRepository {
    final List<Bookmark> all;
    final List<int[]> calls = new ArrayList<>();

    PagedRepository(List<Bookmark> all) {
      this.all = all;
    }

    @Override
    public Bookmark save(Bookmark bookmark) {
      throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Bookmark> findById(long id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public boolean deleteById(long id) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<Bookmark> findAll(int page, int size) {
      calls.add(new int[] {page, size});
      int from = Math.min(all.size(), page * size);
      int to = Math.min(all.size(), from + size);
      return all.subList(from, to);
    }

    @Override
    public long count() {
      return all.size();
    }

    List<Integer> pages() {
      return calls.stream().map(c -> c[0]).toList();
    }
  }

  static Bookmark bm(long id, String url) {
    return new Bookmark(id, url, "t" + id, List.of(), NOW);
  }

  static List<Bookmark> numbered(int n) {
    List<Bookmark> list = new ArrayList<>();
    for (int i = 0; i < n; i++) {
      list.add(bm(i + 1, "https://example.com/" + i));
    }
    return list;
  }

  @Test
  void findsOnSecondPage() {
    var repo = new PagedRepository(numbered(101));
    assertThat(repo.findByUrl("https://example.com/100"))
        .contains(bm(101, "https://example.com/100"));
    assertThat(repo.pages()).containsExactly(0, 1);
    assertThat(repo.calls).allSatisfy(c -> assertThat(c[1]).isEqualTo(100));
  }

  @Test
  void missingStopsAtShortPage() {
    var repo = new PagedRepository(numbered(101));
    assertThat(repo.findByUrl("https://example.com/101")).isEmpty();
    assertThat(repo.pages()).containsExactly(0, 1);
  }

  @Test
  void stopsAtFirstPageWhenFound() {
    var repo = new PagedRepository(numbered(250));
    assertThat(repo.findByUrl("https://example.com/5")).contains(bm(6, "https://example.com/5"));
    assertThat(repo.pages()).containsExactly(0);
  }

  @Test
  void exactlyFullPageThenEmpty() {
    var repo = new PagedRepository(numbered(100));
    assertThat(repo.findByUrl("https://example.com/x")).isEmpty();
    assertThat(repo.pages()).containsExactly(0, 1);
  }

  @Test
  void emptyRepository() {
    var repo = new PagedRepository(List.of());
    assertThat(repo.findByUrl("https://example.com/")).isEmpty();
    assertThat(repo.pages()).containsExactly(0);
  }

  @Test
  void caseSensitiveAndFirstMatchWins() {
    var repo =
        new PagedRepository(
            List.of(bm(1, "https://example.com/A"), bm(2, "https://example.com/a"),
                bm(3, "https://example.com/a")));
    assertThat(repo.findByUrl("https://example.com/a")).contains(bm(2, "https://example.com/a"));
    assertThat(repo.findByUrl("https://example.com/B")).isEmpty();
  }

  @Test
  void nullUrlThrows() {
    var repo = new PagedRepository(List.of());
    assertThatThrownBy(() -> repo.findByUrl(null)).isInstanceOf(NullPointerException.class);
  }
}
