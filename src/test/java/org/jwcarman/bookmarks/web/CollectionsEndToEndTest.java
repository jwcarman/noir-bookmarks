package org.jwcarman.bookmarks.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jayway.jsonpath.JsonPath;
import org.hamcrest.Matchers;
import org.jwcarman.bookmarks.domain.BookmarkCollection;
import org.jwcarman.bookmarks.domain.BookmarkRepository;
import org.jwcarman.bookmarks.domain.CollectionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@SpringBootTest
@AutoConfigureMockMvc
class CollectionsEndToEndTest {

  @Autowired MockMvc mvc;
  @Autowired BookmarkRepository bookmarks;
  @Autowired CollectionRepository collections;

  @BeforeEach
  void clear() {
    collections.findAll().stream().map(BookmarkCollection::id).forEach(collections::deleteById);
    bookmarks.findAll(0, 1000).forEach(b -> bookmarks.deleteById(b.id()));
  }

  ResultActions postCollection(String body) throws Exception {
    return mvc.perform(post("/collections").contentType(MediaType.APPLICATION_JSON).content(body));
  }

  long createCollection(String name) throws Exception {
    String json =
        postCollection("{\"name\":\"" + name + "\"}")
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(json, "$.id")).longValue();
  }

  long createBookmark(String url) throws Exception {
    String json =
        mvc.perform(
                post("/bookmarks")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("{\"url\":\"" + url + "\",\"title\":\"A\"}"))
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    return ((Number) JsonPath.read(json, "$.id")).longValue();
  }

  @Test
  void createReturns201() throws Exception {
    String json =
        postCollection("{\"name\":\"Work\"}")
            .andExpect(status().isCreated())
            .andReturn()
            .getResponse()
            .getContentAsString();
    long id = ((Number) JsonPath.read(json, "$.id")).longValue();
    mvc.perform(get("/collections/" + id))
        .andExpect(status().isOk())
        .andExpect(content().json("{\"id\":" + id + ",\"name\":\"Work\",\"bookmarks\":[]}", true));
    postCollection("{\"name\":\"Other\"}")
        .andExpect(header().string("Location", Matchers.matchesPattern(".*/collections/\\d+$")));
  }

  @Test
  void locationHeaderEndsWithId() throws Exception {
    var result = postCollection("{\"name\":\"Work\"}").andReturn().getResponse();
    long id = ((Number) JsonPath.read(result.getContentAsString(), "$.id")).longValue();
    org.assertj.core.api.Assertions.assertThat(result.getHeader("Location"))
        .endsWith("/collections/" + id);
  }

  @Test
  void duplicateAndInvalidNames() throws Exception {
    createCollection("Work");
    postCollection("{\"name\":\"work\"}")
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.title").value("Duplicate collection"));
    postCollection("{\"name\":\"  \"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad request"));
    postCollection("{}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad request"));
  }

  @Test
  void addAndRemoveBookmarks() throws Exception {
    long c = createCollection("Work");
    long b = createBookmark("https://a.example/");
    mvc.perform(put("/collections/" + c + "/bookmarks/" + b))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookmarks[0].url").value("https://a.example/"))
        .andExpect(jsonPath("$.bookmarks[0].id").value(b));
    mvc.perform(put("/collections/" + c + "/bookmarks/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Bookmark not found"));
    mvc.perform(delete("/collections/" + c + "/bookmarks/" + b))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookmarks").isEmpty());
  }

  @Test
  void listsSummaries() throws Exception {
    long work = createCollection("Work");
    long home = createCollection("Home");
    long b = createBookmark("https://a.example/");
    mvc.perform(put("/collections/" + work + "/bookmarks/" + b)).andExpect(status().isOk());
    mvc.perform(get("/collections"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    "[{\"id\":" + home + ",\"name\":\"Home\",\"size\":0},{\"id\":" + work
                        + ",\"name\":\"Work\",\"size\":1}]",
                    true));
  }

  @Test
  void renameAndDelete() throws Exception {
    long c = createCollection("Work");
    mvc.perform(
            put("/collections/" + c)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Job\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Job"));
    mvc.perform(delete("/collections/" + c)).andExpect(status().isNoContent());
    mvc.perform(get("/collections/" + c))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Collection not found"));
  }

  @Test
  void deletingBookmarkRemovesItFromCollections() throws Exception {
    long c = createCollection("Work");
    long a = createBookmark("https://a.example/");
    long b = createBookmark("https://b.example/");
    mvc.perform(put("/collections/" + c + "/bookmarks/" + a)).andExpect(status().isOk());
    mvc.perform(put("/collections/" + c + "/bookmarks/" + b)).andExpect(status().isOk());
    mvc.perform(delete("/bookmarks/" + a)).andExpect(status().isNoContent());
    mvc.perform(get("/collections/" + c))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookmarks.length()").value(1))
        .andExpect(jsonPath("$.bookmarks[0].id").value(b));
  }
}
