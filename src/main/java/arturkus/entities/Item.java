package arturkus.entities;

import io.vertx.core.json.JsonObject;

import java.util.UUID;

public class Item {
    private UUID id;
    private String name;
    private UUID userId;

    public Item(JsonObject message) {
        this.id = UUID.randomUUID();
        JsonObject body = message.getJsonObject("body");
        this.name = (body != null) ? body.getString("name") : null;
        this.userId = UUID.fromString(message.getString("userId"));
    }

    public Item(JsonObject item, UUID id) {
        this.id = id;
        this.name = item.getString("name");
        String userId = item.getString("userId");
        this.userId = userId != null ? UUID.fromString(userId) : null;
    }

    public JsonObject getJsonObject(Boolean idWithUnderscore) {
        return getItemWithoutId()
                .put(idWithUnderscore ? "_id" : "id", id != null ? id.toString() : null);
    }

    public JsonObject getItemWithoutId() {
        return new JsonObject()
                .put("name", name)
                .put("userId", userId != null ? userId.toString() : null);
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public boolean isFilled() {
        return name != null;
    }

    @Override
    public String toString() {
        return "Item{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", userId=" + userId +
                '}';
    }
}