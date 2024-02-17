package org.hyperagents.yggdrasil.utils;

import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.function.Consumer;

/**
 * Utility class for working with JSON objects.
 */
public final class JsonObjectUtils {

  private JsonObjectUtils() {}

  /**
   * Retrieves an optional integer value from a JSON object based on the specified key.
   *
   * @param jsonObject the JSON object to retrieve the value from
   * @param key the key of the value to retrieve
   * @param logger a consumer for logging any exceptions that occur
   * @return an optional integer value
   */
  public static Optional<Integer> getInteger(
      final JsonObject jsonObject,
      final String key,
      final Consumer<String> logger
  ) {
    return getValue(jsonObject, key, JsonObject::getInteger, logger);
  }

  /**
   * Retrieves an optional string value from a JSON object based on the specified key.
   *
   * @param jsonObject the JSON object to retrieve the value from
   * @param key the key of the value to retrieve
   * @param logger a consumer for logging any exceptions that occur
   * @return an optional string value
   */
  public static Optional<String> getString(
      final JsonObject jsonObject,
      final String key,
      final Consumer<String> logger
  ) {
    return getValue(jsonObject, key, JsonObject::getString, logger);
  }

  /**
   * Retrieves an optional boolean value from a JSON object based on the specified key.
   *
   * @param jsonObject the JSON object to retrieve the value from
   * @param key the key of the value to retrieve
   * @param logger a consumer for logging any exceptions that occur
   * @return an optional boolean value
   */
  public static Optional<Boolean> getBoolean(
      final JsonObject jsonObject,
      final String key,
      final Consumer<String> logger
  ) {
    return getValue(jsonObject, key, JsonObject::getBoolean, logger);
  }

  /**
   * Retrieves an optional JSON object value from a JSON object based on the specified key.
   *
   * @param jsonObject the JSON object to retrieve the value from
   * @param key the key of the value to retrieve
   * @param logger a consumer for logging any exceptions that occur
   * @return an optional JSON object value
   */
  public static Optional<JsonObject> getJsonObject(
      final JsonObject jsonObject,
      final String key,
      final Consumer<String> logger
  ) {
    return getValue(jsonObject, key, JsonObject::getJsonObject, logger);
  }

  /**
   * Retrieves an optional JSON array value from a JSON object based on the specified key.
   *
   * @param jsonObject the JSON object to retrieve the value from
   * @param key the key of the value to retrieve
   * @param logger a consumer for logging any exceptions that occur
   * @return an optional JSON array value
   */
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
      logger.accept(
          "Exception raised while reading rdf-store config properties: " + e.getMessage()
      );
    }
    return Optional.empty();
  }
}
