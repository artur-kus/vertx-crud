package arturkus.persistance;

import arturkus.entities.User;
import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.mongo.MongoClient;

import java.util.UUID;

public class UserRepositoryImpl implements UserRepository {

    public static final String DB_TABLE = "users";
    private static final Logger LOGGER = LoggerFactory.getLogger(UserRepositoryImpl.class);

    MongoClient client;

    public UserRepositoryImpl(final MongoClient client) {
        this.client = client;
    }

    @Override
    public void login(User user, Handler<AsyncResult<User>> resultHandler) {
        client.find(DB_TABLE, user.getUserWithoutId(), res -> {
            if (res.succeeded() && res.result() != null) {
                res.result().stream().findFirst().ifPresent(findUser -> {
                    String id = findUser.getString("_id");
                    if (id != null)
                        resultHandler.handle(Future.succeededFuture(new User(findUser, UUID.fromString(id))));
                });
                return;
            }
            LOGGER.error("Error while searching user", res.cause());
            resultHandler.handle(Future.failedFuture("User not found"));
        });
    }

    @Override
    public void register(User user, Handler<AsyncResult<User>> resultHandler) {
        checkLoginExist(user, existUser -> {
            if (existUser.result()) {
                resultHandler.handle(Future.failedFuture("User with the same login already exists"));
            } else if (existUser.succeeded()) {
                save(user, resultHandler);
            } else resultHandler.handle(Future.failedFuture(existUser.cause()));
        });
    }

    private void checkLoginExist(User user, Handler<AsyncResult<Boolean>> resultHandler) {
        JsonObject query = new JsonObject().put("login", user.getLogin());
        client.find(DB_TABLE, query, res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(!res.result().isEmpty()));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }

    private void save(User user, Handler<AsyncResult<User>> resultHandler) {
        client.save(DB_TABLE, user.getJsonObject(), res -> {
            if (res.succeeded()) {
                resultHandler.handle(Future.succeededFuture(user));
            } else {
                resultHandler.handle(Future.failedFuture(res.cause()));
            }
        });
    }
}
