package org.jwcarman.bookmarks.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.BookmarkCsv;
import org.junit.jupiter.api.Test;

class BookmarkImporterNotesTest {

  static final String NEW_HEADER = "id,url,title,tags,createdAt,notes\r\n";
  static final String OLD_HEADER = "id,url,title,tags,createdAt\r\n";

  final InMemoryBookmarkRepository repository = new InMemoryBookmarkRepository();
  final BookmarkImporter importer = new BookmarkImporter(repository);

  Bookmark onlyStored() {
    List<Bookmark> all = repository.findAll(0, 100);
    assertThat(all).hasSize(1);
    return all.get(0);
  }

  @Test
  void sixthFieldIsTheNotes() {
    assertThat(
            importer.importCsv(
                NEW_HEADER + "1,https://a.example/,A,,2024-01-01T00:00:00Z,why\r\n"))
        .isEqualTo(new ImportResult(1, List.of()));
    assertThat(onlyStored().notes()).isEqualTo("why");
  }

  @Test
  void oldExportGetsEmptyNotes() {
    assertThat(
            importer.importCsv(OLD_HEADER + "1,https://a.example/,A,,2024-01-01T00:00:00Z\r\n"))
        .isEqualTo(new ImportResult(1, List.of()));
    assertThat(onlyStored().notes()).isEmpty();
  }

  @Test
  void fiveFieldsWithNotesHeaderGetEmptyNotes() {
    assertThat(
            importer.importCsv(NEW_HEADER + "1,https://a.example/,A,,2024-01-01T00:00:00Z\r\n"))
        .isEqualTo(new ImportResult(1, List.of()));
    assertThat(onlyStored().notes()).isEmpty();
  }

  @Test
  void sixFieldsWithOldHeaderKeepNotes() {
    assertThat(
            importer.importCsv(
                OLD_HEADER + "1,https://a.example/,A,,2024-01-01T00:00:00Z,why\r\n"))
        .isEqualTo(new ImportResult(1, List.of()));
    assertThat(onlyStored().notes()).isEqualTo("why");
  }

  @Test
  void sevenFieldsAreWrongFieldCount() {
    assertThat(
            importer.importCsv(
                NEW_HEADER + "1,https://a.example/,A,,2024-01-01T00:00:00Z,why,extra\r\n"))
        .isEqualTo(new ImportResult(0, List.of(new SkippedRow(2, SkipReason.WRONG_FIELD_COUNT))));
    assertThat(repository.count()).isZero();
  }

  @Test
  void fourFieldsAreWrongFieldCount() {
    assertThat(importer.importCsv(NEW_HEADER + "1,https://a.example/,A,\r\n"))
        .isEqualTo(new ImportResult(0, List.of(new SkippedRow(2, SkipReason.WRONG_FIELD_COUNT))));
  }

  @Test
  void notesAreStoredAsGiven() {
    importer.importCsv(
        NEW_HEADER + "1,https://a.example/,A,,2024-01-01T00:00:00Z,\"  a, \"\"b\"\"\n c  \"\r\n");
    assertThat(onlyStored().notes()).isEqualTo("  a, \"b\"\n c  ");
  }

  @Test
  void tooLongNotesAreCut() {
    assertThat(
            importer.importCsv(
                NEW_HEADER
                    + "1,https://a.example/,A,,2024-01-01T00:00:00Z,"
                    + "y".repeat(2500)
                    + "\r\n"))
        .isEqualTo(new ImportResult(1, List.of()));
    assertThat(onlyStored().notes()).isEqualTo("y".repeat(2000));
  }

  @Test
  void notesOfExactlyMaxLengthAreKept() {
    String notes = "a".repeat(1999) + "z";
    importer.importCsv(
        NEW_HEADER + "1,https://a.example/,A,,2024-01-01T00:00:00Z," + notes + "\r\n");
    assertThat(onlyStored().notes()).isEqualTo(notes);
  }

  @Test
  void notesOneOverMaxLengthAreCut() {
    String notes = "a".repeat(2000) + "z";
    importer.importCsv(
        NEW_HEADER + "1,https://a.example/,A,,2024-01-01T00:00:00Z," + notes + "\r\n");
    assertThat(onlyStored().notes()).isEqualTo("a".repeat(2000));
  }

  @Test
  void exportRoundTripsNotes() {
    List<Bookmark> exported =
        List.of(
            new Bookmark(
                1L, "https://a.example/", "A", List.of("x"), Instant.parse("2024-01-01T00:00:00Z"),
                "one, two\nthree"),
            new Bookmark(
                2L, "https://b.example/", "B", List.of(), Instant.parse("2024-02-01T00:00:00Z"),
                "plain"),
            new Bookmark(
                3L, "https://c.example/", "C", List.of(), Instant.parse("2024-03-01T00:00:00Z"),
                ""));
    assertThat(importer.importCsv(BookmarkCsv.write(exported)))
        .isEqualTo(new ImportResult(3, List.of()));
    List<Bookmark> stored =
        repository.findAll(0, 100).stream().sorted(Comparator.comparing(Bookmark::url)).toList();
    assertThat(stored).extracting(Bookmark::notes).containsExactly("one, two\nthree", "plain", "");
    assertThat(stored).extracting(Bookmark::createdAt)
        .containsExactlyElementsOf(exported.stream().map(Bookmark::createdAt).toList());
  }
}
