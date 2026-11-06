package org.jwcarman.bookmarks.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class BookmarkCsvReaderTest {

  static final String H = "id,url,title,tags,createdAt\r\n";

  @Test
  void readsCrlfRecord() {
    assertThat(BookmarkCsvReader.read(H + "1,https://a.example/,A,x;y,2024-01-01T00:00:00Z\r\n"))
        .isEqualTo(
            List.of(
                new CsvRecord(
                    2, List.of("1", "https://a.example/", "A", "x;y", "2024-01-01T00:00:00Z"))));
  }

  @Test
  void readsLfRecordWithoutFinalLineEnding() {
    assertThat(
            BookmarkCsvReader.read(
                "id,url,title,tags,createdAt\n7,https://b.example/,B,,2024-01-02T00:00:00Z"))
        .isEqualTo(
            List.of(
                new CsvRecord(
                    2, List.of("7", "https://b.example/", "B", "", "2024-01-02T00:00:00Z"))));
  }

  @Test
  void headerOnlyIsEmpty() {
    assertThat(BookmarkCsvReader.read(H)).isEmpty();
    assertThat(BookmarkCsvReader.read("id,url,title,tags,createdAt")).isEmpty();
  }

  @Test
  void ignoresByteOrderMark() {
    assertThat(BookmarkCsvReader.read("﻿" + H)).isEmpty();
    assertThat(BookmarkCsvReader.read("﻿" + H + "a\r\n"))
        .isEqualTo(List.of(new CsvRecord(2, List.of("a"))));
  }

  @Test
  void byteOrderMarkOnlyAtStart() {
    assertThatThrownBy(() -> BookmarkCsvReader.read("﻿﻿" + H))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BookmarkCsvReader.read("﻿"))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void unquotesQuotedFields() {
    List<CsvRecord> records =
        BookmarkCsvReader.read(
            H + "1,https://a.example/,\"Hello, \"\"world\"\"\",,2024-01-01T00:00:00Z\r\n");
    assertThat(records).hasSize(1);
    assertThat(records.get(0).fields())
        .containsExactly(
            "1", "https://a.example/", "Hello, \"world\"", "", "2024-01-01T00:00:00Z");
  }

  @Test
  void countsLineBreaksInQuotedFields() {
    List<CsvRecord> records =
        BookmarkCsvReader.read(
            H
                + "1,https://a.example/,\"two\r\nlines\",,2024-01-01T00:00:00Z\r\n"
                + "2,https://b.example/,B,,2024-01-01T00:00:00Z\r\n");
    assertThat(records).hasSize(2);
    assertThat(records.get(0).line()).isEqualTo(2);
    assertThat(records.get(0).fields().get(2)).isEqualTo("two\r\nlines");
    assertThat(records.get(1).line()).isEqualTo(4);
    assertThat(records.get(1).fields().get(0)).isEqualTo("2");
  }

  @Test
  void quotedFieldWithLfAndManyLines() {
    List<CsvRecord> records = BookmarkCsvReader.read(H + "\"a\n\nb\",x\nc\n");
    assertThat(records)
        .containsExactly(
            new CsvRecord(2, List.of("a\n\nb", "x")), new CsvRecord(5, List.of("c")));
  }

  @Test
  void emptyQuotedField() {
    List<CsvRecord> records = BookmarkCsvReader.read(H + "\"\",https://a.example/,A,,x\r\n");
    assertThat(records.get(0).fields()).hasSize(5);
    assertThat(records.get(0).fields().get(0)).isEmpty();
  }

  @Test
  void quotedFieldsAtEndOfRecordAndInput() {
    assertThat(BookmarkCsvReader.read(H + "a,\"b\"\r\n\"c\"\n\"d\""))
        .containsExactly(
            new CsvRecord(2, List.of("a", "b")),
            new CsvRecord(3, List.of("c")),
            new CsvRecord(4, List.of("d")));
  }

  @Test
  void quotedFieldOfOnlyEscapedQuote() {
    assertThat(BookmarkCsvReader.read(H + "\"\"\"\""))
        .containsExactly(new CsvRecord(2, List.of("\"")));
  }

  @Test
  void quotedHeaderFieldsAreUnquoted() {
    assertThat(BookmarkCsvReader.read("\"id\",url,title,tags,createdAt\r\n")).isEmpty();
  }

  @Test
  void skipsButCountsEmptyLines() {
    List<CsvRecord> records =
        BookmarkCsvReader.read(H + "\r\n1,https://a.example/, A ,,x\r\n\r\n");
    assertThat(records).hasSize(1);
    assertThat(records.get(0).line()).isEqualTo(3);
    assertThat(records.get(0).fields().get(2)).isEqualTo(" A ");
  }

  @Test
  void skipsEmptyLinesBeforeHeaderAndLfEmptyLines() {
    assertThat(BookmarkCsvReader.read("\n\r\n" + H + "\n\na\n"))
        .containsExactly(new CsvRecord(6, List.of("a")));
  }

  @Test
  void recordsMayHaveAnyFieldCount() {
    assertThat(BookmarkCsvReader.read(H + "a,b\r\n"))
        .isEqualTo(List.of(new CsvRecord(2, List.of("a", "b"))));
  }

  @Test
  void trailingCommaGivesEmptyField() {
    assertThat(BookmarkCsvReader.read(H + "a,\r\nb,"))
        .containsExactly(
            new CsvRecord(2, List.of("a", "")), new CsvRecord(3, List.of("b", "")));
  }

  @Test
  void loneCarriageReturnAndQuotesInUnquotedFieldAreKept() {
    assertThat(BookmarkCsvReader.read(H + "a\rb,c\"d\r\n"))
        .containsExactly(new CsvRecord(2, List.of("a\rb", "c\"d")));
  }

  @Test
  void lineWithOnlySpacesIsARecord() {
    assertThat(BookmarkCsvReader.read(H + " \r\n"))
        .containsExactly(new CsvRecord(2, List.of(" ")));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "",
        "\r\n\n",
        "url,title\r\n",
        "ID,url,title,tags,createdAt\r\n",
        "id,url,title,tags\r\n",
        "id,url,title,tags,createdAt,extra\r\n",
        " id,url,title,tags,createdAt\r\n",
        "id,url,title,tags,createdAt \r\n",
        "id,url,title,tags,createdAt\r\n1,\"https://a.example/,A,,x\r\n",
        "id,url,title,tags,createdAt\r\n1,\"a\"b,A,,x\r\n",
        "id,url,title,tags,createdAt\r\n\"a\" ,b\r\n",
        "id,url,title,tags,createdAt\r\n\"a\"\rb\r\n",
        "id,url,title,tags,createdAt\r\n\"a\"\r",
        "id,url,title,tags,createdAt\r\n\"",
        "id,url,title,tags,createdAt\r\n\"a\"\"",
        "\"id,url,title,tags,createdAt\r\n"
      })
  void rejectsMalformedInput(String csv) {
    assertThatThrownBy(() -> BookmarkCsvReader.read(csv))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void rejectsMoreThanMaxRecords() {
    assertThat(BookmarkCsvReader.MAX_RECORDS).isEqualTo(1000);
    assertThatThrownBy(() -> BookmarkCsvReader.read(H + "a\r\n".repeat(1001)))
        .isInstanceOf(IllegalArgumentException.class);
  }

  @Test
  void acceptsExactlyMaxRecords() {
    List<CsvRecord> records = BookmarkCsvReader.read(H + "a\r\n".repeat(1000));
    assertThat(records).hasSize(1000);
    assertThat(records.get(999)).isEqualTo(new CsvRecord(1001, List.of("a")));
  }

  @Test
  void emptyLinesDoNotCountAsRecords() {
    assertThat(BookmarkCsvReader.read(H + "a\r\n\r\n".repeat(1000))).hasSize(1000);
  }

  @Test
  void rejectsNull() {
    assertThatThrownBy(() -> BookmarkCsvReader.read(null))
        .isInstanceOf(NullPointerException.class);
  }
}
