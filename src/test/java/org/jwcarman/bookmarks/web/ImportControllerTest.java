package org.jwcarman.bookmarks.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import org.jwcarman.bookmarks.service.BookmarkImporter;
import org.jwcarman.bookmarks.service.ImportResult;
import org.jwcarman.bookmarks.service.SkipReason;
import org.jwcarman.bookmarks.service.SkippedRow;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ImportController.class)
class ImportControllerTest {

  @Autowired MockMvc mvc;
  @MockitoBean BookmarkImporter importer;

  @Test
  void returnsResultAsJson() throws Exception {
    given(importer.importCsv("body"))
        .willReturn(new ImportResult(2, List.of(new SkippedRow(3, SkipReason.DUPLICATE))));
    mvc.perform(post("/bookmarks/import").contentType("text/csv").content("body"))
        .andExpect(status().isOk())
        .andExpect(
            content().json("{\"imported\":2,\"skipped\":[{\"line\":3,\"reason\":\"DUPLICATE\"}]}", true));
  }

  @Test
  void rejectedFileIsBadRequest() throws Exception {
    given(importer.importCsv("url,title\r\n")).willThrow(new IllegalArgumentException("no header"));
    mvc.perform(post("/bookmarks/import").contentType("text/csv").content("url,title\r\n"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad request"));
  }

  @Test
  void requiresCsvContentType() throws Exception {
    mvc.perform(post("/bookmarks/import").contentType("application/json").content("{}"))
        .andExpect(status().isUnsupportedMediaType());
  }
}
