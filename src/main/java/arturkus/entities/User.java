package arturkus.entities;

import io.vertx.core.json.JsonObject;

import java.util.UUID;


public class User {
    private UUID id;
    private String login;
    private String password;

    public User() {
    }

    public User(String login, String password) {
        this.login = login;
        this.password = password;
    }

    public User(JsonObject object) {
        this.login = object.getString("login");
        this.password = object.getString("password");
    }

    public User(JsonObject body, UUID id) {
        this(body);
        this.id = id;
    }

    public UUID getId() {
        return id;
    }

    public String getLogin() {
        return login;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public boolean isFilled() {
        return login != null && password != null;
    }

    public JsonObject getJsonObject() {
        return getUserWithoutId()
                .put("_id", id != null ? id.toString() : null);
    }

    public JsonObject getUserWithoutId() {
        return new JsonObject()
                .put("login", login)
                .put("password", password);
    }

    @Override
    public String toString() {
        return "{\"login\": \"" + login + "\"," + "\"password\": \"" + password + "\"}";
    }
}
