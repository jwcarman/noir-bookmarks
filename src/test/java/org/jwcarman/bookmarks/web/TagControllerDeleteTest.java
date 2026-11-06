package org.jwcarman.bookmarks.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.jwcarman.bookmarks.service.BookmarkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(TagController.class)
class TagControllerDeleteTest {

  @Autowired MockMvc mvc;
  @MockitoBean BookmarkService service;

  @Test
  void deleteReturns200WithCount() throws Exception {
    given(service.deleteTagged("java")).willReturn(2);
    mvc.perform(delete("/tags/java"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.deleted").value(2));
    verify(service).deleteTagged("java");
  }

  @Test
  void illegalArgumentIs400Problem() throws Exception {
    given(service.deleteTagged(anyString())).willThrow(new IllegalArgumentException("tag must not be blank"));
    mvc.perform(delete("/tags/%20"))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
  }

  @Test
  void renameStillWorks() throws Exception {
    given(service.renameTag("js", "javascript")).willReturn(1);
    mvc.perform(
            post("/tags/js/rename")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"to\":\"javascript\"}"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.renamed").value(1));
  }

  @Test
  void responseHoldsItsValue() {
    assertThat(new DeleteTagResponse(5).deleted()).isEqualTo(5);
  }
}
