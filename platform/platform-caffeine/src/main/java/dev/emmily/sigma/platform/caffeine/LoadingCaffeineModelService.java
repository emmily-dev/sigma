package dev.emmily.sigma.platform.caffeine;

import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.CaffeineSpec;
import com.github.benmanes.caffeine.cache.LoadingCache;
import dev.emmily.sigma.api.Model;
import dev.emmily.sigma.api.service.ModelService;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

public class LoadingCaffeineModelService<T extends Model>
  implements ModelService<T> {
  private static final IllegalArgumentException INVALID_QUERY = new IllegalArgumentException(
    "LoadingCaffeineModelService only accepts queries of type String"
  );
  private final LoadingCache<String, T> cache;

  public LoadingCaffeineModelService(LoadingCache<String, T> cache) {
    this.cache = cache;
  }

  public LoadingCaffeineModelService(
    String spec,
    CacheLoader<String, T> loader
  ) {
    this(Caffeine.from(spec).build(loader));
  }

  public LoadingCaffeineModelService(
    CaffeineSpec spec,
    CacheLoader<String, T> loader
  ) {
    this(Caffeine.from(spec).build(loader));
  }

  @Override
  public void create(T model) {
    cache.put(model.getId(), model);
  }

  @Override
  public T find(String id) {
    return cache.get(id);
  }

  @Override
  public T findByQuery(Object query) {
    if (query instanceof String) {
      return find((String) query);
    }

    throw INVALID_QUERY;
  }

  @Override
  public List<T> findMany(
    List<String> ids,
    int limit
  ) {
    List<T> models = new ArrayList<>();

    for (T model : cache.getAll(ids).values()) {
      if (limit == 0) {
        break;
      }

      models.add(model);
    }

    return models;
  }

  @Override
  public List<T> findManyByQuery(
    Object query,
    int limit
  ) {
    if (!(query instanceof Predicate)) {
      throw INVALID_QUERY;
    }

    @SuppressWarnings("unchecked")
    Predicate<T> modelQuery = (Predicate<T>) query;

    List<T> models = new ArrayList<>();

    for (T model : cache.asMap().values()) {
      if (modelQuery.test(model)) {
        if (limit-- == 0) {
          break;
        }

        models.add(model);
      }
    }

    return models;
  }

  @Override
  public List<T> findAll() {
    return new ArrayList<>(cache.asMap().values());
  }

  @Override
  public void delete(String id) {
    cache.invalidate(id);
  }

  @Override
  public void deleteByQuery(Object query) {
    if (!(query instanceof Predicate)) {
      throw INVALID_QUERY;
    }

    @SuppressWarnings("unchecked")
    Predicate<T> modelQuery = (Predicate<T>) query;

    for (T model : cache.asMap().values()) {
      if (modelQuery.test(model)) {
        cache.invalidate(model.getId());
      }
    }
  }

  @Override
  public void deleteMany(List<String> ids) {
    cache.invalidateAll(ids);
  }

  @Override
  public void deleteManyByQuery(
    Object query,
    int limit
  ) {
    if (!(query instanceof Predicate)) {
      throw INVALID_QUERY;
    }

    @SuppressWarnings("unchecked")
    Predicate<T> modelQuery = (Predicate<T>) query;

    for (T model : cache.asMap().values()) {
      if (modelQuery.test(model)) {
        if (limit-- == 0) {
          break;
        }

        delete(model);
      }
    }
  }
}

