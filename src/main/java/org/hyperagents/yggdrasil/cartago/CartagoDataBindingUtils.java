package org.hyperagents.yggdrasil.cartago;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public final class CartagoDataBindingUtils {

  public static String toJson(List<Object> params) {
    List<List<Object>> typedParams = objectListToTypedList(params);
    return new Gson().toJson(typedParams);
  }
  
  public static Object[] fromJson(String representation) {
    Type paramListType = new TypeToken<List<List<Object>>>() {}.getType();
    List<List<Object>> typedParams = new Gson().fromJson(representation, paramListType);
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
