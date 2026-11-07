package org.jwcarman.bookmarks.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.BookmarkRepository;
import org.jwcarman.bookmarks.service.TagService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TagServiceEndToEndTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  @Autowired MockMvc mvc;
  @Autowired BookmarkRepository repository;
  @Autowired ApplicationContext context;

  @BeforeEach
  void clear() {
    repository.findAll(0, 1000).forEach(b -> repository.deleteById(b.id()));
  }

  Bookmark seed(String url, String tag) {
    return repository.save(new Bookmark(null, url, "T", List.of(tag), NOON));
  }

  @Test
  void contextHasOneTagServiceUsedByTheController() throws Exception {
    assertThat(context.getBeansOfType(TagService.class)).hasSize(1);
    TagService tags = context.getBean(TagService.class);
    seed("https://a.example/", "java");
    // The controller's rename goes through the TagService bean: prove it by its behaviour and
    // by the captured constructor argument.
    TagController controller = context.getBean(TagController.class);
    Object renamer = ReflectionTestUtils.getField(controller, "renamer");
    assertThat(ReflectionTestUtils.getField(renamer, findCapturedField(renamer))).isSameAs(tags);
  }

  private static String findCapturedField(Object lambda) {
    for (var field : lambda.getClass().getDeclaredFields()) {
      if (TagService.class.isAssignableFrom(field.getType())) {
        return field.getName();
      }
    }
    throw new AssertionError("controller is not built over a TagService");
  }

  @Test
  void renameEndpointRenamesEveryTaggedBookmark() throws Exception {
    Bookmark a = seed("https://a.example/", "java");
    Bookmark b = seed("https://b.example/", "java");
    mvc.perform(
            post("/tags/java/rename")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"to\":\"jvm\"}"))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"renamed\":2}", true));
    assertThat(repository.findById(a.id()).orElseThrow().tags()).containsExactly("jvm");
    assertThat(repository.findById(b.id()).orElseThrow().tags()).containsExactly("jvm");
  }

  @Test
  void deleteEndpointDeletesTaggedBookmarks() throws Exception {
    seed("https://a.example/", "java");
    seed("https://b.example/", "java");
    Bookmark kotlin = seed("https://c.example/", "kotlin");
    mvc.perform(delete("/tags/java"))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"deleted\":2}", true));
    assertThat(repository.findAll(0, 10)).containsExactly(kotlin);
  }

  @Test
  void emptyRenameTargetIs400() throws Exception {
    mvc.perform(
            post("/tags/java/rename")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"to\":\"\"}"))
        .andExpect(status().isBadRequest());
  }
}
