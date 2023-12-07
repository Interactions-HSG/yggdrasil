package org.hyperagents.yggdrasil.cartago;

import java.util.ArrayList;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class CartagoDataBundleTest {
  private static final String TEST_STRING = "bla";
  private static final String VALUES_EQUAL = "The values should be equal";
  private static final String TYPES_EQUAL = "The types should be equal";
  private static final String LENGTHS_EQUAL = "The lengths should be equal";

  @Test
  public void testDeserializeArrayOfPrimitives() {
    final var payload =
        "[ [ \"java.lang.Integer\", \"1\" ], "
        + "[ \"java.lang.Double\", \"1.5\" ], "
        + "[ \"java.lang.String\", \"my_test\" ], "
        + "[ \"java.lang.Boolean\", \"true\" ] ]";

    final var params = CartagoDataBundle.fromJson(payload);
    Assertions.assertEquals(4, params.length, LENGTHS_EQUAL);

    Assertions.assertTrue(params[0] instanceof Integer, TYPES_EQUAL);
    Assertions.assertEquals(1, params[0], VALUES_EQUAL);

    Assertions.assertTrue(params[1] instanceof Double, TYPES_EQUAL);
    Assertions.assertEquals(1.5, (Double) params[1], 0.001, VALUES_EQUAL);

    Assertions.assertEquals("my_test", params[2], VALUES_EQUAL);

    Assertions.assertTrue(params[3] instanceof Boolean, TYPES_EQUAL);
    Assertions.assertTrue((Boolean) params[3], VALUES_EQUAL);
  }

  @Test
  public void testDeserializeArrayIntegerDouble() {
    final var payload = "[ [ \"java.lang.Integer\", \"2\" ], [ \"java.lang.Double\", \"2\" ] ]";

    final var params = CartagoDataBundle.fromJson(payload);
    Assertions.assertEquals(2, params.length, LENGTHS_EQUAL);

    Assertions.assertTrue(params[0] instanceof Integer, TYPES_EQUAL);
    Assertions.assertEquals(2, params[0], VALUES_EQUAL);

    Assertions.assertTrue(params[1] instanceof Double, TYPES_EQUAL);
    Assertions.assertEquals(2, (Double) params[1], 0.001, VALUES_EQUAL);
  }

  @Test
  public void testDeserializeArrayDuplicateItems() {
    final var payload = "[ [ \"java.lang.Integer\", \"2\" ], [ \"java.lang.Integer\", \"2\" ] ]";

    final var params = CartagoDataBundle.fromJson(payload);
    Assertions.assertEquals(2, params.length, LENGTHS_EQUAL);

    Assertions.assertTrue(params[0] instanceof Integer, TYPES_EQUAL);
    Assertions.assertEquals(2, params[0], VALUES_EQUAL);

    Assertions.assertTrue(params[1] instanceof Integer, TYPES_EQUAL);
    Assertions.assertEquals(2, params[1], VALUES_EQUAL);
  }

  @Test
  public void testDeserializeNestedArraysOneLevel() {
    final var payload =
        "[ [\"java.lang.Integer\", \"1\" ],"
        + "[\"java.util.List\", [[\"java.lang.Double\",\"1.5\"],[\"java.lang.Boolean\",\"true\"]]],"
        + "[\"java.lang.String\", \""
        + TEST_STRING
        + "\"] ]";

    final var params = CartagoDataBundle.fromJson(payload);
    Assertions.assertEquals(3, params.length, LENGTHS_EQUAL);

    Assertions.assertTrue(params[0] instanceof Integer, TYPES_EQUAL);
    Assertions.assertEquals(1, params[0], VALUES_EQUAL);
    Assertions.assertEquals(TEST_STRING, params[2], VALUES_EQUAL);

    Assertions.assertTrue(params[1] instanceof Object[], TYPES_EQUAL);
    final var innerArray = (Object[]) params[1];
    Assertions.assertEquals(2, innerArray.length, LENGTHS_EQUAL);

    Assertions.assertTrue(innerArray[0] instanceof Double, TYPES_EQUAL);
    Assertions.assertEquals(1.5, (Double) innerArray[0], 0.001, VALUES_EQUAL);

    Assertions.assertTrue(innerArray[1] instanceof Boolean, TYPES_EQUAL);
    Assertions.assertTrue((Boolean) innerArray[1], VALUES_EQUAL);
  }

  @Test
  public void testDeserializeNestedArraysThreeLevels() {
    final var payload =
        "[[\"java.util.List\",[[\"java.util.List\",[[\"java.lang.Double\",\"2.5\"],"
        + "[\"java.lang.String\",\""
        + TEST_STRING
        + "\"]]]]]]";

    final var level1 = CartagoDataBundle.fromJson(payload);
    Assertions.assertEquals(1, level1.length, LENGTHS_EQUAL);
    Assertions.assertTrue(level1[0] instanceof Object[], TYPES_EQUAL);

    final var level2 = (Object[]) level1[0];
    Assertions.assertEquals(1, level2.length, LENGTHS_EQUAL);
    Assertions.assertTrue(level2[0] instanceof Object[], TYPES_EQUAL);

    final var level3 = (Object[]) level2[0];
    Assertions.assertEquals(2, level3.length, LENGTHS_EQUAL);
    Assertions.assertTrue(level3[0] instanceof Double, TYPES_EQUAL);
    Assertions.assertEquals(2.5, (Double) level3[0], 0.001, VALUES_EQUAL);
    Assertions.assertEquals(TEST_STRING, level3[1], VALUES_EQUAL);
  }

  @Test
  public void testSerializeNestedArraysOneLevel() {
    final var level2 = new ArrayList<>();
    level2.add(1.5);
    level2.add(true);

    final var level1 = new ArrayList<>();
    level1.add(1);
    level1.add(level2);
    level1.add(TEST_STRING);

    final var expected =
        "[[\"java.lang.Integer\",\"1\"],"
        + "[\"java.util.List\",[[\"java.lang.Double\",\"1.5\"],[\"java.lang.Boolean\",\"true\"]]],"
        + "[\"java.lang.String\",\""
        + TEST_STRING
        + "\"]]";

    Assertions.assertEquals(
        expected,
        CartagoDataBundle.toJson(level1),
        "The serialization should be correct"
    );
  }

  @Test
  public void testSerializeNestedArraysThreeLevels() {
    final var level3 = new ArrayList<>();
    level3.add(2.5);
    level3.add(TEST_STRING);

    final var level2 = new ArrayList<>();
    level2.add(level3);

    final var level1 = new ArrayList<>();
    level1.add(level2);

    final var expected =
        "[[\"java.util.List\",[[\"java.util.List\",[[\"java.lang.Double\",\"2.5\"],"
        + "[\"java.lang.String\",\""
        + TEST_STRING
        + "\"]]]]]]";

    Assertions.assertEquals(
        expected,
        CartagoDataBundle.toJson(level1),
        "The serialization should be correct"
    );
  }
}
