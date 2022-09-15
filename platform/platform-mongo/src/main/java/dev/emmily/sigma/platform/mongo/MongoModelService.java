package dev.emmily.sigma.platform.mongo;

import com.mongodb.client.MongoCollection;
import com.mongodb.client.model.ReplaceOptions;
import dev.emmily.sigma.api.Model;
import dev.emmily.sigma.api.service.CachedAsyncModelService;
import dev.emmily.sigma.api.service.ModelService;
import org.bson.conversions.Bson;
import org.bson.json.JsonObject;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Filters.in;

/**
 * This is a MongoDB implementation using
 * the MongoDB sync driver. It allows the
 * usage of {@link Bson} objects as queries,
 * as well as raw strings representing
 * {@link Bson} queries.
 *
 * @param <T> The type of model held by this
 *            model service.
 */
public class MongoModelService<T extends Model>
  extends CachedAsyncModelService<T>
  implements ModelService<T> {
  private static final IllegalArgumentException INVALID_QUERY = new IllegalArgumentException(
    "MongoModelService only accepts queries of type Bson and String"
  );
  private final MongoCollection<T> mongoCollection;

  public MongoModelService(
    Executor executor,
    ModelService<T> cacheModelService,
    MongoCollection<T> mongoCollection
  ) {
    super(executor, cacheModelService);
    this.mongoCollection = mongoCollection;
  }

  public MongoModelService(
    ModelService<T> cacheModelService,
    MongoCollection<T> mongoCollection
  ) {
    this(
      Executors.newSingleThreadExecutor(),
      cacheModelService,
      mongoCollection
    );
  }
  @Override
  public void create(T model) {
    mongoCollection.replaceOne(
      eq("_id", model.getId()),
      model,
      new ReplaceOptions().upsert(true)
    );
  }

  @Override
  public T find(String id) {
    return mongoCollection
      .find(eq("_id", id))
      .first();
  }

  @Override
  public T findByQuery(Object query) {
    Bson filter;

    if (query instanceof Bson) {
      filter = (Bson) query;
    } else if (query instanceof String) {
      filter = new JsonObject((String) query);
    } else {
      throw INVALID_QUERY;
    }

    return mongoCollection
      .find(filter)
      .first();
  }

  @Override
  public List<T> findMany(
    List<String> ids,
    int limit
  ) {
    return mongoCollection
      .find(in("_id", ids))
      .limit(limit)
      .into(new ArrayList<>());
  }

  @Override
  public List<T> findManyByQuery(
    Object query,
    int limit
  ) {
    Bson filter;

    if (query instanceof Bson) {
      filter = (Bson) query;
    } else if (query instanceof String) {
      filter = new JsonObject((String) query);
    } else {
      throw INVALID_QUERY;
    }

    return mongoCollection
      .find(filter)
      .limit(limit)
      .into(new ArrayList<>());
  }

  @Override
  public List<T> findAll() {
    return mongoCollection.find().into(new ArrayList<>());
  }

  @Override
  public void delete(String id) {
    mongoCollection.deleteOne(eq("_id", id));
  }

  @Override
  public void deleteByQuery(Object query) {
    Bson filter;

    if (query instanceof Bson) {
      filter = (Bson) query;
    } else if (query instanceof String) {
      filter = new JsonObject((String) query);
    } else {
      throw INVALID_QUERY;
    }

    mongoCollection.deleteOne(filter);
  }

  @Override
  public void deleteMany(List<String> ids) {
    mongoCollection.deleteMany(in("_id", ids));
  }

  @Override
  public void deleteManyByQuery(
    Object query,
    int limit
  ) {
    Bson filter;

    if (query instanceof Bson) {
      filter = (Bson) query;
    } else if (query instanceof String) {
      filter = new JsonObject((String) query);
    } else {
      throw INVALID_QUERY;
    }

    mongoCollection.deleteMany(filter);
  }
}
