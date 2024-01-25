package arturkus.persistance;

import arturkus.entities.Item;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.util.List;
import java.util.UUID;

public class ItemRepositoryImpl implements ItemRepository {

    public static final String DB_TABLE = "items";
    private static final Logger LOGGER = LoggerFactory.getLogger(ItemRepositoryImpl.class);

    MongoClient client;

    public ItemRepositoryImpl(MongoClient mongoClient) {
        this.client = mongoClient;
    }

    @Override
    public void create(Item item, Handler<AsyncResult<Item>> resultHandler) {
        save(item, resultHandler);
    }

    @Override
    public void get(String id, Handler<AsyncResult<Item>> resultHandler) {
        client.find(DB_TABLE, new JsonObject().put("_id", id), res -> {
            if (res.succeeded() && res.result() != null) {
                res.result().stream().findFirst().ifPresent(findItem -> {
                    String findId = findItem.getString("_id");
                    if (findId != null)
                        resultHandler.handle(Future.succeededFuture(new Item(findItem, UUID.fromString(findId))));
                });
                return;
            }
            LOGGER.error("Error while searching item", res.cause());
            resultHandler.handle(Future.failedFuture("Item not found"));
        });
    }

    @Override
    public void findAll(String userId, Handler<AsyncResult<JsonObject>> resultHandler) {
        client.find(DB_TABLE, new JsonObject().put("userId", userId), res -> {
            if (res.succeeded() && res.result() != null) {
                List<Item> items = res.result().stream()
                        .map(item -> new Item(item, UUID.fromString(item.getString("_id"))))
                        .toList();
                JsonObject entries = new JsonObject();
                for (int i = 0; i < items.size(); i++) {
                    entries.put(String.valueOf(i + 1), items.get(i).getJsonObject(false));
                }
                resultHandler.handle(Future.succeededFuture(entries));
                return;
            }
            LOGGER.error("Error while searching item", res.cause());
            resultHandler.handle(Future.failedFuture("Item not found"));
        });
    }

    @Override
    public void update(Item item, Handler<AsyncResult<JsonObject>> resultHandler) {
        JsonObject query = new JsonObject().put("_id", new JsonObject().put("$oid", item.getId().toString()));
        JsonObject update = new JsonObject().put("$set", new JsonObject().put("name", item.getName()));

        client.findOneAndUpdate(DB_TABLE, query, update, res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(res.result()));
            } else {
                LOGGER.error("Error while updating item", res.cause());
                resultHandler.handle(Future.failedFuture("Item not found or update failed"));
            }
        });
    }

    @Override
    public void delete(UUID id, Handler<AsyncResult<Item>> resultHandler) {
    }

    private void save(Item item, Handler<AsyncResult<Item>> resultHandler) {
        client.save(DB_TABLE, item.getJsonObject(true), res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(item));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }
}
