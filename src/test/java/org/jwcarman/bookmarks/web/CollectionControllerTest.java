package org.jwcarman.bookmarks.web;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.service.BookmarkNotFoundException;
import org.jwcarman.bookmarks.service.CollectionNotFoundException;
import org.jwcarman.bookmarks.service.CollectionService;
import org.jwcarman.bookmarks.service.CollectionSummary;
import org.jwcarman.bookmarks.service.CollectionView;
import org.jwcarman.bookmarks.service.DuplicateCollectionException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(CollectionController.class)
class CollectionControllerTest {

  static final Bookmark BOOKMARK =
      new Bookmark(
          4L, "https://a.example/", "A", List.of("x"), Instant.parse("2026-11-06T12:00:00Z"), "n");

  @Autowired MockMvc mvc;
  @MockitoBean CollectionService service;

  ResultActions postJson(String body) throws Exception {
    return mvc.perform(post("/collections").contentType(MediaType.APPLICATION_JSON).content(body));
  }

  ResultActions putJson(String path, String body) throws Exception {
    return mvc.perform(put(path).contentType(MediaType.APPLICATION_JSON).content(body));
  }

  @Test
  void createReturns201WithLocation() throws Exception {
    given(service.create("Work")).willReturn(new CollectionView(7, "Work", List.of()));
    postJson("{\"name\":\"Work\"}")
        .andExpect(status().isCreated())
        .andExpect(header().string("Location", "http://localhost/collections/7"))
        .andExpect(content().json("{\"id\":7,\"name\":\"Work\",\"bookmarks\":[]}", true));
  }

  @Test
  void createDuplicateIs409() throws Exception {
    given(service.create("work")).willThrow(new DuplicateCollectionException("work"));
    postJson("{\"name\":\"work\"}")
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Duplicate collection"))
        .andExpect(jsonPath("$.detail").value("A collection named work already exists"));
  }

  @Test
  void createInvalidNameIs400() throws Exception {
    given(service.create(anyString())).willThrow(new IllegalArgumentException("name must not be blank"));
    postJson("{\"name\":\"  \"}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad request"));
  }

  @Test
  void missingNameIs400() throws Exception {
    postJson("{}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad request"))
        .andExpect(jsonPath("$.detail").value("name is required"));
    putJson("/collections/1", "{}")
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad request"));
    verifyNoInteractions(service);
  }

  @Test
  void malformedBodyIs400() throws Exception {
    postJson("{not json")
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Bad request"))
        .andExpect(jsonPath("$.detail").value("Invalid request body"));
  }

  @Test
  void listReturnsSummaries() throws Exception {
    given(service.list())
        .willReturn(List.of(new CollectionSummary(2, "Home", 0), new CollectionSummary(1, "Work", 1)));
    mvc.perform(get("/collections"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    "[{\"id\":2,\"name\":\"Home\",\"size\":0},{\"id\":1,\"name\":\"Work\",\"size\":1}]",
                    true));
  }

  @Test
  void getReturnsCollectionWithBookmarks() throws Exception {
    given(service.get(7)).willReturn(new CollectionView(7, "Work", List.of(BOOKMARK)));
    mvc.perform(get("/collections/7"))
        .andExpect(status().isOk())
        .andExpect(
            content()
                .json(
                    "{\"id\":7,\"name\":\"Work\",\"bookmarks\":[{\"id\":4,\"url\":\"https://a.example/\","
                        + "\"title\":\"A\",\"tags\":[\"x\"],\"createdAt\":\"2026-11-06T12:00:00Z\",\"notes\":\"n\"}]}",
                    true));
  }

  @Test
  void unknownCollectionIs404() throws Exception {
    given(service.get(9)).willThrow(new CollectionNotFoundException(9));
    mvc.perform(get("/collections/9"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
        .andExpect(jsonPath("$.title").value("Collection not found"))
        .andExpect(jsonPath("$.detail").value("Collection 9 not found"));
  }

  @Test
  void renameReturns200() throws Exception {
    given(service.rename(7, "Job")).willReturn(new CollectionView(7, "Job", List.of()));
    putJson("/collections/7", "{\"name\":\"Job\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Job"))
        .andExpect(jsonPath("$.id").value(7));
  }

  @Test
  void deleteReturns204() throws Exception {
    mvc.perform(delete("/collections/7")).andExpect(status().isNoContent());
    verify(service).delete(7);
  }

  @Test
  void deleteUnknownIs404() throws Exception {
    willThrow(new CollectionNotFoundException(9)).given(service).delete(9);
    mvc.perform(delete("/collections/9"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Collection not found"));
  }

  @Test
  void addReturnsCollection() throws Exception {
    given(service.add(7, 4)).willReturn(new CollectionView(7, "Work", List.of(BOOKMARK)));
    mvc.perform(put("/collections/7/bookmarks/4"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookmarks[0].url").value("https://a.example/"));
  }

  @Test
  void addUnknownBookmarkIs404() throws Exception {
    given(service.add(anyLong(), anyLong())).willThrow(new BookmarkNotFoundException(999));
    mvc.perform(put("/collections/7/bookmarks/999"))
        .andExpect(status().isNotFound())
        .andExpect(jsonPath("$.title").value("Bookmark not found"));
  }

  @Test
  void removeReturnsCollection() throws Exception {
    given(service.remove(7, 4)).willReturn(new CollectionView(7, "Work", List.of()));
    mvc.perform(delete("/collections/7/bookmarks/4"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.bookmarks").isEmpty());
  }
}
