package ro.andreiciortea.yggdrasil.environment;

import com.google.gson.*;

import java.lang.reflect.Type;

public class ActionDeserializer implements JsonDeserializer<Action>
{

  @Override
  public Action deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException
  {
    JsonObject obj = json.getAsJsonObject();
    String name = (String) obj.keySet().toArray()[0];
    JsonObject propertyDesc = obj.getAsJsonObject(name);
    String type = propertyDesc.get("@type").getAsString();
    String description = propertyDesc.get("description").getAsString();
    JsonElement forms = propertyDesc.get("forms");

    return new Action(name, type, description, forms, obj);
  }
}
