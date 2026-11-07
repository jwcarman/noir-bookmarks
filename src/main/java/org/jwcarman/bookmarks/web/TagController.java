package org.jwcarman.bookmarks.web;

import jakarta.validation.Valid;
import java.util.function.ToIntBiFunction;
import java.util.function.ToIntFunction;
import org.jwcarman.bookmarks.service.BookmarkService;
import org.jwcarman.bookmarks.service.TagService;
import org.springframework.beans.factory.annotation.Autowired;
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

  private final ToIntBiFunction<String, String> renamer;
  private final ToIntFunction<String> deleter;

  /** Creates the controller over {@code tags}; this is the constructor Spring uses. */
  @Autowired
  public TagController(TagService tags) {
    this((from, to) -> tags.renameTag(from, to), tag -> tags.deleteTagged(tag));
  }

  /** Creates the controller over {@code service}'s tag operations. */
  public TagController(BookmarkService service) {
    this((from, to) -> service.renameTag(from, to), tag -> service.deleteTagged(tag));
  }

  private TagController(ToIntBiFunction<String, String> renamer, ToIntFunction<String> deleter) {
    this.renamer = renamer;
    this.deleter = deleter;
  }

  /**
   * {@code POST /tags/{tag}/rename}: renames {@code tag} with {@code TagService.renameTag} and
   * returns 200 with the number of bookmarks changed. 400 (problem detail) when the body is
   * invalid.
   */
  @PostMapping("/{tag}/rename")
  public RenameTagResponse rename(
      @PathVariable String tag, @Valid @RequestBody RenameTagRequest request) {
    return new RenameTagResponse(renamer.applyAsInt(tag, request.to()));
  }

  /**
   * {@code DELETE /tags/{tag}}: deletes with {@code TagService.deleteTagged} and returns 200 with
   * the number deleted.
   */
  @DeleteMapping("/{tag}")
  public DeleteTagResponse delete(@PathVariable String tag) {
    return new DeleteTagResponse(deleter.applyAsInt(tag));
  }
}
