package org.jwcarman.bookmarks.service;

/**
 * A row the import skipped.
 *
 * @param line the line the row starts on ({@link org.jwcarman.bookmarks.domain.CsvRecord#line()})
 * @param reason why it was skipped
 */
public record SkippedRow(int line, SkipReason reason) {}
