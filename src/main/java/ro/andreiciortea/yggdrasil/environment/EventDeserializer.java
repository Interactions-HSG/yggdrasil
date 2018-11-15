package ro.andreiciortea.yggdrasil.environment;

import com.google.gson.*;

import java.lang.reflect.Type;

public class EventDeserializer implements JsonDeserializer<Event>
{

  @Override
  public Event deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException
  {
    JsonObject obj = json.getAsJsonObject();
    String name = (String) obj.keySet().toArray()[0];
    JsonObject eventDesc = obj.getAsJsonObject(name);
    String type = eventDesc.get("@type").getAsString();
    String description = eventDesc.get("description").getAsString();
    JsonElement forms = eventDesc.get("forms");


    return new Event(name, type, description, forms, obj);
  }
}
