package org.jwcarman.bookmarks.web;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.service.BookmarkService;
import org.jwcarman.bookmarks.service.CursorPage;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookmarkController.class)
class BookmarkControllerNotesTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  static Bookmark saved(String notes) {
    return new Bookmark(7L, "https://a.example/", "A", List.of(), NOON, notes);
  }

  @Autowired MockMvc mvc;
  @MockitoBean BookmarkService service;

  @Test
  void createPassesNotes() throws Exception {
    given(service.create("https://a.example/", "A", List.of(), "why")).willReturn(saved("why"));
    mvc.perform(
            post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://a.example/\",\"title\":\"A\",\"notes\":\"why\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.notes").value("why"));
    verify(service).create("https://a.example/", "A", List.of(), "why");
  }

  @Test
  void createWithoutNotesPassesEmptyNotes() throws Exception {
    given(service.create("https://a.example/", "A", List.of(), "")).willReturn(saved(""));
    mvc.perform(
            post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://a.example/\",\"title\":\"A\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.notes").value(""));
    verify(service).create("https://a.example/", "A", List.of(), "");
  }

  @Test
  void createWithTooLongNotesIs400() throws Exception {
    mvc.perform(
            post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"url\":\"https://a.example/\",\"title\":\"A\",\"notes\":\""
                        + "n".repeat(2001)
                        + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    verifyNoInteractions(service);
  }

  @Test
  void createWithMaxLengthNotesIsAccepted() throws Exception {
    var notes = "n".repeat(2000);
    given(service.create("https://a.example/", "A", List.of(), notes)).willReturn(saved(notes));
    mvc.perform(
            post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"url\":\"https://a.example/\",\"title\":\"A\",\"notes\":\"" + notes + "\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.notes").value(notes));
  }

  @Test
  void updatePassesNotes() throws Exception {
    given(service.update(7, "T", List.of(), "new")).willReturn(saved("new"));
    mvc.perform(
            put("/bookmarks/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"T\",\"notes\":\"new\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notes").value("new"));
    verify(service).update(7, "T", List.of(), "new");
  }

  @Test
  void updateWithoutNotesClearsThem() throws Exception {
    given(service.update(7, "T", List.of(), "")).willReturn(saved(""));
    mvc.perform(
            put("/bookmarks/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"T\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notes").value(""));
    verify(service).update(7, "T", List.of(), "");
  }

  @Test
  void updateWithTooLongNotesIs400() throws Exception {
    mvc.perform(
            put("/bookmarks/7")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"T\",\"notes\":\"" + "n".repeat(2001) + "\"}"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    verifyNoInteractions(service);
  }

  @Test
  void getAndListReturnNotes() throws Exception {
    given(service.get(7)).willReturn(saved("n"));
    given(service.listAfter(null, 20)).willReturn(new CursorPage(List.of(saved("n")), null));
    mvc.perform(get("/bookmarks/7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notes").value("n"));
    mvc.perform(get("/bookmarks"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.items[0].notes").value("n"));
  }
}
