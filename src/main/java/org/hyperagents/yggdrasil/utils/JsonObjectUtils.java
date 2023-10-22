package org.hyperagents.yggdrasil.utils;

import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;

import java.util.Optional;
import java.util.function.BiFunction;

public class JsonObjectUtils {

  private JsonObjectUtils() {}

  public static Optional<String> getString(final JsonObject jsonObject, final String key, final Logger logger) {
    return JsonObjectUtils.getValue(jsonObject, key, JsonObject::getString, logger);
  }

  public static Optional<Boolean> getBoolean(final JsonObject jsonObject, final String key, final Logger logger) {
    return JsonObjectUtils.getValue(jsonObject, key, JsonObject::getBoolean, logger);
  }

  private static <T> Optional<T> getValue(
    final JsonObject jsonObject,
    final String key,
    final BiFunction<JsonObject, String, T> valueExtractor,
    final Logger logger
  ) {
    Optional<T> value = Optional.empty();
    try {
      value = Optional.ofNullable(valueExtractor.apply(jsonObject, key));
    } catch (final ClassCastException e) {
      logger.error("Exception raised while reading rdf-store config properties: " + e.getMessage());
    }
    return value;
  }
}
