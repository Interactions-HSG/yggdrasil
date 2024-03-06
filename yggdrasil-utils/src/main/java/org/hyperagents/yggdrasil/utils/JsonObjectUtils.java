package org.hyperagents.yggdrasil.utils;

import ch.unisg.ics.interactions.hmas.interaction.shapes.IntegerSpecification;
import ch.unisg.ics.interactions.hmas.interaction.shapes.QualifiedValueSpecification;
import com.google.gson.JsonElement;
import io.vertx.core.json.JsonArray;
import io.vertx.core.json.JsonObject;
import org.eclipse.rdf4j.model.vocabulary.RDF;


import java.util.List;
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


  // TODO: Put into HMAS Lib
  public static List<Object> parseInput(JsonElement jsonElement, QualifiedValueSpecification qualifiedValueSpecification, List<Object> result) {
    var semanticTypes =  qualifiedValueSpecification.getRequiredSemanticTypes();
    var properties = qualifiedValueSpecification.getPropertySpecifications();

    if (semanticTypes.contains(RDF.LIST.stringValue())) {
      assert jsonElement.isJsonArray();
      var jsonArray = jsonElement.getAsJsonArray();
      var firstProperty = properties.get(RDF.FIRST.stringValue());
      var restProperty = properties.get(RDF.REST.stringValue());
      var firstElement = jsonArray.remove(0);

      if (firstProperty instanceof IntegerSpecification) {
        result.add(firstElement.getAsInt());
      }

      if (restProperty instanceof QualifiedValueSpecification restQualifiedValueSpecification) {
          parseInput(jsonArray, restQualifiedValueSpecification, result);
      }
    }
    return result;
  }
}
