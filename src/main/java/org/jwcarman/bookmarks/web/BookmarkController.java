package org.jwcarman.bookmarks.web;

import jakarta.validation.Valid;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;
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

  /**
   * {@code POST /bookmarks}: creates the bookmark with {@code BookmarkService.create(url, title,
   * tags, notes)} and returns 201 with it. 400 (problem detail) when notes are longer than 2000
   * characters.
   */
  @PostMapping
  public ResponseEntity<BookmarkResponse> create(@Valid @RequestBody CreateBookmarkRequest request) {
    var saved = service.create(request.url(), request.title(), request.tags(), request.notes());
    var location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(saved.id())
            .toUri();
    return ResponseEntity.created(location).body(BookmarkResponse.from(saved));
  }

  /**
   * No longer mapped to any request: {@code GET /bookmarks} is served by {@link #list(String,
   * int)}. Kept only because noir keeps existing public API.
   */
  public BookmarkPageResponse list(int page, int size) {
    return BookmarkPageResponse.from(service.list(page, size));
  }

  /**
   * {@code GET /bookmarks?limit=&after=}: returns 200 with {@code BookmarkService.listAfter(after,
   * limit)} as a {@link CursorPageResponse}. {@code limit} defaults to 20; {@code after} is
   * optional. The old {@code page} and {@code size} parameters are ignored. 400 (problem detail,
   * title {@code Bad request}) for a limit out of range or an invalid cursor.
   */
  @GetMapping
  public CursorPageResponse list(
      @RequestParam(required = false) String after,
      @RequestParam(defaultValue = "20") int limit) {
    return CursorPageResponse.from(service.listAfter(after, limit));
  }

  /**
   * {@code GET /bookmarks/search?q=&tag=&tag=&limit=&after=}: returns 200 with {@code
   * BookmarkService.searchAfter(q, tags, after, limit)} as a {@link CursorPageResponse}. {@code q}
   * defaults to {@code ""}; {@code tag} may be repeated and defaults to none; {@code limit} defaults
   * to 20. 400 (problem detail, title {@code Bad request}) for a limit out of range or an invalid
   * cursor.
   */
  @GetMapping("/search")
  public CursorPageResponse search(
      @RequestParam(defaultValue = "") String q,
      @RequestParam(required = false) List<String> tag,
      @RequestParam(required = false) String after,
      @RequestParam(defaultValue = "20") int limit) {
    Set<String> tags = tag == null ? Set.of() : Set.copyOf(tag);
    return CursorPageResponse.from(service.searchAfter(q, tags, after, limit));
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
   * {@code PUT /bookmarks/{id}}: applies the request with {@code BookmarkService.update(id, title,
   * tags, notes)} and returns 200 with the updated bookmark. 404 (problem detail, title {@code
   * Bookmark not found}) when there is no such bookmark; 400 (problem detail) when the body is
   * invalid, including notes longer than 2000 characters.
   */
  @PutMapping("/{id}")
  public BookmarkResponse update(
      @PathVariable long id, @Valid @RequestBody UpdateBookmarkRequest request) {
    return BookmarkResponse.from(service.update(id, request.title(), request.tags(), request.notes()));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
