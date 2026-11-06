package org.jwcarman.bookmarks.web;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.service.BookmarkNotFoundException;
import org.jwcarman.bookmarks.service.BookmarkPage;
import org.jwcarman.bookmarks.service.BookmarkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookmarkController.class)
class BookmarkControllerTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");
  static final Bookmark SAVED = new Bookmark(7L, "https://a.example", "A", List.of("x"), NOON);

  @Autowired MockMvc mvc;
  @MockitoBean BookmarkService service;

  @Test
  void createReturns201WithLocation() throws Exception {
    given(service.create("https://a.example", "A", List.of("x"))).willReturn(SAVED);
    mvc.perform(
            post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://a.example\",\"title\":\"A\",\"tags\":[\"x\"]}"))
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/bookmarks/7"))
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.tags[0]").value("x"))
        .andExpect(jsonPath("$.createdAt").value("2026-11-06T12:00:00Z"));
  }

  @Test
  void createWithoutTagsUsesAnEmptyList() throws Exception {
    given(service.create("https://a.example", "A", List.of())).willReturn(SAVED);
    mvc.perform(
            post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://a.example\",\"title\":\"A\"}"))
        .andExpect(status().isCreated());
  }

  @Test
  void createWithBlankUrlIs400() throws Exception {
    mvc.perform(
            post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\" \",\"title\":\"A\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void getReturnsTheBookmark() throws Exception {
    given(service.get(7)).willReturn(SAVED);
    mvc.perform(get("/bookmarks/7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.url").value("https://a.example"));
  }

  @Test
  void getMissingIs404Problem() throws Exception {
    given(service.get(anyLong())).willThrow(new BookmarkNotFoundException(9));
    mvc.perform(get("/bookmarks/9"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Bookmark not found"))
        .andExpect(jsonPath("$.detail").value("Bookmark 9 not found"));
  }

  @Test
  void deleteReturns204() throws Exception {
    mvc.perform(delete("/bookmarks/7")).andExpect(status().isNoContent());
    verify(service).delete(7);
  }

  @Test
  void deleteMissingIs404() throws Exception {
    willThrow(new BookmarkNotFoundException(9)).given(service).delete(9);
    mvc.perform(delete("/bookmarks/9"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Bookmark not found"));
  }

  @Test
  void listUsesDefaults() throws Exception {
    given(service.list(0, 20)).willReturn(new BookmarkPage(List.of(SAVED), 0, 20, 1));
    mvc.perform(get("/bookmarks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(7))
        .andExpect(jsonPath("$.total").value(1))
        .andExpect(jsonPath("$.size").value(20));
  }

  @Test
  void badPagingIsAProblem() throws Exception {
    given(service.list(0, 500)).willThrow(new IllegalArgumentException("size must be from 1 to 100: 500"));
    mvc.perform(get("/bookmarks").param("size", "500"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Bad request"))
        .andExpect(jsonPath("$.detail").value("size must be from 1 to 100: 500"));
  }

  @Test
  void createWithATooLongTagIs400() throws Exception {
    var tag = "t".repeat(51);
    mvc.perform(
            post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://a.example\",\"title\":\"A\",\"tags\":[\"" + tag + "\"]}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }
}
