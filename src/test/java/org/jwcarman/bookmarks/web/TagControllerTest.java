package org.jwcarman.bookmarks.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.jwcarman.bookmarks.service.TagService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

@WebMvcTest(TagController.class)
class TagControllerTest {

  @Autowired MockMvc mvc;
  @MockitoBean TagService service;

  ResultActions postJson(String tag, String body) throws Exception {
    return mvc.perform(
        post("/tags/" + tag + "/rename").contentType(MediaType.APPLICATION_JSON).content(body));
  }

  @Test
  void renameReturns200WithCount() throws Exception {
    given(service.renameTag("js", "javascript")).willReturn(3);
    postJson("js", "{\"to\":\"javascript\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.renamed").value(3));
    verify(service).renameTag("js", "javascript");
  }

  @Test
  void blankToIs400Problem() throws Exception {
    postJson("js", "{\"to\":\"  \"}")
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON));
    verifyNoInteractions(service);
  }

  @Test
  void missingToIs400() throws Exception {
    postJson("js", "{}").andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void tooLongToIs400() throws Exception {
    postJson("js", "{\"to\":\"" + "a".repeat(51) + "\"}").andExpect(status().isBadRequest());
    verifyNoInteractions(service);
  }

  @Test
  void fiftyCharacterToIsAccepted() throws Exception {
    String to = "a".repeat(50);
    given(service.renameTag("js", to)).willReturn(1);
    postJson("js", "{\"to\":\"" + to + "\"}")
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.renamed").value(1));
  }

  @Test
  void recordsHoldTheirValues() {
    assertThat(new RenameTagRequest("x").to()).isEqualTo("x");
    assertThat(new RenameTagResponse(4).renamed()).isEqualTo(4);
  }
}
