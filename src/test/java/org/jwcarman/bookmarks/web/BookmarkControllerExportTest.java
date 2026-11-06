package org.jwcarman.bookmarks.web;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;
import org.jwcarman.bookmarks.domain.Bookmark;
import org.jwcarman.bookmarks.service.BookmarkService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(BookmarkController.class)
class BookmarkControllerExportTest {

  static final Instant NOON = Instant.parse("2026-11-06T12:00:00Z");

  @Autowired MockMvc mvc;
  @MockitoBean BookmarkService service;

  @Test
  void exportsCsv() throws Exception {
    given(service.all())
        .willReturn(List.of(new Bookmark(1L, "https://a.example", "Alpha", List.of("java"), NOON)));
    mvc.perform(get("/bookmarks/export.csv"))
        .andExpect(status().isOk())
        .andExpect(header().string("Content-Type", "text/csv;charset=UTF-8"))
        .andExpect(header().string("Content-Disposition", "attachment; filename=\"bookmarks.csv\""))
        .andExpect(
            content()
                .string(
                    "id,url,title,tags,createdAt,notes\r\n"
                        + "1,https://a.example,Alpha,java,2026-11-06T12:00:00Z,\r\n"));
  }

  @Test
  void otherRoutesStillWork() throws Exception {
    given(service.get(7))
        .willReturn(new Bookmark(7L, "https://a.example", "A", List.of(), NOON));
    mvc.perform(get("/bookmarks/7"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(7));
  }
}
