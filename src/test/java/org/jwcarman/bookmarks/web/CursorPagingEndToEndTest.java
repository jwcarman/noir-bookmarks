package org.jwcarman.bookmarks.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.domain.BookmarkRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class CursorPagingEndToEndTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  @Autowired MockMvc mvc;
  @Autowired BookmarkRepository repository;

  @BeforeEach
  @AfterEach
  void clear() {
    repository.findAll(0, 1000).forEach(b -> repository.deleteById(b.id()));
  }

  Bookmark save(int minute, String title, String... tags) {
    return repository.save(
        new Bookmark(
            null,
            "https://example.com/" + minute,
            title,
            List.of(tags),
            NOON.plusSeconds(60L * minute)));
  }

  @Test
  void listPagesWithCursor() throws Exception {
    var b1 = save(1, "B1");
    var b2 = save(2, "B2");
    var b3 = save(3, "B3");

    String json =
        mvc.perform(get("/bookmarks").param("limit", "2"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.items.length()").value(2))
            .andExpect(jsonPath("$.items[0].id").value(b3.id()))
            .andExpect(jsonPath("$.items[1].id").value(b2.id()))
            .andExpect(jsonPath("$.next").isString())
            .andExpect(jsonPath("$.page").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.size").doesNotHaveJsonPath())
            .andExpect(jsonPath("$.total").doesNotHaveJsonPath())
            .andReturn()
            .getResponse()
            .getContentAsString();
    String next = JsonPath.read(json, "$.next");
    assertThat(next).matches("[A-Za-z0-9_-]+");

    mvc.perform(get("/bookmarks").param("limit", "2").param("after", next))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].id").value(b1.id()))
        .andExpect(jsonPath("$.next").hasJsonPath())
        .andExpect(jsonPath("$.next").value((Object) null));
  }

  @Test
  void defaultLimitIs20() throws Exception {
    for (int i = 0; i < 25; i++) {
      save(i, "B" + i);
    }
    mvc.perform(get("/bookmarks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(20))
        .andExpect(jsonPath("$.next").isString());
  }

  @Test
  void badLimitOrCursorIs400() throws Exception {
    mvc.perform(get("/bookmarks").param("limit", "101"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad request"));
    mvc.perform(get("/bookmarks").param("after", "!!"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad request"));
  }

  @Test
  void oldPagingParametersAreIgnored() throws Exception {
    save(1, "B1");
    save(2, "B2");
    save(3, "B3");
    mvc.perform(get("/bookmarks").param("page", "1").param("size", "1"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(3));
  }

  @Test
  void searchWithTextAndTags() throws Exception {
    save(1, "Java 1", "x");
    save(2, "Java 2", "x", "y");
    save(3, "Kotlin", "x");

    mvc.perform(
            get("/bookmarks/search").param("q", "java").param("tag", "x").param("tag", "y"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(1))
        .andExpect(jsonPath("$.items[0].title").value("Java 2"))
        .andExpect(jsonPath("$.next").value((Object) null));

    mvc.perform(get("/bookmarks/search").param("tag", "x").param("limit", "2"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items.length()").value(2))
        .andExpect(jsonPath("$.items[0].title").value("Kotlin"))
        .andExpect(jsonPath("$.items[1].title").value("Java 2"))
        .andExpect(jsonPath("$.next").isString());
  }

  @Test
  void searchBadLimitIs400() throws Exception {
    mvc.perform(get("/bookmarks/search").param("limit", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad request"));
  }
}
