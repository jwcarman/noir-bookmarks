package org.jwcarman.bookmarks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.BookmarkRepository;
import org.jwcarman.bookmarks.domain.CollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.function.Executable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Import;

@DataJpaTest
@Import(CollectionServiceTest.Config.class)
class CollectionServiceTest {

  @ComponentScan(basePackages = "org.jwcarman.bookmarks.persistence")
  static class Config {}

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  @Autowired CollectionRepository collections;
  @Autowired BookmarkRepository bookmarks;

  CollectionService service;
  Bookmark b1;
  Bookmark b2;
  Bookmark b3;

  @BeforeEach
  void setUp() {
    service = new CollectionService(collections, bookmarks);
    b1 = bookmarks.save(new Bookmark(null, "https://1.example", "One", List.of("a"), NOON));
    b2 = bookmarks.save(new Bookmark(null, "https://2.example", "Two", List.of(), NOON));
    b3 = bookmarks.save(new Bookmark(null, "https://3.example", "Three", List.of(), NOON));
  }

  @Test
  void createStripsNameAndIsEmpty() {
    var view = service.create(" Work ");
    assertThat(view.name()).isEqualTo("Work");
    assertThat(view.bookmarks()).isEmpty();
    assertThat(service.get(view.id())).isEqualTo(view);
  }

  @Test
  void createRejectsDuplicateIgnoringCase() {
    service.create("Work");
    assertThatThrownBy(() -> service.create(" WORK "))
        .isInstanceOf(DuplicateCollectionException.class)
        .hasMessage("A collection named WORK already exists");
    assertThat(service.list()).hasSize(1);
  }

  @Test
  void createRejectsBadNames() {
    assertThatThrownBy(() -> service.create(null)).isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> service.create("  ")).isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> service.create("a".repeat(101)))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(service.list()).isEmpty();
  }

  @Test
  void renameEnforcesUniqueness() {
    var work = service.create("Work");
    var home = service.create("Home");
    assertThatThrownBy(() -> service.rename(home.id(), "work"))
        .isInstanceOf(DuplicateCollectionException.class)
        .hasMessage("A collection named work already exists");
    assertThat(service.get(home.id()).name()).isEqualTo("Home");
    var renamed = service.rename(work.id(), " WORK ");
    assertThat(renamed.id()).isEqualTo(work.id());
    assertThat(renamed.name()).isEqualTo("WORK");
    assertThat(service.get(work.id()).name()).isEqualTo("WORK");
  }

  @Test
  void renameKeepsMembers() {
    var c = service.create("Work");
    service.add(c.id(), b2.id());
    service.add(c.id(), b1.id());
    var renamed = service.rename(c.id(), "Job");
    assertThat(renamed).isEqualTo(new CollectionView(c.id(), "Job", List.of(b2, b1)));
  }

  @Test
  void renameRejectsBadNames() {
    var c = service.create("Work");
    assertThatThrownBy(() -> service.rename(c.id(), null))
        .isInstanceOf(NullPointerException.class);
    assertThatThrownBy(() -> service.rename(c.id(), " "))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void unknownCollectionIsNotFound() {
    assertNotFound(() -> service.get(9));
    assertNotFound(() -> service.rename(9, "x"));
    assertNotFound(() -> service.delete(9));
    assertNotFound(() -> service.add(9, b1.id()));
    assertNotFound(() -> service.remove(9, b1.id()));
  }

  void assertNotFound(Executable call) {
    assertThatThrownBy(call::execute)
        .isInstanceOfSatisfying(
            CollectionNotFoundException.class, e -> assertThat(e.id()).isEqualTo(9))
        .hasMessage("Collection 9 not found");
  }

  @Test
  void deleteKeepsBookmarks() {
    var c = service.create("Work");
    service.add(c.id(), b1.id());
    service.delete(c.id());
    assertThatThrownBy(() -> service.get(c.id())).isInstanceOf(CollectionNotFoundException.class);
    assertThat(bookmarks.findById(b1.id())).contains(b1);
  }

  @Test
  void addAppendsIdempotently() {
    var c = service.create("Work");
    assertThat(service.add(c.id(), b2.id()).bookmarks()).containsExactly(b2);
    assertThat(service.add(c.id(), b1.id()).bookmarks()).containsExactly(b2, b1);
    var last = service.add(c.id(), b2.id());
    assertThat(last).isEqualTo(new CollectionView(c.id(), "Work", List.of(b2, b1)));
  }

  @Test
  void removeIsIdempotentAndKeepsBookmark() {
    var c = service.create("Work");
    service.add(c.id(), b2.id());
    service.add(c.id(), b1.id());
    assertThat(service.remove(c.id(), b2.id()).bookmarks()).containsExactly(b1);
    assertThat(bookmarks.findById(b2.id())).contains(b2);
    var view = service.remove(c.id(), b3.id());
    assertThat(view).isEqualTo(new CollectionView(c.id(), "Work", List.of(b1)));
    assertThat(service.get(c.id()).bookmarks()).containsExactly(b1);
  }

  @Test
  void addUnknownBookmarkLeavesCollectionUnchanged() {
    var c = service.create("Work");
    service.add(c.id(), b1.id());
    assertThatThrownBy(() -> service.add(c.id(), 99))
        .isInstanceOf(BookmarkNotFoundException.class)
        .hasMessage("Bookmark 99 not found");
    assertThat(service.get(c.id()).bookmarks()).containsExactly(b1);
  }

  @Test
  void listSortsByNameAndCountsMembers() {
    var b = service.create("b");
    service.add(b.id(), b1.id());
    service.add(b.id(), b2.id());
    var a = service.create("A");
    assertThat(service.list())
        .containsExactly(new CollectionSummary(a.id(), "A", 0), new CollectionSummary(b.id(), "b", 2));
  }
}
