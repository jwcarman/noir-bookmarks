package org.jwcarman.bookmarks.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;

class BookmarkCsvNotesTest {

  static final Instant T = Instant.parse("2024-01-01T00:00:00Z");

  @Test
  void notesAreTheLastColumnAndQuotedWhenNeeded() {
    var b = new Bookmark(1L, "https://a.example/", "A", List.of("x"), T, "say \"hi\", then go");
    assertThat(BookmarkCsv.write(List.of(b)))
        .isEqualTo(
            "id,url,title,tags,createdAt,notes\r\n"
                + "1,https://a.example/,A,x,2024-01-01T00:00:00Z,\"say \"\"hi\"\", then go\"\r\n");
  }

  @Test
  void plainNotesAreNotQuoted() {
    var b = new Bookmark(1L, "https://a.example/", "A", List.of(), T, "why not");
    assertThat(BookmarkCsv.write(List.of(b))).endsWith(",why not\r\n");
  }

  @Test
  void emptyNotesAreAnEmptyLastField() {
    var b = new Bookmark(1L, "https://a.example/", "A", List.of(), T, "");
    assertThat(BookmarkCsv.write(List.of(b)))
        .endsWith("2024-01-01T00:00:00Z,\r\n")
        .endsWith(",\r\n");
  }

  @Test
  void writtenNotesReadBack() {
    var b = new Bookmark(1L, "https://a.example/", "A", List.of(), T, "a,b\nc\r\"d\"");
    List<CsvRecord> records = BookmarkCsvReader.read(BookmarkCsv.write(List.of(b)));
    assertThat(records).hasSize(1);
    assertThat(records.get(0).fields()).hasSize(6).last().isEqualTo("a,b\nc\r\"d\"");
  }

  @Test
  void readerAcceptsNotesHeader() {
    List<CsvRecord> records =
        BookmarkCsvReader.read(
            "id,url,title,tags,createdAt,notes\r\n1,https://a.example/,A,,2024-01-01T00:00:00Z,why\r\n");
    assertThat(records).hasSize(1);
    assertThat(records.get(0).line()).isEqualTo(2);
    assertThat(records.get(0).fields())
        .containsExactly("1", "https://a.example/", "A", "", "2024-01-01T00:00:00Z", "why");
  }

  @Test
  void readerAcceptsOldHeader() {
    List<CsvRecord> records =
        BookmarkCsvReader.read(
            "id,url,title,tags,createdAt\r\n1,https://a.example/,A,,2024-01-01T00:00:00Z\r\n");
    assertThat(records).hasSize(1);
    assertThat(records.get(0).fields()).hasSize(5);
  }

  @Test
  void readerAcceptsNotesHeaderAlone() {
    assertThat(BookmarkCsvReader.read("id,url,title,tags,createdAt,notes\r\n")).isEmpty();
  }

  @Test
  void readerRejectsOtherHeaders() {
    assertThatThrownBy(() -> BookmarkCsvReader.read("id,url,title,tags,createdAt,note\r\n"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BookmarkCsvReader.read("id,url,title,tags,createdAt,notes,x\r\n"))
        .isInstanceOf(IllegalArgumentException.class);
    assertThatThrownBy(() -> BookmarkCsvReader.read("id,url,title,tags\r\n"))
        .isInstanceOf(IllegalArgumentException.class);
  }
}
