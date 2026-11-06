package org.jwcarman.bookmarks.web;

import org.jwcarman.bookmarks.service.BookmarkImporter;
import org.jwcarman.bookmarks.service.ImportResult;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** The bookmark import endpoint. */
@RestController
public class ImportController {

  private final BookmarkImporter importer;

  /** Creates the controller. */
  public ImportController(BookmarkImporter importer) {
    this.importer = importer;
  }

  /**
   * {@code POST /bookmarks/import} with a {@code text/csv} body: imports it with {@code
   * BookmarkImporter.importCsv} and returns 200 with the {@link ImportResult} as JSON, for example
   * {@code {"imported":2,"skipped":[{"line":3,"reason":"DUPLICATE"}]}}. 400 (problem detail, title
   * {@code Bad request}, from the existing {@code ProblemHandler}) when the importer throws {@link
   * IllegalArgumentException}.
   */
  @PostMapping(path = "/bookmarks/import", consumes = "text/csv")
  public ImportResult importCsv(@RequestBody String csv) {
    return importer.importCsv(csv);
  }
}
