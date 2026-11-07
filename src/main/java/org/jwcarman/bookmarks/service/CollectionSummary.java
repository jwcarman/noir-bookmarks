package org.jwcarman.bookmarks.service;

/**
 * A collection without its members.
 *
 * @param id the collection's id
 * @param name the collection's name
 * @param size how many bookmarks it holds
 */
public record CollectionSummary(long id, String name, int size) {}
