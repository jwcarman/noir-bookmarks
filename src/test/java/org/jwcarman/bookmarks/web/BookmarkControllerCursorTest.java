package org.jwcarman.bookmarks.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.service.BookmarkPage;
import org.jwcarman.bookmarks.service.BookmarkService;
import org.jwcarman.bookmarks.service.CursorPage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookmarkController.class)
class BookmarkControllerCursorTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");
  static final Bookmark A = new Bookmark(7L, "https://a.example/", "A", List.of("x"), NOON, "n");

  @Autowired MockMvc mvc;
  @Autowired BookmarkController controller;
  @MockitoBean BookmarkService service;

  @Test
  void listPassesLimitAndAfter() throws Exception {
    given(service.listAfter("tok", 2)).willReturn(new CursorPage(List.of(A), "next1"));
    mvc.perform(get("/bookmarks").param("limit", "2").param("after", "tok"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    "{\"items\":[{\"id\":7,\"url\":\"https://a.example/\",\"title\":\"A\","
                        + "\"tags\":[\"x\"],\"createdAt\":\"2026-11-06T12:00:00Z\","
                        + "\"notes\":\"n\"}],\"next\":\"next1\"}",
                    true));
  }

  @Test
  void lastPageHasNullNext() throws Exception {
    given(service.listAfter(null, 20)).willReturn(new CursorPage(List.of(), null));
    mvc.perform(get("/bookmarks"))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"items\":[],\"next\":null}", true));
  }

  @Test
  void invalidCursorIsBadRequest() throws Exception {
    given(service.listAfter("!!", 20)).willThrow(new IllegalArgumentException("invalid cursor: !!"));
    mvc.perform(get("/bookmarks").param("after", "!!"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Bad request"));
  }

  @Test
  void searchUsesDefaults() throws Exception {
    given(service.searchAfter("", Set.of(), null, 20)).willReturn(new CursorPage(List.of(A), null));
    mvc.perform(get("/bookmarks/search"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].id").value(7))
        .andExpect(jsonPath("$.next").isEmpty());
    verify(service).searchAfter("", Set.of(), null, 20);
  }

  @Test
  void searchPassesEveryParameter() throws Exception {
    given(service.searchAfter("java", Set.of("x", "y"), "tok", 3))
        .willReturn(new CursorPage(List.of(A), "more"));
    mvc.perform(
            get("/bookmarks/search")
                .param("q", "java")
                .param("tag", "x")
                .param("tag", "y")
                .param("after", "tok")
                .param("limit", "3"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].title").value("A"))
        .andExpect(jsonPath("$.next").value("more"));
  }

  @Test
  void searchBadLimitIsBadRequest() throws Exception {
    given(service.searchAfter("", Set.of(), null, 0))
        .willThrow(new IllegalArgumentException("limit must be from 1 to 100: 0"));
    mvc.perform(get("/bookmarks/search").param("limit", "0"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad request"));
  }

  @Test
  void oldListMethodStillDelegatesToPagedList() {
    given(service.list(1, 5)).willReturn(new BookmarkPage(List.of(A), 1, 5, 6));
    assertThat(controller.list(1, 5))
        .isEqualTo(new BookmarkPageResponse(List.of(BookmarkResponse.from(A)), 1, 5, 6));
  }

  @Test
  void cursorPageResponseConvertsEveryItem() {
    var b = new Bookmark(8L, "https://b.example/", "B", List.of(), NOON);
    assertThat(CursorPageResponse.from(new CursorPage(List.of(A, b), "n")))
        .isEqualTo(
            new CursorPageResponse(
                List.of(BookmarkResponse.from(A), BookmarkResponse.from(b)), "n"));
  }
}
