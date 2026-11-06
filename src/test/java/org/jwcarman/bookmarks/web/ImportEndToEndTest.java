package org.jwcarman.bookmarks.web;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.jwcarman.bookmarks.domain.BookmarkRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class ImportEndToEndTest {

  @Autowired MockMvc mvc;
  @Autowired BookmarkRepository repository;

  @BeforeEach
  void clear() {
    repository.findAll(0, 1000).forEach(b -> repository.deleteById(b.id()));
  }

  @Test
  void importsAndReportsDuplicates() throws Exception {
    mvc.perform(
            post("/bookmarks/import")
                .contentType("text/csv")
                .content(
                    "id,url,title,tags,createdAt\r\n"
                        + "1,https://a.example/,A,,2024-01-01T00:00:00Z\r\n"
                        + "2,https://a.example/,A,,2024-01-01T00:00:00Z\r\n"))
        .andExpect(status().isOk())
        .andExpect(
            content().json("{\"imported\":1,\"skipped\":[{\"line\":3,\"reason\":\"DUPLICATE\"}]}", true));
    assertThat(repository.count()).isEqualTo(1);
  }

  @Test
  void rejectsFileWithoutHeader() throws Exception {
    mvc.perform(post("/bookmarks/import").contentType("text/csv").content("url,title\r\n"))
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.title").value("Bad request"));
    assertThat(repository.count()).isZero();
  }
}
