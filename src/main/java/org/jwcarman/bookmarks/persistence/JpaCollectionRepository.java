package org.jwcarman.bookmarks.persistence;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import org.jwcarman.bookmarks.domain.BookmarkCollection;
import org.jwcarman.bookmarks.domain.CollectionRepository;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * A {@link CollectionRepository} backed by JPA. Members are stored in a membership table that
 * references the bookmark table, keeping their order.
 */
@Repository
@Transactional
public class JpaCollectionRepository implements CollectionRepository {

  @PersistenceContext private EntityManager entityManager;

  @Override
  public BookmarkCollection save(BookmarkCollection collection) {
    List<BookmarkEntity> members =
        collection.bookmarkIds().stream()
            .map(id -> entityManager.getReference(BookmarkEntity.class, id))
            .toList();
    CollectionEntity entity = existing(collection.id());
    if (entity == null) {
      entity = new CollectionEntity(collection.name(), new ArrayList<>(members));
      entityManager.persist(entity);
    } else {
      entity.rename(collection.name());
      entity.bookmarks().clear();
      entity.bookmarks().addAll(members);
    }
    return new BookmarkCollection(entity.id(), collection.name(), collection.bookmarkIds());
  }

  private CollectionEntity existing(Long id) {
    return id == null ? null : entityManager.find(CollectionEntity.class, id);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<BookmarkCollection> findById(long id) {
    return Optional.ofNullable(entityManager.find(CollectionEntity.class, id)).map(this::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public Optional<BookmarkCollection> findByName(String name) {
    return entityManager
        .createQuery(
            "select c from CollectionEntity c where lower(c.name) = :name order by c.id",
            CollectionEntity.class)
        .setParameter("name", name.toLowerCase(Locale.ROOT))
        .setMaxResults(1)
        .getResultStream()
        .findFirst()
        .map(this::toDomain);
  }

  @Override
  @Transactional(readOnly = true)
  public List<BookmarkCollection> findAll() {
    return entityManager
        .createQuery(
            "select c from CollectionEntity c order by lower(c.name), c.id", CollectionEntity.class)
        .getResultStream()
        .map(this::toDomain)
        .toList();
  }

  @Override
  public boolean deleteById(long id) {
    CollectionEntity entity = entityManager.find(CollectionEntity.class, id);
    if (entity == null) {
      return false;
    }
    entityManager.remove(entity);
    entityManager.flush();
    return true;
  }

  private BookmarkCollection toDomain(CollectionEntity entity) {
    var util = entityManager.getEntityManagerFactory().getPersistenceUnitUtil();
    List<Long> ids = entity.bookmarks().stream().map(b -> (Long) util.getIdentifier(b)).toList();
    return new BookmarkCollection(entity.id(), entity.name(), ids);
  }
}
