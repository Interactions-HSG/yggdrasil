package org.hyperagents.yggdrasil.cartago;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(VertxUnitRunner.class)
public class CartagoDataBundleTest {
  private static final String TEST_STRING = "bla";

  @Test
  public void testDeserializeArrayOfPrimitives(final TestContext tc) {
    final var payload =
        "[ [ \"java.lang.Integer\", \"1\" ], "
        + "[ \"java.lang.Double\", \"1.5\" ], "
        + "[ \"java.lang.String\", \"my_test\" ], "
        + "[ \"java.lang.Boolean\", \"true\" ] ]";

    final var params = CartagoDataBundle.fromJson(payload);
    tc.assertEquals(4, params.length);

    tc.assertTrue(params[0] instanceof Integer);
    tc.assertEquals(1, params[0]);

    tc.assertTrue(params[1] instanceof Double);
    tc.assertInRange(1.5, (Double) params[1], 0.001);

    tc.assertEquals("my_test", params[2]);

    tc.assertTrue(params[3] instanceof Boolean);
    tc.assertTrue((Boolean) params[3]);
  }

  @Test
  public void testDeserializeArrayIntegerDouble(final TestContext tc) {
    final var payload = "[ [ \"java.lang.Integer\", \"2\" ], [ \"java.lang.Double\", \"2\" ] ]";

    final var params = CartagoDataBundle.fromJson(payload);
    tc.assertEquals(2, params.length);

    tc.assertTrue(params[0] instanceof Integer);
    tc.assertEquals(2, params[0]);

    tc.assertTrue(params[1] instanceof Double);
    tc.assertInRange(2, (Double) params[1], 0.001);
  }

  @Test
  public void testDeserializeArrayDuplicateItems(final TestContext tc) {
    final var payload = "[ [ \"java.lang.Integer\", \"2\" ], [ \"java.lang.Integer\", \"2\" ] ]";

    final var params = CartagoDataBundle.fromJson(payload);
    tc.assertEquals(2, params.length);

    tc.assertTrue(params[0] instanceof Integer);
    tc.assertEquals(2, params[0]);

    tc.assertTrue(params[1] instanceof Integer);
    tc.assertEquals(2, params[1]);
  }

  @Test
  public void testDeserializeNestedArraysOneLevel(final TestContext tc) {
    final var payload =
        "[ [\"java.lang.Integer\", \"1\" ],"
        + "[\"java.util.List\", [[\"java.lang.Double\",\"1.5\"],[\"java.lang.Boolean\",\"true\"]]],"
        + "[\"java.lang.String\", \""
        + TEST_STRING
        + "\"] ]";

    final var params = CartagoDataBundle.fromJson(payload);
    tc.assertEquals(3, params.length);

    tc.assertTrue(params[0] instanceof Integer);
    tc.assertEquals(1, params[0]);
    tc.assertEquals(TEST_STRING, params[2]);

    tc.assertTrue(params[1] instanceof Object[]);
    final var innerArray = (Object[]) params[1];
    tc.assertEquals(2, innerArray.length);

    tc.assertTrue(innerArray[0] instanceof Double);
    tc.assertInRange(1.5, (Double) innerArray[0], 0.001);

    tc.assertTrue(innerArray[1] instanceof Boolean);
    tc.assertTrue((Boolean) innerArray[1]);
  }

  @Test
  public void testDeserializeNestedArraysThreeLevels(final TestContext tc) {
    final var payload =
        "[[\"java.util.List\",[[\"java.util.List\",[[\"java.lang.Double\",\"2.5\"],"
        + "[\"java.lang.String\",\""
        + TEST_STRING
        + "\"]]]]]]";

    final var level1 = CartagoDataBundle.fromJson(payload);
    tc.assertEquals(1, level1.length);
    tc.assertTrue(level1[0] instanceof Object[]);

    final var level2 = (Object[]) level1[0];
    tc.assertEquals(1, level2.length);
    tc.assertTrue(level2[0] instanceof Object[]);

    final var level3 = (Object[]) level2[0];
    tc.assertEquals(2, level3.length);
    tc.assertTrue(level3[0] instanceof Double);
    tc.assertInRange(2.5, (Double) level3[0], 0.001);
    tc.assertEquals(TEST_STRING, level3[1]);
  }

  @Test
  public void testSerializeNestedArraysOneLevel(final TestContext tc) {
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

    tc.assertEquals(expected, CartagoDataBundle.toJson(level1));
  }

  @Test
  public void testSerializeNestedArraysThreeLevels(final TestContext tc) {
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

    tc.assertEquals(expected, CartagoDataBundle.toJson(level1));
  }
}
