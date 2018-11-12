package ro.andreiciortea.yggdrasil.mas;

import com.google.gson.*;

import java.lang.reflect.Type;

public class PropertyDeserializer implements JsonDeserializer<Property>
{

  @Override
  public Property deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException
  {
    JsonObject obj = json.getAsJsonObject();
    String name = (String) obj.keySet().toArray()[0];
    JsonObject propertyDesc = obj.getAsJsonObject(name);
    String type = propertyDesc.get("@type").getAsString();
    String description = propertyDesc.get("description").getAsString();
    boolean writable = propertyDesc.get("writable").getAsBoolean();
    JsonElement forms = propertyDesc.get("forms");

    return new Property(name, type, description, writable, forms, obj);
  }
}
