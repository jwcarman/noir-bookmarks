package org.jwcarman.bookmarks.persistence;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.BookmarkCollection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import({JpaBookmarkRepository.class, JpaCollectionRepository.class})
class JpaCollectionRepositoryTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  @Autowired JpaBookmarkRepository bookmarks;
  @Autowired JpaCollectionRepository collections;
  @Autowired TestEntityManager entityManager;

  long b1;
  long b2;
  long b3;

  @BeforeEach
  void seed() {
    b1 = save("https://1.example", "x");
    b2 = save("https://2.example", "x");
    b3 = save("https://3.example", "y");
    entityManager.flush();
    entityManager.clear();
  }

  long save(String url, String tag) {
    return bookmarks.save(new Bookmark(null, url, "T", List.of(tag), NOON)).id();
  }

  BookmarkCollection reload(long id) {
    entityManager.flush();
    entityManager.clear();
    return collections.findById(id).orElseThrow();
  }

  @Test
  void savesAndReadsBackOrderedMembers() {
    var saved = collections.save(new BookmarkCollection(null, "Work", List.of(b3, b1)));
    assertThat(saved.id()).isNotNull();
    assertThat(saved.name()).isEqualTo("Work");
    assertThat(saved.bookmarkIds()).containsExactly(b3, b1);
    var read = reload(saved.id());
    assertThat(read).isEqualTo(new BookmarkCollection(saved.id(), "Work", List.of(b3, b1)));
  }

  @Test
  void findByIdWithinSamePersistenceContext() {
    var saved = collections.save(new BookmarkCollection(null, "Work", List.of(b2, b1)));
    assertThat(collections.findById(saved.id()).orElseThrow().bookmarkIds())
        .containsExactly(b2, b1);
  }

  @Test
  void savingAgainReplacesNameAndMembers() {
    var saved = collections.save(new BookmarkCollection(null, "Work", List.of(b3, b1)));
    reload(saved.id());
    var again =
        collections.save(new BookmarkCollection(saved.id(), "Job", List.of(b3, b1, b2)));
    assertThat(again.id()).isEqualTo(saved.id());
    var read = reload(saved.id());
    assertThat(read.name()).isEqualTo("Job");
    assertThat(read.bookmarkIds()).containsExactly(b3, b1, b2);
    assertThat(collections.findAll()).hasSize(1);
  }

  @Test
  void savingWithUnknownIdCreatesNew() {
    var saved = collections.save(new BookmarkCollection(12345L, "Work", List.of()));
    assertThat(saved.id()).isNotNull().isNotEqualTo(12345L);
    assertThat(reload(saved.id()).name()).isEqualTo("Work");
  }

  @Test
  void findAllSortsByNameIgnoringCaseThenId() {
    var beta = collections.save(new BookmarkCollection(null, "beta", List.of()));
    var alpha = collections.save(new BookmarkCollection(null, "Alpha", List.of(b1)));
    var gamma = collections.save(new BookmarkCollection(null, "gamma", List.of()));
    var beta2 = collections.save(new BookmarkCollection(null, "Beta", List.of()));
    entityManager.flush();
    entityManager.clear();
    assertThat(collections.findAll())
        .extracting(BookmarkCollection::id)
        .containsExactly(alpha.id(), beta.id(), beta2.id(), gamma.id());
    assertThat(collections.findAll().get(0).bookmarkIds()).containsExactly(b1);
  }

  @Test
  void findByNameIgnoresCase() {
    var alpha = collections.save(new BookmarkCollection(null, "Alpha", List.of()));
    collections.save(new BookmarkCollection(null, "beta", List.of()));
    entityManager.flush();
    entityManager.clear();
    assertThat(collections.findByName("ALPHA")).contains(alpha);
    assertThat(collections.findByName("alpha")).contains(alpha);
    assertThat(collections.findByName("gamma")).isEmpty();
  }

  @Test
  void deleteRemovesCollectionButNotBookmarks() {
    var saved = collections.save(new BookmarkCollection(null, "Work", List.of(b1, b2)));
    entityManager.flush();
    entityManager.clear();
    assertThat(collections.deleteById(saved.id())).isTrue();
    entityManager.clear();
    assertThat(collections.findById(saved.id())).isEmpty();
    assertThat(bookmarks.findById(b1)).isPresent();
    assertThat(bookmarks.findById(b2)).isPresent();
    assertThat(memberRows()).isZero();
    assertThat(collections.deleteById(saved.id())).isFalse();
  }

  long memberRows() {
    return ((Number)
            entityManager
                .getEntityManager()
                .createNativeQuery("select count(*) from collection_bookmark")
                .getSingleResult())
        .longValue();
  }

  @Test
  void deletingBookmarkRemovesItFromEveryCollection() {
    var a = collections.save(new BookmarkCollection(null, "A", List.of(b1, b2, b3)));
    var b = collections.save(new BookmarkCollection(null, "B", List.of(b2)));
    var c = collections.save(new BookmarkCollection(null, "C", List.of(b3)));
    entityManager.flush();
    entityManager.clear();
    assertThat(bookmarks.deleteById(b2)).isTrue();
    assertThat(reload(a.id()).bookmarkIds()).containsExactly(b1, b3);
    assertThat(reload(b.id()).bookmarkIds()).isEmpty();
    assertThat(reload(c.id()).bookmarkIds()).containsExactly(b3);
    assertThat(bookmarks.findById(b2)).isEmpty();
    assertThat(bookmarks.deleteById(b2)).isFalse();
  }

  @Test
  void deletingBookmarkWithCollectionsLoaded() {
    var a = collections.save(new BookmarkCollection(null, "A", List.of(b1, b2, b3)));
    assertThat(bookmarks.deleteById(b1)).isTrue();
    assertThat(reload(a.id()).bookmarkIds()).containsExactly(b2, b3);
  }

  @Test
  void deletingByTagRemovesFromEveryCollection() {
    var a = collections.save(new BookmarkCollection(null, "A", List.of(b1, b3, b2)));
    entityManager.flush();
    entityManager.clear();
    assertThat(bookmarks.deleteByTag("x")).isEqualTo(2);
    assertThat(reload(a.id()).bookmarkIds()).containsExactly(b3);
    assertThat(bookmarks.count()).isEqualTo(1);
    assertThat(memberRows()).isEqualTo(1);
  }
}
