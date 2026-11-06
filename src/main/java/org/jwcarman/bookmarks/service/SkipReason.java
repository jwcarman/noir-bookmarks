package org.jwcarman.bookmarks.service;

/** Why an import row was skipped. */
public enum SkipReason {
  /** The row does not have exactly 5 fields. */
  WRONG_FIELD_COUNT,
  /** The URL is not an absolute http or https URL, or is longer than 2048 characters. */
  INVALID_URL,
  /** The title is blank or longer than 200 characters. */
  INVALID_TITLE,
  /** More than 10 tags remain after cleaning, or a cleaned tag is longer than 50 characters. */
  INVALID_TAGS,
  /** {@code createdAt} is not an instant that {@link java.time.Instant#parse} accepts. */
  INVALID_CREATED_AT,
  /** A bookmark with the same canonical URL is already saved, or was saved earlier in the file. */
  DUPLICATE
}
