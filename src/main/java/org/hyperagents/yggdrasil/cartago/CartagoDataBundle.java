package org.hyperagents.yggdrasil.cartago;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;

import io.vertx.core.json.Json;

/**
 * A class for serializing and deserializing CArtAgO datatypes to JSON. This is used for
 * sending action parameters via the event bus.
 * 
 * @author Andrei Ciortea
 *
 */
public final class CartagoDataBundle {

  public static String toJson(List<Object> params) {
    List<List<Object>> typedParams = objectListToTypedList(params);
    return Json.encode(typedParams);
  }
  
  public static Object[] fromJson(String representation) {
    TypeReference<List<List<Object>>> paramListType = new TypeReference<List<List<Object>>>() {};
    List<List<Object>> typedParams = Json.decodeValue(representation, paramListType);
    return typedListToObjectList(typedParams).toArray();
  }
  
  @SuppressWarnings("unchecked")
  private static List<Object> typedListToObjectList(List<List<Object>> typedParams) {
    return typedParams.stream().map(param -> 
        {
          String type = (String) param.get(0);
          if (type.compareTo(String.class.getCanonicalName()) == 0) {
            return param.get(1);
          } else if (type.compareTo(Integer.class.getCanonicalName()) == 0) {
            return Integer.valueOf((String) param.get(1));
          } else if (type.compareTo(Double.class.getCanonicalName()) == 0) {
            return Double.valueOf((String) param.get(1));
          } else if (type.compareTo(Boolean.class.getCanonicalName()) == 0) {
            return Boolean.valueOf((String) param.get(1));
          } else if (type.compareTo(List.class.getCanonicalName()) == 0) {
            return typedListToObjectList((List<List<Object>>) param.get(1)).toArray();
          } else {
            return null;
          }
        })
        .collect(Collectors.toList());
  }
  
  @SuppressWarnings("unchecked")
  private static List<List<Object>> objectListToTypedList(List<Object> params) {
    return params.stream().map(param -> {
          List<Object> typedParam = new ArrayList<Object>();
          
          if (param instanceof List<?>) {
            typedParam.add(List.class.getCanonicalName());
            typedParam.add(objectListToTypedList((List<Object>) param));
          } else {
            typedParam.add(param.getClass().getCanonicalName());
            typedParam.add(String.valueOf(param));
          }
          
          return typedParam; 
        }).collect(Collectors.toList());
  }
}
