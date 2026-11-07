package org.jwcarman.bookmarks.web;

import java.util.List;
import org.jwcarman.bookmarks.service.CollectionService;
import org.jwcarman.bookmarks.service.CollectionSummary;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

/**
 * The collections REST API. Bodies are JSON. A collection is returned as {@code
 * {"id":1,"name":"Work","bookmarks":[...]}}, each bookmark as in {@code GET /bookmarks/{id}}. 404
 * (problem detail, title {@code Collection not found}) for an unknown collection, 404 (title {@code
 * Bookmark not found}) for an unknown bookmark, 409 (title {@code Duplicate collection}) for a
 * duplicate name, 400 (title {@code Bad request}) for an invalid name or body.
 *
 * <ul>
 *   <li>{@code POST /collections} with {@code {"name":"Work"}}: 201 with the collection and a
 *       {@code Location} header {@code /collections/<id>}.
 *   <li>{@code GET /collections}: 200 with {@code [{"id":1,"name":"Work","size":2},...]}.
 *   <li>{@code GET /collections/{id}}: 200 with the collection.
 *   <li>{@code PUT /collections/{id}} with {@code {"name":"Job"}}: 200 with the renamed collection.
 *   <li>{@code DELETE /collections/{id}}: 204.
 *   <li>{@code PUT /collections/{id}/bookmarks/{bookmarkId}}: 200 with the collection.
 *   <li>{@code DELETE /collections/{id}/bookmarks/{bookmarkId}}: 200 with the collection.
 * </ul>
 */
@RestController
@RequestMapping("/collections")
public class CollectionController {

  private final CollectionService service;

  /** Creates the controller. */
  public CollectionController(CollectionService service) {
    this.service = service;
  }

  @PostMapping
  ResponseEntity<CollectionResponse> create(@RequestBody CollectionNameRequest request) {
    var created = CollectionResponse.from(service.create(nameOf(request)));
    var location =
        ServletUriComponentsBuilder.fromCurrentRequest()
            .path("/{id}")
            .buildAndExpand(created.id())
            .toUri();
    return ResponseEntity.created(location).body(created);
  }

  @GetMapping
  List<CollectionSummary> list() {
    return service.list();
  }

  @GetMapping("/{id}")
  CollectionResponse get(@PathVariable long id) {
    return CollectionResponse.from(service.get(id));
  }

  @PutMapping("/{id}")
  CollectionResponse rename(@PathVariable long id, @RequestBody CollectionNameRequest request) {
    return CollectionResponse.from(service.rename(id, nameOf(request)));
  }

  @DeleteMapping("/{id}")
  ResponseEntity<Void> delete(@PathVariable long id) {
    service.delete(id);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/{id}/bookmarks/{bookmarkId}")
  CollectionResponse add(@PathVariable long id, @PathVariable long bookmarkId) {
    return CollectionResponse.from(service.add(id, bookmarkId));
  }

  @DeleteMapping("/{id}/bookmarks/{bookmarkId}")
  CollectionResponse remove(@PathVariable long id, @PathVariable long bookmarkId) {
    return CollectionResponse.from(service.remove(id, bookmarkId));
  }

  /** 400 with title {@code Bad request} for a body that is not valid JSON. */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  ProblemDetail unreadable(HttpMessageNotReadableException e) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Invalid request body");
    problem.setTitle("Bad request");
    return problem;
  }

  private static String nameOf(CollectionNameRequest request) {
    if (request.name() == null) {
      throw new IllegalArgumentException("name is required");
    }
    return request.name();
  }
}
