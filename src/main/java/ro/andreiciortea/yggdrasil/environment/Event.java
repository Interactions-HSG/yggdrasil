package ro.andreiciortea.yggdrasil.environment;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

public class Event {
  private String name;
  private String description;
  private String type;
  private JsonElement forms;
  private JsonObject original;

  public Event(String name, String description, String type, JsonElement forms, JsonObject original) {
    this.name = name;
    this.description = description;
    this.type = type;
    this.forms = forms;
    this.original = original;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public JsonElement getForms() {
    return forms;
  }

  public void setForms(JsonElement forms) {
    this.forms = forms;
  }

  public JsonObject getOriginal() {
    return original;
  }

  public void setOriginal(JsonObject original) {
    this.original = original;
  }
}
