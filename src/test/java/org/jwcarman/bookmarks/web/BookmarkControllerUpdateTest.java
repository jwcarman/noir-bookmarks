package org.jwcarman.bookmarks.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.service.BookmarkNotFoundException;
import org.jwcarman.bookmarks.service.BookmarkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(BookmarkController.class)
class BookmarkControllerUpdateTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");
  static final Bookmark UPDATED =
      new Bookmark(7L, "https://a.example", "New", List.of("a", "b"), NOON);

  @Autowired MockMvc mvc;
  @MockitoBean BookmarkService service;

  ResultActions putJson(long id, String body) throws Exception {
    return mvc.perform(
        put("/bookmarks/" + id).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  @Test
  void updateReturns200WithTheBookmark() throws Exception {
    given(service.update(7, "New", List.of("a", "b"), "")).willReturn(UPDATED);
    putJson(7, "{\"title\":\"New\",\"tags\":[\"a\",\"b\"]}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7))
        .andExpect(jsonPath("$.title").value("New"))
        .andExpect(jsonPath("$.url").value("https://a.example"))
        .andExpect(jsonPath("$.tags[1]").value("b"))
        .andExpect(jsonPath("$.createdAt").value("2026-11-06T12:00:00Z"));
    verify(service).update(7, "New", List.of("a", "b"), "");
  }

  @Test
  void updateWithoutTagsUsesAnEmptyList() throws Exception {
    given(service.update(7, "New", List.of(), "")).willReturn(UPDATED);
    putJson(7, "{\"title\":\"New\"}").andExpect(status().isOk());
    verify(service).update(7, "New", List.of(), "");
  }

  @Test
  void updateMissingIs404Problem() throws Exception {
    given(service.update(anyLong(), any(), any(), any())).willThrow(new BookmarkNotFoundException(9));
    putJson(9, "{\"title\":\"x\"}")
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Bookmark not found"));
  }

  @Test
  void blankTitleIs400Problem() throws Exception {
    putJson(7, "{\"title\":\"   \"}")
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    verifyNoInteractions(service);
  }

  @Test
  void missingTitleIs400() throws Exception {
    putJson(7, "{\"tags\":[\"a\"]}").andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void tooLongTitleIs400() throws Exception {
    putJson(7, "{\"title\":\"" + "t".repeat(201) + "\"}").andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void maxLengthTitleIsAccepted() throws Exception {
    var title = "t".repeat(200);
    given(service.update(7, title, List.of(), "")).willReturn(UPDATED);
    putJson(7, "{\"title\":\"" + title + "\"}").andExpect(status().isOk());
  }

  @Test
  void blankOrTooLongTagIs400() throws Exception {
    putJson(7, "{\"title\":\"x\",\"tags\":[\" \"]}").andExpect(status().isBadRequest());
    putJson(7, "{\"title\":\"x\",\"tags\":[\"" + "t".repeat(51) + "\"]}")
        .andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void tooManyTagsIs400() throws Exception {
    var tags = String.join(",", java.util.Collections.nCopies(51, "\"a\""));
    putJson(7, "{\"title\":\"x\",\"tags\":[" + tags + "]}").andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }
}
