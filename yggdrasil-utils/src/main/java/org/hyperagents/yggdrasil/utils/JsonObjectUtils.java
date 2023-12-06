package org.hyperagents.yggdrasil.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public final class JsonObjectUtils {

  private JsonObjectUtils() {}

  public static Optional<String> getString(
      final JsonObject jsonObject,
      final String key,
      final Consumer<String> logger
  ) {
    return getValue(jsonObject, key, JsonObject::getString, logger);
  }

  public static Optional<Boolean> getBoolean(
      final JsonObject jsonObject,
      final String key,
      final Consumer<String> logger
  ) {
    return getValue(jsonObject, key, JsonObject::getBoolean, logger);
  }

  public static Optional<JsonObject> getJsonObject(
      final JsonObject jsonObject,
      final String key,
      final Consumer<String> logger
  ) {
    return getValue(jsonObject, key, JsonObject::getJsonObject, logger);
  }

  public static Optional<JsonArray> getJsonArray(
      final JsonObject jsonObject,
      final String key,
      final Consumer<String> logger
  ) {
    return getValue(jsonObject, key, JsonObject::getJsonArray, logger);
  }

  private static <T> Optional<T> getValue(
      final JsonObject jsonObject,
      final String key,
      final BiFunction<JsonObject, String, T> valueExtractor,
      final Consumer<String> logger
  ) {
    try {
      return Optional.ofNullable(valueExtractor.apply(jsonObject, key));
    } catch (final ClassCastException e) {
      logger.accept("Exception raised while reading rdf-store config properties: " + e.getMessage());
    }
    return Optional.empty();
  }
}
