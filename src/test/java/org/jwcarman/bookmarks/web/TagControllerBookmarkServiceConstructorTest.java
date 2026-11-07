package org.jwcarman.bookmarks.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import org.jwcarman.bookmarks.service.BookmarkService;
import org.jwcarman.bookmarks.service.TagService;
import org.junit.jupiter.api.Test;

class TagControllerBookmarkServiceConstructorTest {

  @Test
  void oldConstructorDelegatesToBookmarkService() {
    BookmarkService service = mock(BookmarkService.class);
    given(service.renameTag("js", "javascript")).willReturn(3);
    given(service.deleteTagged("java")).willReturn(2);
    TagController controller = new TagController(service);
    assertThat(controller.rename("js", new RenameTagRequest("javascript")).renamed()).isEqualTo(3);
    assertThat(controller.delete("java").deleted()).isEqualTo(2);
  }

  @Test
  void newConstructorDelegatesToTagService() {
    TagService tags = mock(TagService.class);
    given(tags.renameTag("js", "javascript")).willReturn(4);
    given(tags.deleteTagged("java")).willReturn(5);
    TagController controller = new TagController(tags);
    assertThat(controller.rename("js", new RenameTagRequest("javascript")).renamed()).isEqualTo(4);
    assertThat(controller.delete("java").deleted()).isEqualTo(5);
  }
}
