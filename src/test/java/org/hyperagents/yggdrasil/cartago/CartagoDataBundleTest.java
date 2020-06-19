package org.hyperagents.yggdrasil.cartago;

import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;

import io.vertx.ext.unit.TestContext;
import io.vertx.ext.unit.junit.VertxUnitRunner;

@RunWith(VertxUnitRunner.class)
public class CartagoDataBundleTest {
  
  @Test
  public void testDeserializeArrayOfPrimitives(TestContext tc) {
    String payload = "[ [ \"java.lang.Integer\", \"1\" ], "
        + "[ \"java.lang.Double\", \"1.5\" ], "
        + "[ \"java.lang.String\", \"my_test\" ], "
        + "[ \"java.lang.Boolean\", \"true\" ] ]";
    
    Object[] params = CartagoDataBundle.fromJson(payload);
    tc.assertEquals(4, params.length);
    
    tc.assertTrue(params[0] instanceof Integer);
    tc.assertEquals(1, params[0]);
    
    tc.assertTrue(params[1] instanceof Double);
    tc.assertInRange(1.5, (Double) params[1], 0.001);
    
    tc.assertEquals("my_test", (String) params[2]);
    
    tc.assertTrue(params[3] instanceof Boolean);
    tc.assertTrue((Boolean) params[3]);
  }
  
  @Test
  public void testDeserializeArrayIntegerDouble(TestContext tc) {
    String payload = "[ [ \"java.lang.Integer\", \"2\" ], "
        + "[ \"java.lang.Double\", \"2\" ] ]";
    
    Object[] params = CartagoDataBundle.fromJson(payload);
    tc.assertEquals(2, params.length);
    
    tc.assertTrue(params[0] instanceof Integer);
    tc.assertEquals(2, params[0]);
    
    tc.assertTrue(params[1] instanceof Double);
    tc.assertInRange(2, (Double) params[1], 0.001);
  }
  
  @Test
  public void testDeserializeArrayDuplicateItems(TestContext tc) {
    String payload = "[ [ \"java.lang.Integer\", \"2\" ], "
        + "[ \"java.lang.Integer\", \"2\" ] ]";
    
    Object[] params = CartagoDataBundle.fromJson(payload);
    tc.assertEquals(2, params.length);
    
    tc.assertTrue(params[0] instanceof Integer);
    tc.assertEquals(2, params[0]);
    
    tc.assertTrue(params[1] instanceof Integer);
    tc.assertEquals(2, params[1]);
  }
  
  @Test
  public void testDeserializeNestedArraysOneLevel(TestContext tc) {
    String payload = "[ [\"java.lang.Integer\", \"1\" ],"
        + "[\"java.util.List\", [[\"java.lang.Double\",\"1.5\"],[\"java.lang.Boolean\",\"true\"]]],"
        + "[\"java.lang.String\", \"bla\"] ]";
    
    Object[] params = CartagoDataBundle.fromJson(payload);
    tc.assertEquals(3, params.length);
    
    tc.assertTrue(params[0] instanceof Integer);
    tc.assertEquals(1, params[0]);
    tc.assertEquals("bla", (String) params[2]);
    
    tc.assertTrue(params[1] instanceof Object[]);
    Object[] innerArray = (Object[]) params[1];
    tc.assertEquals(2, innerArray.length);
    
    tc.assertTrue(innerArray[0] instanceof Double);
    tc.assertInRange(1.5, (Double) innerArray[0], 0.001);
    
    tc.assertTrue(innerArray[1] instanceof Boolean);
    tc.assertTrue((Boolean) innerArray[1]);
  }
  
  @Test
  public void testDeserializeNestedArraysThreeLevels(TestContext tc) {
    String payload = "[[\"java.util.List\",[[\"java.util.List\",[[\"java.lang.Double\",\"2.5\"],"
        + "[\"java.lang.String\",\"bla\"]]]]]]";
    
    Object[] level1 = CartagoDataBundle.fromJson(payload);
    tc.assertEquals(1, level1.length);
    tc.assertTrue(level1[0] instanceof Object[]);
    
    Object[] level2 = (Object[]) level1[0];
    tc.assertEquals(1, level2.length);
    tc.assertTrue(level2[0] instanceof Object[]);
    
    Object[] level3 = (Object[]) level2[0];
    tc.assertEquals(2, level3.length);
    tc.assertTrue(level3[0] instanceof Double);
    tc.assertInRange(2.5, (Double) level3[0], 0.001);
    tc.assertEquals("bla", level3[1]);
  }
  
  @Test
  public void testSerializeNestedArraysOneLevel(TestContext tc) {
    List<Object> level2 = new ArrayList<Object>();
    level2.add(1.5);
    level2.add(true);
    
    List<Object> level1 = new ArrayList<Object>();
    level1.add(1);
    level1.add(level2);
    level1.add("bla");
    
    String expected = "[[\"java.lang.Integer\",\"1\"],"
        + "[\"java.util.List\",[[\"java.lang.Double\",\"1.5\"],[\"java.lang.Boolean\",\"true\"]]],"
        + "[\"java.lang.String\",\"bla\"]]";
    
    tc.assertEquals(expected, CartagoDataBundle.toJson(level1)); 
  }
  
  @Test
  public void testSerializeNestedArraysThreeLevels(TestContext tc) {
    List<Object> level3 = new ArrayList<Object>();
    level3.add(2.5);
    level3.add("bla");
    
    List<Object> level2 = new ArrayList<Object>();
    level2.add(level3);
    
    List<Object> level1 = new ArrayList<Object>();
    level1.add(level2);
    
    String expected = "[[\"java.util.List\",[[\"java.util.List\",[[\"java.lang.Double\",\"2.5\"],"
        + "[\"java.lang.String\",\"bla\"]]]]]]";
    
    tc.assertEquals(expected, CartagoDataBundle.toJson(level1));
  }
}
