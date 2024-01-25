package arturkus.utils;

import io.vertx.core.json.JsonObject;
import io.vertx.ext.auth.KeyStoreOptions;
import io.vertx.ext.auth.jwt.JWTAuthOptions;

public class JWTUtils {

    private static final String JWT_KEY = "jwt";
    private static final String JWT_SECRET_KEY = "secret";

    public static JWTAuthOptions getJWTConfig(JsonObject config) {
        JsonObject jwtConfig = config.getJsonObject(JWT_KEY);
        return new JWTAuthOptions()
                .setKeyStore(new KeyStoreOptions()
                        .setPath("keystore.jceks")
                        .setType("jceks")
                        .setPassword(jwtConfig.getString(JWT_SECRET_KEY)));
    }


    public static String extractToken(String bearerToken) {
        bearerToken = bearerToken.trim();
        if (bearerToken.startsWith("Bearer")) {
            return bearerToken.split("\\s+")[1];
        } else return null;
    }
}
