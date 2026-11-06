package org.jwcarman.bookmarks.web;

import org.jwcarman.bookmarks.service.BookmarkNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Maps service exceptions to RFC 7807 problem details. */
@RestControllerAdvice
class ProblemHandler {

  @ExceptionHandler(BookmarkNotFoundException.class)
  ProblemDetail notFound(BookmarkNotFoundException e) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, e.getMessage());
    problem.setTitle("Bookmark not found");
    return problem;
  }

  @ExceptionHandler(IllegalArgumentException.class)
  ProblemDetail badRequest(IllegalArgumentException e) {
    var problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
    problem.setTitle("Bad request");
    return problem;
  }
}
