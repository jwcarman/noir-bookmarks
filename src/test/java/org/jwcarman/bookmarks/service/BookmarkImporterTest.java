package org.jwcarman.bookmarks.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.BookmarkCsv;
import org.junit.jupiter.api.Test;

class BookmarkImporterTest {

  static final String H = "id,url,title,tags,createdAt\r\n";
  static final String T = "2024-01-01T00:00:00Z";

  final InMemoryBookmarkRepository repository = new InMemoryBookmarkRepository();
  final BookmarkImporter importer = new BookmarkImporter(repository);

  static String row(String url, String title, String tags) {
    return "1," + url + "," + title + "," + tags + "," + T + "\r\n";
  }

  List<Bookmark> stored() {
    return repository.findAll(0, 2000);
  }

  List<String> storedUrls() {
    return stored().stream().map(Bookmark::url).sorted().toList();
  }

  ImportResult importOne(String row) {
    return importer.importCsv(H + row);
  }

  @Test
  void rejectsNullRepository() {
    assertThatThrownBy(() -> new BookmarkImporter(null))
        .isInstanceOf(NullPointerException.class);
  }

  @Test
  void savesRowWithNewIdCanonicalUrlCleanTagsAndOwnTime() {
    ImportResult result =
        importer.importCsv(
            H
                + "42,HTTPS://Example.com:443/a/,Example, Java ;news;JAVA,"
                + "2024-03-01T10:15:30.123456789Z\r\n");

    assertThat(result).isEqualTo(new ImportResult(1, List.of()));
    assertThat(stored()).hasSize(1);
    Bookmark saved = stored().get(0);
    assertThat(saved.id()).isNotNull().isNotEqualTo(42L);
    assertThat(saved.url()).isEqualTo("https://example.com/a");
    assertThat(saved.title()).isEqualTo("Example");
    assertThat(saved.tags()).containsExactly("java", "news");
    assertThat(saved.createdAt()).isEqualTo(Instant.parse("2024-03-01T10:15:30.123456Z"));
  }

  @Test
  void savesSeveralRows() {
    ImportResult result =
        importer.importCsv(H + row("https://a.example/", "A", "") + row("https://b.example/", "B", ""));
    assertThat(result).isEqualTo(new ImportResult(2, List.of()));
    assertThat(storedUrls()).containsExactly("https://a.example/", "https://b.example/");
  }

  @Test
  void emptyTagsFieldMeansNoTags() {
    importOne(row("https://a.example/", "A", ""));
    assertThat(stored().get(0).tags()).isEmpty();
  }

  @Test
  void blankTagsAreDropped() {
    importOne(row("https://a.example/", "A", " ;x;;  ;y;"));
    assertThat(stored().get(0).tags()).containsExactly("x", "y");
  }

  @Test
  void titleIsStoredAsGiven() {
    importOne(row("https://a.example/", " Padded Title ", ""));
    assertThat(stored().get(0).title()).isEqualTo(" Padded Title ");
  }

  @Test
  void headerOnlyImportsNothing() {
    assertThat(importer.importCsv(H)).isEqualTo(new ImportResult(0, List.of()));
    assertThat(stored()).isEmpty();
  }

  @Test
  void skipsInvalidRowsWithFirstReason() {
    String csv =
        H
            + "1,https://a.example/,A,,2024-01-01T00:00:00Z\r\n"
            + "2,https://b.example/,B\r\n"
            + "3,ftp://c.example/,C,,2024-01-01T00:00:00Z\r\n"
            + "4,https://d.example/,   ,,2024-01-01T00:00:00Z\r\n"
            + "5,https://e.example/,E,a;b;c;d;e;f;g;h;i;j;k,2024-01-01T00:00:00Z\r\n"
            + "6,https://f.example/,F,,yesterday\r\n"
            + "7,https://g.example/,G,,2024-01-01T00:00:00Z\r\n";
    assertThat(importer.importCsv(csv))
        .isEqualTo(
            new ImportResult(
                2,
                List.of(
                    new SkippedRow(3, SkipReason.WRONG_FIELD_COUNT),
                    new SkippedRow(4, SkipReason.INVALID_URL),
                    new SkippedRow(5, SkipReason.INVALID_TITLE),
                    new SkippedRow(6, SkipReason.INVALID_TAGS),
                    new SkippedRow(7, SkipReason.INVALID_CREATED_AT))));
    assertThat(storedUrls()).containsExactly("https://a.example/", "https://g.example/");
  }

  @Test
  void sixFieldsIsWrongFieldCount() {
    assertThat(importOne("1,https://a.example/,A,,x,extra\r\n").skipped())
        .containsExactly(new SkippedRow(2, SkipReason.WRONG_FIELD_COUNT));
  }

  @Test
  void fieldCountIsCheckedFirst() {
    assertThat(importOne("1,ftp://a.example/\r\n").skipped())
        .containsExactly(new SkippedRow(2, SkipReason.WRONG_FIELD_COUNT));
  }

  @Test
  void urlIsCheckedBeforeTitle() {
    assertThat(importOne("1,ftp://x.example/, ,a;b;c;d;e;f;g;h;i;j;k,bad\r\n").skipped())
        .containsExactly(new SkippedRow(2, SkipReason.INVALID_URL));
  }

  @Test
  void titleIsCheckedBeforeTags() {
    assertThat(importOne("1,https://x.example/,,a;b;c;d;e;f;g;h;i;j;k,bad\r\n").skipped())
        .containsExactly(new SkippedRow(2, SkipReason.INVALID_TITLE));
  }

  @Test
  void tagsAreCheckedBeforeCreatedAt() {
    assertThat(importOne("1,https://x.example/,X," + "t".repeat(51) + ",bad\r\n").skipped())
        .containsExactly(new SkippedRow(2, SkipReason.INVALID_TAGS));
  }

  @Test
  void createdAtIsCheckedBeforeDuplicate() {
    repository.save(new Bookmark(null, "https://x.example/", "X", List.of(), Instant.EPOCH));
    assertThat(importOne("1,https://x.example/,X,,bad\r\n").skipped())
        .containsExactly(new SkippedRow(2, SkipReason.INVALID_CREATED_AT));
  }

  @Test
  void emptyTitleIsInvalid() {
    assertThat(importOne(row("https://a.example/", "", "")).skipped())
        .containsExactly(new SkippedRow(2, SkipReason.INVALID_TITLE));
  }

  @Test
  void titleOf201IsInvalidAnd200IsSaved() {
    assertThat(importOne(row("https://a.example/", "t".repeat(201), "")))
        .isEqualTo(new ImportResult(0, List.of(new SkippedRow(2, SkipReason.INVALID_TITLE))));
    assertThat(importOne(row("https://a.example/", "t".repeat(200), "")))
        .isEqualTo(new ImportResult(1, List.of()));
    assertThat(stored().get(0).title()).hasSize(200);
  }

  @Test
  void tagOf51IsInvalidAnd50IsSaved() {
    assertThat(importOne(row("https://a.example/", "A", "ok;" + "t".repeat(51))))
        .isEqualTo(new ImportResult(0, List.of(new SkippedRow(2, SkipReason.INVALID_TAGS))));
    assertThat(importOne(row("https://a.example/", "A", " " + "T".repeat(50) + " ")))
        .isEqualTo(new ImportResult(1, List.of()));
    assertThat(stored().get(0).tags()).containsExactly("t".repeat(50));
  }

  @Test
  void elevenTagsThatCleanToTenAreSaved() {
    assertThat(importOne(row("https://a.example/", "A", "a;b;c;d;e;f;g;h;i;j;A")))
        .isEqualTo(new ImportResult(1, List.of()));
    assertThat(stored().get(0).tags()).containsExactly("a", "b", "c", "d", "e", "f", "g", "h", "i", "j");
  }

  @Test
  void blankTagsDoNotCountTowardsTheLimit() {
    assertThat(importOne(row("https://a.example/", "A", "a;b;c;d;e;f;g;h;i; ;j;")))
        .isEqualTo(new ImportResult(1, List.of()));
    assertThat(stored().get(0).tags()).hasSize(10);
  }

  @Test
  void urlOf2049IsInvalidAnd2048IsSaved() {
    String base = "https://a.example/";
    String tooLong = base + "x".repeat(2031);
    String longest = base + "x".repeat(2030);
    assertThat(tooLong).hasSize(2049);
    assertThat(longest).hasSize(2048);
    assertThat(importOne(row(tooLong, "A", "")))
        .isEqualTo(new ImportResult(0, List.of(new SkippedRow(2, SkipReason.INVALID_URL))));
    assertThat(importOne(row(longest, "A", ""))).isEqualTo(new ImportResult(1, List.of()));
    assertThat(storedUrls()).containsExactly(longest);
  }

  @Test
  void urlLengthIsMeasuredAsGiven() {
    // canonical form is shorter than 2048, but the URL as given is 2049 characters
    String given = "https://a.example:443/" + "x".repeat(2027);
    assertThat(given).hasSize(2049);
    assertThat(importOne(row(given, "A", "")).skipped())
        .containsExactly(new SkippedRow(2, SkipReason.INVALID_URL));
  }

  @Test
  void skipsDuplicateOfStoredBookmark() {
    Bookmark existing =
        repository.save(
            new Bookmark(null, "https://example.com/a", "Old", List.of("x"), Instant.EPOCH));
    assertThat(importOne(row("HTTPS://EXAMPLE.com/a/", "New", "y")))
        .isEqualTo(new ImportResult(0, List.of(new SkippedRow(2, SkipReason.DUPLICATE))));
    assertThat(stored()).containsExactly(existing);
  }

  @Test
  void skipsDuplicateWithinFile() {
    assertThat(
            importer.importCsv(
                H + row("https://a.example/", "A", "") + row("https://a.example", "A2", "")))
        .isEqualTo(new ImportResult(1, List.of(new SkippedRow(3, SkipReason.DUPLICATE))));
    assertThat(stored()).hasSize(1);
    assertThat(stored().get(0).title()).isEqualTo("A");
  }

  @Test
  void skippedRowDoesNotMakeLaterOneDuplicate() {
    assertThat(
            importer.importCsv(
                H + row("https://a.example/", " ", "") + row("https://a.example/", "A", "")))
        .isEqualTo(new ImportResult(1, List.of(new SkippedRow(2, SkipReason.INVALID_TITLE))));
    assertThat(storedUrls()).containsExactly("https://a.example/");
  }

  @Test
  void rejectedFileImportsNothing() {
    assertThatThrownBy(
            () ->
                importer.importCsv(
                    H + "1,https://a.example/,A,,2024-01-01T00:00:00Z\r\n2,\"broken,B,,x\r\n"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThat(stored()).isEmpty();
  }

  @Test
  void rejectsNullCsv() {
    assertThatThrownBy(() -> importer.importCsv(null)).isInstanceOf(NullPointerException.class);
  }

  @Test
  void resultSkippedListIsUnmodifiable() {
    List<SkippedRow> skipped = importOne("1,a\r\n").skipped();
    assertThatThrownBy(() -> skipped.add(new SkippedRow(9, SkipReason.DUPLICATE)))
        .isInstanceOf(UnsupportedOperationException.class);
  }

  @Test
  void roundTripsExport() {
    List<Bookmark> originals =
        List.of(
            new Bookmark(
                1L, "https://a.example/x", "Hello, \"world\"", List.of("java", "news"),
                Instant.parse("2024-01-01T00:00:00.123456Z")),
            new Bookmark(
                2L, "https://b.example/", "two\r\nlines", List.of(),
                Instant.parse("2024-02-01T00:00:00Z")),
            new Bookmark(
                3L, "https://c.example/?q=1,2", "C", List.of("a"),
                Instant.parse("2024-03-01T00:00:00Z")));

    assertThat(importer.importCsv(BookmarkCsv.write(originals)))
        .isEqualTo(new ImportResult(3, List.of()));

    assertThat(stored().stream().map(BookmarkImporterTest::withoutId).toList())
        .containsExactlyInAnyOrderElementsOf(
            originals.stream().map(BookmarkImporterTest::withoutId).toList());
  }

  static List<Object> withoutId(Bookmark b) {
    return List.of(b.url(), b.title(), b.tags(), b.createdAt());
  }
}
