package org.jwcarman.bookmarks.web;

/**
 * The body of {@code POST /collections} and {@code PUT /collections/{id}}.
 *
 * @param name the collection's name
 */
record CollectionNameRequest(String name) {}
