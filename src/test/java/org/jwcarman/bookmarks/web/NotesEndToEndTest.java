package org.jwcarman.bookmarks.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.jwcarman.bookmarks.domain.BookmarkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class NotesEndToEndTest {

  @Autowired MockMvc mvc;
  @Autowired BookmarkRepository repository;

  @BeforeEach
  void clear() {
    repository.findAll(0, 1000).forEach(b -> repository.deleteById(b.id()));
  }

  long create(String body) throws Exception {
    String json =
        mvc.perform(post("/bookmarks").contentType(MediaType.APPLICATION_JSON).content(body))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(json, "$.id")).longValue();
  }

  @Test
  void createStoresNotes() throws Exception {
    mvc.perform(
            post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://a.example/\",\"title\":\"A\",\"notes\":\"why\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.notes").value("why"));
    assertThat(repository.findByUrl("https://a.example/").orElseThrow().notes()).isEqualTo("why");
  }

  @Test
  void createWithoutNotesHasEmptyNotes() throws Exception {
    mvc.perform(
            post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"url\":\"https://a.example/\",\"title\":\"A\"}"))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.notes").value(""));
  }

  @Test
  void tooLongNotesAre400AndNothingIsSaved() throws Exception {
    mvc.perform(
            post("/bookmarks")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    "{\"url\":\"https://a.example/\",\"title\":\"A\",\"notes\":\""
                        + "n".repeat(2001)
                        + "\"}"))
        .andExpect(status().isBadRequest());
    assertThat(repository.count()).isZero();
  }

  @Test
  void putReplacesAndClearsNotes() throws Exception {
    long id = create("{\"url\":\"https://a.example/\",\"title\":\"A\",\"notes\":\"old\"}");
    mvc.perform(
            put("/bookmarks/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"T\",\"notes\":\"new\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notes").value("new"));
    mvc.perform(
            put("/bookmarks/" + id)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"T\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.notes").value(""));
    assertThat(repository.findById(id).orElseThrow().notes()).isEmpty();
  }

  @Test
  void getAndListShowNotes() throws Exception {
    long id = create("{\"url\":\"https://a.example/\",\"title\":\"A\",\"notes\":\"n\"}");
    mvc.perform(get("/bookmarks/" + id)).andExpect(jsonPath("$.notes").value("n"));
    mvc.perform(get("/bookmarks")).andExpect(jsonPath("$.items[0].notes").value("n"));
  }
}
