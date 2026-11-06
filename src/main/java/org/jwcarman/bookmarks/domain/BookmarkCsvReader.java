package org.jwcarman.bookmarks.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/** Reads CSV in the format {@link BookmarkCsv#write} produces. */
public final class BookmarkCsvReader {

  /** The largest number of data records {@link #read} accepts. */
  public static final int MAX_RECORDS = 1000;

  private static final List<String> OLD_HEADER =
      List.of("id", "url", "title", "tags", "createdAt");
  private static final List<String> HEADER =
      List.of("id", "url", "title", "tags", "createdAt", "notes");
  private static final char BOM = '﻿';

  private BookmarkCsvReader() {}

  /**
   * Parses {@code csv} and returns its data records (every record after the header), in order.
   *
   * <p>Format: records end with {@code "\r\n"} or {@code "\n"}; the last record may have no line
   * ending. Fields are separated by {@code ,}. A field that starts with {@code "} is quoted: it
   * ends at the next {@code "} that is not doubled, may contain commas, {@code "\r"} and {@code
   * "\n"}, and {@code ""} inside it stands for one {@code "}. An unquoted field is taken as it is,
   * with no stripping. A line that is empty (outside a quoted field) is not a record and is
   * skipped, but still counts for line numbers. A UTF-8 byte order mark ({@code U+FEFF}) at the
   * very start is ignored. The first record must be exactly one of the headers {@code
   * id,url,title,tags,createdAt} (an export from before notes existed) or {@code
   * id,url,title,tags,createdAt,notes}. Data records are not checked for their number of fields.
   *
   * <p>A record's {@link CsvRecord#line()} is the physical line it starts on: 1 plus the number of
   * {@code "\n"} before it, including those inside quoted fields and in skipped empty lines.
   *
   * @throws NullPointerException if {@code csv} is {@code null}
   * @throws IllegalArgumentException if there is no header or it is neither of the two headers; if a quoted field is not closed; if a closing {@code "} is
   *     followed by anything other than {@code ,}, a line ending or the end of the input; or if
   *     there are more than {@value #MAX_RECORDS} data records
   */
  public static List<CsvRecord> read(String csv) {
    Objects.requireNonNull(csv, "csv");
    return new Parser(csv).parse();
  }

  private static final class Parser {
    private final String in;
    private int pos;
    private int line = 1;

    Parser(String in) {
      this.in = in;
      this.pos = !in.isEmpty() && in.charAt(0) == BOM ? 1 : 0;
    }

    List<CsvRecord> parse() {
      skipEmptyLines();
      List<String> header = parseRecord();
      if (!HEADER.equals(header) && !OLD_HEADER.equals(header)) {
        throw new IllegalArgumentException(
            "CSV must start with the header " + HEADER + " or " + OLD_HEADER);
      }
      List<CsvRecord> records = new ArrayList<>();
      skipEmptyLines();
      while (!atEnd()) {
        if (records.size() == MAX_RECORDS) {
          throw new IllegalArgumentException("CSV has more than " + MAX_RECORDS + " records");
        }
        int start = line;
        records.add(new CsvRecord(start, parseRecord()));
        skipEmptyLines();
      }
      return records;
    }

    private void skipEmptyLines() {
      while (atLineEnd()) {
        consumeLineEnd();
      }
    }

    private List<String> parseRecord() {
      List<String> fields = new ArrayList<>();
      fields.add(parseField());
      while (!atEnd() && in.charAt(pos) == ',') {
        pos++;
        fields.add(parseField());
      }
      return List.copyOf(fields);
    }

    private String parseField() {
      if (!atEnd() && in.charAt(pos) == '"') {
        return parseQuoted();
      }
      int start = pos;
      while (!atEnd() && in.charAt(pos) != ',' && !atLineEnd()) {
        pos++;
      }
      return in.substring(start, pos);
    }

    private String parseQuoted() {
      int openLine = line;
      pos++;
      StringBuilder sb = new StringBuilder();
      while (true) {
        if (atEnd()) {
          throw new IllegalArgumentException("Unclosed quoted field starting on line " + openLine);
        }
        char c = in.charAt(pos++);
        if (c == '"' && !isAt('"')) {
          checkAfterClosingQuote();
          return sb.toString();
        }
        if (c == '"') {
          pos++;
        } else if (c == '\n') {
          line++;
        }
        sb.append(c);
      }
    }

    private void checkAfterClosingQuote() {
      if (!atEnd() && !isAt(',') && !atLineEnd()) {
        throw new IllegalArgumentException("Unexpected character after quote on line " + line);
      }
    }

    private boolean atEnd() {
      return pos >= in.length();
    }

    private boolean isAt(char c) {
      return !atEnd() && in.charAt(pos) == c;
    }

    private boolean atLineEnd() {
      return isAt('\n') || (isAt('\r') && pos + 1 < in.length() && in.charAt(pos + 1) == '\n');
    }

    /** Consumes the line ending at {@code pos}; only called when {@link #atLineEnd()}. */
    private void consumeLineEnd() {
      pos += isAt('\r') ? 2 : 1;
      line++;
    }
  }
}
