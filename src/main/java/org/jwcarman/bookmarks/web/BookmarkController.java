package org.jwcarman.bookmarks.web;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import org.jwcarman.bookmarks.domain.BookmarkCsv;
import org.jwcarman.bookmarks.service.BookmarkService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** The bookmarks REST API. */
@RestController
@RequestMapping("/bookmarks")
public class BookmarkController {

  private static final MediaType CSV = new MediaType("text", "csv", StandardCharsets.UTF_8);

  private final BookmarkService service;

  public BookmarkController(BookmarkService service) {
    this.service = service;
  }

  @PostMapping
  public ResponseEntity<BookmarkResponse> create(@Valid @RequestBody CreateBookmarkRequest request) {
    var saved = service.create(request.url(), request.title(), request.tags());
    var location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(saved.id())
            .toUri();
    return ResponseEntity.created(location).body(BookmarkResponse.from(saved));
  }

  @GetMapping
  public BookmarkPageResponse list(
      @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
    return BookmarkPageResponse.from(service.list(page, size));
  }

  /**
   * {@code GET /bookmarks/export.csv}: returns 200 with {@code BookmarkCsv.write(service.all())},
   * content type {@code text/csv;charset=UTF-8} and header {@code Content-Disposition: attachment;
   * filename="bookmarks.csv"}.
   */
  @GetMapping("/export.csv")
  public ResponseEntity<String> export() {
    return ResponseEntity.ok()
        .contentType(CSV)
        .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bookmarks.csv\"")
        .body(BookmarkCsv.write(service.all()));
  }

  @GetMapping("/{id}")
  public BookmarkResponse get(@PathVariable long id) {
    return BookmarkResponse.from(service.get(id));
  }

  /**
   * {@code PUT /bookmarks/{id}}: applies the request with {@code BookmarkService.update} and
   * returns 200 with the updated bookmark. 404 (problem detail, title {@code Bookmark not found})
   * when there is no such bookmark; 400 (problem detail) when the body is invalid.
   */
  @PutMapping("/{id}")
  public BookmarkResponse update(
      @PathVariable long id, @Valid @RequestBody UpdateBookmarkRequest request) {
    return BookmarkResponse.from(service.update(id, request.title(), request.tags()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
