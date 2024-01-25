package arturkus.verticles;

import io.vertx.config.ConfigRetriever;
import io.vertx.config.ConfigRetrieverOptions;
import io.vertx.config.ConfigStoreOptions;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Vertx;
import io.vertx.core.json.JsonObject;

public class ConfigVerticle extends AbstractVerticle {

    public static final String CONFIG_FILE_NAME = "config.json";

    public static Future<JsonObject> getConfig(Vertx vertx) {
        ConfigStoreOptions defaultConfig = new ConfigStoreOptions()
                .setType("file")
                .setFormat("json")
                .setConfig(new JsonObject().put("path", CONFIG_FILE_NAME));

        ConfigRetrieverOptions retrieverOptions = new ConfigRetrieverOptions().addStore(defaultConfig);
        return ConfigRetriever.create(vertx, retrieverOptions).getConfig();
    }
}
