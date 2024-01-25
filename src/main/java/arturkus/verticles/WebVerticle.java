package arturkus.verticles;

import arturkus.entities.Item;
import arturkus.entities.User;
import arturkus.persistance.ItemRepository;
import arturkus.persistance.ItemRepositoryImpl;
import arturkus.persistance.UserRepository;
import arturkus.persistance.UserRepositoryImpl;
import arturkus.utils.JWTUtils;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.eventbus.Message;
import io.vertx.core.http.HttpMethod;
import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.JWTOptions;
import io.vertx.ext.auth.authentication.TokenCredentials;
import io.vertx.ext.auth.impl.jose.JWT;
import io.vertx.ext.auth.jwt.JWTAuth;
import io.vertx.ext.mongo.MongoClient;
import io.vertx.ext.web.Router;
import io.vertx.ext.web.RoutingContext;
import io.vertx.ext.web.handler.BodyHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

public class WebVerticle extends AbstractVerticle {

    //ROUTE ADDRESSES
    private static final String REGISTER_USER = "register-user";
    private static final String LOGIN_USER = "login-user";
    private static final String CREATE_ITEM = "create-item";
    private static final String GET_ITEM = "get-item";
    private static final String FIND_ALL_ITEMS = "find-all-items";
    private static final String UPDATE_ITEM = "update-item";
    private static final String DELETE_ITEM = "delete-item";

    public static final String MONGODB_KEY = "mongodb";

    MongoClient mongoClient;
    JWT jwt = new JWT();
    JWTAuth jwtAuth;
    UserRepository userRepository;
    ItemRepository itemRepository;

    private static final Logger LOGGER = LoggerFactory.getLogger(WebVerticle.class);

    public WebVerticle(Vertx vertx, JsonObject config) {
        this.vertx = vertx;
        jwtAuth = JWTAuth.create(vertx, JWTUtils.getJWTConfig(config));
        mongoClient = MongoClient.createShared(vertx, config.getJsonObject(MONGODB_KEY));
        userRepository = new UserRepositoryImpl(mongoClient);
        itemRepository = new ItemRepositoryImpl(mongoClient);
    }

    @Override
    public void start() {
        Router router = Router.router(vertx);
        getAllRoutes(router);
        getAllConsumers();
        vertx.createHttpServer().requestHandler(router).listen(8080);
    }

    @Override
    public void stop() {
        if (mongoClient != null) {
            mongoClient.close();
        }
    }

    private void getAllRoutes(Router router) {
        router.route().handler(BodyHandler.create());
        router.route(HttpMethod.POST, "/register").handler(rc -> prepareDefaultHandler(rc, REGISTER_USER));
        router.route(HttpMethod.POST, "/login").handler(rc -> prepareDefaultHandler(rc, LOGIN_USER));
        getItemsRoutes(router);
    }

    private void getItemsRoutes(Router router) {
        router.route(HttpMethod.POST, "/items").handler(rc -> prepareDefaultHandler(rc, CREATE_ITEM));
        router.route(HttpMethod.GET, "/items/:id").handler(rc -> prepareDefaultHandler(rc, GET_ITEM));
        router.route(HttpMethod.GET, "/items").handler(rc -> prepareDefaultHandler(rc, FIND_ALL_ITEMS));
        router.route(HttpMethod.PUT, "/items").handler(rc -> prepareDefaultHandler(rc, UPDATE_ITEM));
        router.route(HttpMethod.DELETE, "/items").handler(rc -> prepareDefaultHandler(rc, DELETE_ITEM));
    }

    private void getAllConsumers() {
        login();
        registerUser();
        vertx.eventBus().<JsonObject>consumer(CREATE_ITEM, message -> checkToken(message, this::createItem));
        vertx.eventBus().<JsonObject>consumer(GET_ITEM, message -> checkToken(message, this::getItem));
        vertx.eventBus().<JsonObject>consumer(FIND_ALL_ITEMS, message -> checkToken(message, this::findAllItems));
        vertx.eventBus().<JsonObject>consumer(UPDATE_ITEM, message -> checkToken(message, this::updateItem));
        // DELETE ITEM NOT IMPLEMENTED YET
    }

    private void createItem(Message<JsonObject> message) {
        Item item = new Item(message.body());
        if (item.isFilled()) {
            itemRepository.create(item, res -> {
                if (res.succeeded()) {
                    LOGGER.info("Adding new item: {}", res.result().toString());
                    message.reply(item.getJsonObject(true).put("status", "OK"));
                } else {
                    message.reply(getErrorMessage(res.cause().getMessage()));
                }
            });
        } else message.reply(getErrorMessage("Incorrect item data"));
    }

    private void getItem(Message<JsonObject> message) {
        if (message.body() != null) {
            String id = message.body().getString("id");
            if (id != null)
                itemRepository.get(id, res -> {
                    if (res.succeeded()) {
                        message.reply(res.result().getJsonObject(true).put("status", "OK"));
                    } else {
                        message.reply(getErrorMessage(res.cause().getMessage()));
                    }
                });
        } else message.reply(getErrorMessage("Incorrect item data"));
    }

    private void findAllItems(Message<JsonObject> message) {
        if (message.body() != null) {
            String userId = message.body().getString("userId");
            if (userId != null)
                itemRepository.findAll(userId, res -> {
                    if (res.succeeded()) {
                        message.reply(res.result());
                    } else {
                        message.reply(getErrorMessage(res.cause().getMessage()));
                    }
                });
        } else message.reply(getErrorMessage("Incorrect item data"));
    }

    private void updateItem(Message<JsonObject> message) {
        Item item = new Item(message.body());
        if (item.isFilled() && item.getId() != null) {
            itemRepository.update(item, res -> {
                if (res.succeeded()) {
                    message.reply(res.result());
                } else {
                    message.reply(getErrorMessage(res.cause().getMessage()));
                }
            });
        } else message.reply(getErrorMessage("Incorrect item data"));
    }

    private void checkToken(Message<JsonObject> message, Consumer<Message<JsonObject>> onSuccess) {
        String token = getToken(message);
        if (token != null) {
            jwtAuth.authenticate(new TokenCredentials(token), authResult -> {
                if (authResult.succeeded()) {
                    message.body().put("userId", authResult.result().get("id"));
                    onSuccess.accept(message);
                } else throwUnauthorize(message);
            });
        } else {
            LOGGER.error("Token is null");
            throwUnauthorize(message);
        }
    }

    private String getToken(Message<JsonObject> message) {
        if (message.body() != null && !message.body().isEmpty()) {
            String bearerToken = message.body().getString("token");
            if (bearerToken != null)
                return JWTUtils.extractToken(bearerToken);
        }
        throwUnauthorize(message);
        return null;
    }

    private void login() {
        vertx.eventBus().<JsonObject>consumer(LOGIN_USER, message -> {
            User user = new User(message.body().getJsonObject("body"));
            if (user.isFilled()) {
                userRepository.login(user, res -> {
                    if (res.succeeded()) {
                        user.setId(res.result().getId());
                        String token = generateJWTToken(user);
                        jwtAuth.authenticate(new TokenCredentials(token), tokenRes -> {
                            LOGGER.info("Login: {} [{}]", user.getLogin(), LocalDateTime.now());
                            message.reply(new JsonObject().put("status", "OK").put("token", token));
                        });
                    } else message.reply(getErrorMessage(res.cause().getMessage()));
                });
            } else message.reply(getErrorMessage("Incorrect login data"));
        });
    }

    private String generateJWTToken(User user) {
        if (user.getId() == null) return null;
        JsonObject tokenData = new JsonObject()
                .put("id", user.getId().toString())
                .put("login", user.getLogin());
        jwt.sign(tokenData, new JWTOptions());
        return jwtAuth.generateToken(tokenData);
    }

    private void registerUser() {
        vertx.eventBus().<JsonObject>consumer(REGISTER_USER, message -> {
            User user = new User(message.body().getJsonObject("body"), UUID.randomUUID());
            if (user.isFilled()) {
                userRepository.register(user, res -> {
                    if (res.succeeded()) {
                        LOGGER.info("Register new user: {}", res.result().toString());
                        message.reply(new JsonObject().put("status", "OK")
                                .put("message", "Register completed. Please log into account"));
                    } else message.reply(getErrorMessage(res.cause().getMessage()));
                });
            } else message.reply(getErrorMessage("Incorrect user data"));
        });
    }

    private static JsonObject getErrorMessage(String errorMessage) {
        return new JsonObject().put("status", "FAILED").put("message", errorMessage);
    }

    private void throwUnauthorize(Message<JsonObject> message) {
        message.fail(403, "Unauthorized");
    }

    private void prepareDefaultHandler(RoutingContext rc, String address) {
        JsonObject requestPayload = new JsonObject()
                .put("token", rc.request().getHeader("Authorization"))
                .put("body", rc.body().asJsonObject())
                .put("id", rc.pathParam("id"));

        vertx.eventBus().<JsonObject>request(address, requestPayload, reply -> {
            if (reply.succeeded()) {
                rc.response().putHeader("Content-Type", "application/json");
                rc.response().end(reply.result().body().toString());
            } else {
                rc.fail(500);
            }
        });
    }
}
