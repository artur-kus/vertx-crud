package arturkus.persistance;

import arturkus.entities.Item;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.json.JsonObject;

import java.util.UUID;

public interface ItemRepository {
    void create(Item item, Handler<AsyncResult<Item>> resultHandler);

    void get(String id, Handler<AsyncResult<Item>> resultHandler);

    void findAll(String userId, Handler<AsyncResult<JsonObject>> resultHandler);

    void update(Item item, Handler<AsyncResult<JsonObject>> resultHandler);

    void delete(UUID id, Handler<AsyncResult<Item>> resultHandler);
}
