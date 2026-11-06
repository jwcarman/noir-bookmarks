package org.jwcarman.bookmarks.service;

import java.util.List;

/**
 * What an import did.
 *
 * @param imported how many bookmarks were saved
 * @param skipped the rows that were not saved, in line order
 */
public record ImportResult(int imported, List<SkippedRow> skipped) {}
