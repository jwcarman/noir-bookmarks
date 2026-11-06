package org.jwcarman.bookmarks.web;

import jakarta.validation.Valid;
import org.jwcarman.bookmarks.service.BookmarkService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Tag-wide operations. */
@RestController
@RequestMapping("/tags")
public class TagController {

  private final BookmarkService service;

  public TagController(BookmarkService service) {
    this.service = service;
  }

  /**
   * {@code POST /tags/{tag}/rename}: renames {@code tag} with {@code BookmarkService.renameTag} and
   * returns 200 with the number of bookmarks changed. 400 (problem detail) when the body is
   * invalid.
   */
  @PostMapping("/{tag}/rename")
  public RenameTagResponse rename(
      @PathVariable String tag, @Valid @RequestBody RenameTagRequest request) {
    return new RenameTagResponse(service.renameTag(tag, request.to()));
  }

  /**
   * {@code DELETE /tags/{tag}}: deletes with {@code BookmarkService.deleteTagged} and returns 200
   * with the number deleted.
   */
  @DeleteMapping("/{tag}")
  public DeleteTagResponse delete(@PathVariable String tag) {
    return new DeleteTagResponse(service.deleteTagged(tag));
  }
}
