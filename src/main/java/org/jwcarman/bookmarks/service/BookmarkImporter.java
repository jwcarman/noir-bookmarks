package org.jwcarman.bookmarks.service;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.BookmarkCsvReader;
import org.jwcarman.bookmarks.domain.BookmarkRepository;
import org.jwcarman.bookmarks.domain.CsvRecord;
import org.jwcarman.bookmarks.domain.UrlNormalizer;
import org.springframework.stereotype.Service;

/** Imports bookmarks from CSV in the export format. */
@Service
public class BookmarkImporter {

  private static final int OLD_FIELD_COUNT = 5;
  private static final int FIELD_COUNT = 6;
  private static final int MAX_URL_LENGTH = 2048;
  private static final int MAX_TITLE_LENGTH = 200;
  private static final int MAX_TAGS = 10;
  private static final int MAX_TAG_LENGTH = 50;

  private final BookmarkRepository repository;

  /**
   * Creates an importer that saves to {@code repository}.
   *
   * @throws NullPointerException if {@code repository} is {@code null}
   */
  public BookmarkImporter(BookmarkRepository repository) {
    this.repository = Objects.requireNonNull(repository, "repository");
  }

  /**
   * Parses {@code csv} with {@code BookmarkCsvReader.read} and saves one bookmark for each valid
   * data record, in file order. Nothing is saved when {@code read} throws.
   *
   * <p>A record's fields are {@code id,url,title,tags,createdAt}. {@code id} is ignored: every
   * saved bookmark gets a new id. The URL is stored in canonical form ({@code
   * UrlNormalizer.normalize}). The title is stored as it is. {@code tags} is split on {@code ;} (an
   * empty field means no tags) and the tags are cleaned as when a bookmark is created (stripped,
   * lowercased with {@code Locale.ROOT}, blanks and later duplicates dropped). {@code createdAt} is
   * parsed with {@link java.time.Instant#parse} and stored truncated to microseconds.
   *
   * <p>A record is skipped instead of saved for the first of these that applies, checked in this
   * order: it does not have exactly 5 fields ({@link SkipReason#WRONG_FIELD_COUNT}); {@code
   * UrlNormalizer.normalize} rejects the URL, or the URL as given is longer than 2048 characters
   * ({@link SkipReason#INVALID_URL}); the title is blank or longer than 200 characters ({@link
   * SkipReason#INVALID_TITLE}); more than 10 tags remain after cleaning, or a cleaned tag is longer
   * than 50 characters ({@link SkipReason#INVALID_TAGS}); {@code createdAt} does not parse ({@link
   * SkipReason#INVALID_CREATED_AT}); a bookmark with the same canonical URL is already saved or was
   * saved earlier in this import ({@link SkipReason#DUPLICATE}).
   *
   * <p>A record is valid with 5 fields ({@code id,url,title,tags,createdAt}; the bookmark gets empty
   * notes) or 6 fields (the sixth is the notes); any other number of fields is {@link
   * SkipReason#WRONG_FIELD_COUNT}, whichever header the file has. Notes are stored as given, except
   * that notes longer than 2000 characters are cut to their first 2000 characters (the row is not
   * skipped).
   *
   * @return how many bookmarks were saved, and the skipped rows in line order
   * @throws NullPointerException if {@code csv} is {@code null}
   * @throws IllegalArgumentException if {@code BookmarkCsvReader.read} rejects {@code csv}
   */
  public ImportResult importCsv(String csv) {
    List<CsvRecord> records = BookmarkCsvReader.read(csv);
    int imported = 0;
    List<SkippedRow> skipped = new ArrayList<>();
    for (CsvRecord record : records) {
      SkipReason reason = importRecord(record.fields());
      if (reason == null) {
        imported++;
      } else {
        skipped.add(new SkippedRow(record.line(), reason));
      }
    }
    return new ImportResult(imported, List.copyOf(skipped));
  }

  /** Saves the bookmark {@code fields} describe; returns why not, or {@code null} when saved. */
  private SkipReason importRecord(List<String> fields) {
    if (fields.size() != OLD_FIELD_COUNT && fields.size() != FIELD_COUNT) {
      return SkipReason.WRONG_FIELD_COUNT;
    }
    String url = canonicalUrl(fields.get(1));
    if (url == null) {
      return SkipReason.INVALID_URL;
    }
    String title = fields.get(2);
    if (title.isBlank() || title.length() > MAX_TITLE_LENGTH) {
      return SkipReason.INVALID_TITLE;
    }
    List<String> tags = cleanTags(fields.get(3));
    if (tags == null) {
      return SkipReason.INVALID_TAGS;
    }
    Instant createdAt = parseInstant(fields.get(4));
    if (createdAt == null) {
      return SkipReason.INVALID_CREATED_AT;
    }
    if (repository.findByUrl(url).isPresent()) {
      return SkipReason.DUPLICATE;
    }
    repository.save(new Bookmark(null, url, title, tags, createdAt, notes(fields)));
    return null;
  }

  private static String notes(List<String> fields) {
    if (fields.size() < FIELD_COUNT) {
      return "";
    }
    String notes = fields.get(FIELD_COUNT - 1);
    return notes.substring(0, Math.min(notes.length(), Bookmark.MAX_NOTES_LENGTH));
  }

  private static String canonicalUrl(String url) {
    if (url.length() > MAX_URL_LENGTH) {
      return null;
    }
    try {
      return UrlNormalizer.normalize(url);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }

  private static List<String> cleanTags(String field) {
    LinkedHashSet<String> cleaned = new LinkedHashSet<>();
    for (String tag : field.split(";")) {
      String clean = tag.strip().toLowerCase(Locale.ROOT);
      if (clean.length() > MAX_TAG_LENGTH) {
        return null;
      }
      if (!clean.isEmpty()) {
        cleaned.add(clean);
      }
    }
    return cleaned.size() > MAX_TAGS ? null : List.copyOf(cleaned);
  }

  private static Instant parseInstant(String value) {
    try {
      return Instant.parse(value).truncatedTo(ChronoUnit.MICROS);
    } catch (DateTimeParseException e) {
      return null;
    }
  }
}
