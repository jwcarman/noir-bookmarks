package org.jwcarman.bookmarks.web;

import jakarta.validation.Valid;
import org.jwcarman.bookmarks.service.BookmarkService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/** The bookmarks REST API. */
@RestController
@RequestMapping("/bookmarks")
public class BookmarkController {

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

  @GetMapping("/{id}")
  public BookmarkResponse get(@PathVariable long id) {
    return BookmarkResponse.from(service.get(id));
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(@PathVariable long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }
}
