package org.jwcarman.bookmarks.domain;

import java.util.List;

/**
 * One record of a CSV file.
 *
 * @param line the 1-based physical line on which the record starts (the header is line 1)
 * @param fields the record's fields, unquoted, in order
 */
public record CsvRecord(int line, List<String> fields) {}
