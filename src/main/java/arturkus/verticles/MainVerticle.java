package arturkus.verticles;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Vertx;
import io.vertx.core.VertxOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MainVerticle extends AbstractVerticle {

    private static final Logger LOGGER = LoggerFactory.getLogger(MainVerticle.class);

    public static void main(String[] args) {
        Vertx vertx = Vertx.vertx(new VertxOptions());
        vertx.deployVerticle(new MainVerticle());
    }

    @Override
    public void start() {
        ConfigVerticle.getConfig(vertx).onComplete(config -> {
            if (config.succeeded()) {
                vertx.deployVerticle(new WebVerticle(vertx, config.result()));
            } else {
                LOGGER.error("Cannot get application configuration.");
            }
        });
    }
}
