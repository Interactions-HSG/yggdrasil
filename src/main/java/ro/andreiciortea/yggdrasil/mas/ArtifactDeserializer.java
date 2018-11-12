package ro.andreiciortea.yggdrasil.mas;

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

public class ArtifactDeserializer implements JsonDeserializer<Artifact>
{

  @Override
  public Artifact deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext ctx) throws JsonParseException
  {
    List<Property> propertyList;
    List<Action> actionList;
    List<Event> eventList;

    // Get thing properties
    JsonObject obj = json.getAsJsonObject();
    String idString = obj.get("id").getAsString();
    String nameString = obj.get("name").getAsString();

    // Assemble properties
    Type propertyListType = new TypeToken<List<Property>>(){}.getType();

    Gson propertyBuilder =
      new GsonBuilder()
        .registerTypeAdapter(Property.class, new PropertyDeserializer())
        .create();
    JsonElement propertiesJson = obj.get("properties");
    if (propertiesJson.isJsonArray()) {
      propertyList = propertyBuilder.fromJson(propertiesJson, propertyListType);
    } else {
      propertyList = new ArrayList<>();
      Property property = propertyBuilder.fromJson(propertiesJson, Property.class);
      propertyList.add(property);
    }

    Property[] propertyArray = new Property[propertyList.size()];
    propertyArray = propertyList.toArray(propertyArray);

    // Assemble actions
    Type actionListType = new TypeToken<List<Action>>(){}.getType();

    Gson actionBuilder =
      new GsonBuilder()
        .registerTypeAdapter(Action.class, new ActionDeserializer())
        .create();

    JsonElement actionsJson = obj.get("actions");
    if (actionsJson.isJsonArray()) {
      actionList = actionBuilder.fromJson(actionsJson, actionListType);
    } else {
      actionList = new ArrayList<>();
      Action action = actionBuilder.fromJson(actionsJson, Action.class);
      actionList.add(action);
    }

    Action[] actionArray = new Action[actionList.size()];
    actionArray = actionList.toArray(actionArray);

    // Assemble events
    Type eventListType = new TypeToken<List<Event>>(){}.getType();

    Gson eventBuilder =
      new GsonBuilder()
        .registerTypeAdapter(Event.class, new EventDeserializer())
        .create();

    JsonElement eventsJson = obj.get("events");
    if (eventsJson.isJsonArray()) {
      eventList = eventBuilder.fromJson(eventsJson, eventListType);
    } else {
      eventList = new ArrayList<>();
      Event event = actionBuilder.fromJson(eventsJson, Event.class);
    }

    Event[] eventArray = new Event[eventList.size()];
    eventArray = actionList.toArray(eventArray);

    return new Artifact(idString, nameString, propertyArray, actionArray, eventArray);
  }
}
