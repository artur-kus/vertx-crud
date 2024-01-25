package arturkus.persistance;

import arturkus.entities.User;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;

public interface UserRepository {
    void register(User user, Handler<AsyncResult<User>> resultHandler);

    void login(User user, Handler<AsyncResult<User>> resultHandler);
}
