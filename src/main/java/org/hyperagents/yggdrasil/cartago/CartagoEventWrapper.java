package org.hyperagents.yggdrasil.cartago;

import cartago.*;
import cartago.events.*;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public class CartagoEventWrapper {

  private CartagoEvent event;

  private String type;

  public CartagoEventWrapper(CartagoEvent event){
    this.event = event;
    if (event instanceof ObsArtListChangedEvent){
      type = "obsArtListChangedEvent";
    }
    else if (event instanceof ConsultManualSucceededEvent){
      type = "consultManualSucceededEvent";
    }
    else if (event instanceof  FocusSucceededEvent){
      type = "focusSucceededEvent";
    }
    else if (event instanceof JoinWSPSucceededEvent){
      type = "joinWSPSucceededEvent";
    }
    else if (event instanceof QuitWSPSucceededEvent){
      type = "quitWSPSucceededEvent";
    }
    else if (event instanceof StopFocusSucceededEvent){
      type = "stopFocusSucceededEvent";
    }
    else if (event instanceof ActionSucceededEvent){
      type = "actionSucceededEvent";
    }
    else if (event instanceof ActionFailedEvent){
      type = "actionFailedEvent";
    }
    else if (event instanceof CartagoActionEvent){
      type = "cartagoActionEvent";
    }
    else if (event instanceof FocussedArtifactDisposedEvent){
      type = "focussedArtifactDisposedEvent";
    }
    else if (event instanceof ArtifactObsEvent){
      type = "artifactObsEvent";
    }
    else {
      type = "cartagoEvent";
    }
  }

  public String getType(){
    return type;
  }

  public CartagoEvent getEvent(){
    return event;
  }

  public Optional<ObsArtListChangedEvent> getObsArtListChangedEvent(){
    Optional<ObsArtListChangedEvent> opEvent = Optional.empty();
    if (type == "obsArtListChangedEvent"){
      opEvent = Optional.of((ObsArtListChangedEvent) event);
    }
    return opEvent;
  }

  public JsonObject getJson(){
    if (event instanceof ObsArtListChangedEvent){
      return getJsonObsArtListChangedEvent();
    }
    else if (event instanceof ConsultManualSucceededEvent){
      return getJsonConsultManualSucceededEvent();
    }
    else if (event instanceof  FocusSucceededEvent){
      return getJsonFocusSucceededEvent();
    }
    else if (event instanceof JoinWSPSucceededEvent){
      return getJsonJoinWSPSucceededEvent();
    }
    else if (event instanceof QuitWSPSucceededEvent){
      return getJsonQuitWSPSucceededEvent();
    }
    else if (event instanceof StopFocusSucceededEvent){
      return getJsonStopFocusSucceededEvent();
    }
    else if (event instanceof ActionSucceededEvent){
      return getJsonActionSucceededEvent();
    }
    else if (event instanceof ActionFailedEvent){
      return getJsonActionFailedEvent();
    }
    else if (event instanceof CartagoActionEvent){
      return getJsonCartagoActionEvent();
    }
    else if (event instanceof FocussedArtifactDisposedEvent){
      return getJsonFocussedArtifactDisposedEvent();
    }
    else if (event instanceof ArtifactObsEvent){
      return getJsonArtifactObsEvent();
    }
    else {
      return getJsonCartagoEvent();
    }
  }

  public JsonObject getJsonCartagoEvent(){
    JsonObject object = new JsonObject();
    object.addProperty("type", type);
    object.addProperty("timestamp", event.getTimestamp());
    object.addProperty("id", event.getId());
    return object;

  }

  public JsonArray getJsonObjects(Object[] objs){
    List<Object> objects = Arrays.asList(objs);
    JsonArray array = new JsonArray();
    for (Object obj: objects){
      array.add(obj.toString());
    }
    return array;

  }

  public JsonObject getJsonOperation(Op operation){
    JsonObject object = new JsonObject();
    object.addProperty("name", operation.getName());
    object.add("paramValues", getJsonObjects(operation.getParamValues()));
    return object;
  }

  public JsonObject getJsonCartagoActionEvent(){
    JsonObject object = new JsonObject();
    CartagoActionEvent actionEvent = (CartagoActionEvent) event;
    object.addProperty("type", type);
    object.addProperty("timestamp", actionEvent.getTimestamp());
    object.addProperty("id", actionEvent.getId());
    object.addProperty("actionId", actionEvent.getActionId());
    object.add("operation", getJsonOperation(actionEvent.getOp()));
    return object;

  }

  public JsonObject getJsonArtifactId(ArtifactId artifactId){
    JsonObject object = new JsonObject();
    object.addProperty("name", artifactId.getName());
    object.addProperty("artifactType", artifactId.getArtifactType());
    object.addProperty("creatorId", artifactId.getCreatorId().toString());
    object.addProperty("id", artifactId.getId().toString());
    object.addProperty("workspaceId", artifactId.getWorkspaceId().toString());
    return object;
  }

  public JsonObject getJsonActionSucceededEvent(){
    JsonObject object = getJsonCartagoActionEvent();
    ActionSucceededEvent succeededEvent = (ActionSucceededEvent) event;
    object.add("artifactId", getJsonArtifactId(succeededEvent.getArtifactId()) );
    return object;
  }

  public JsonObject getJsonTuple(Tuple tuple){
    JsonObject object = new JsonObject();
    object.addProperty("label", tuple.getLabel());
    object.add("contents", getJsonObjects(tuple.getContents()));
    return object;
  }

  public JsonObject getJsonActionFailedEvent(){
    JsonObject object = getJsonCartagoActionEvent();
    ActionFailedEvent failedEvent = (ActionFailedEvent) event;
    object.addProperty("failureMsg", failedEvent.getFailureMsg());
    object.add("failureDescr", getJsonTuple(failedEvent.getFailureDescr()));
    return object;

  }

  public JsonObject getJsonObsProperty(ArtifactObsProperty property){
    JsonObject object = new JsonObject();
    object.addProperty("name", property.getName());
    object.addProperty("id", property.getId());
    object.addProperty("fullId", property.getFullId());
    object.add("values", getJsonObjects(property.getValues()));
    return object;
  }

  public JsonArray getJsonObsPropertyList(List<ArtifactObsProperty> properties){
    JsonArray array = new JsonArray();
    for (ArtifactObsProperty property: properties){
      array.add(getJsonObsProperty(property));
    }
    return array;
  }

  public JsonArray getJsonObsPropertyArray(ArtifactObsProperty[] properties){
    JsonArray array = new JsonArray();
    List<ArtifactObsProperty> propertyList = Arrays.asList(properties);
    for (ArtifactObsProperty property: propertyList){
      array.add(getJsonObsProperty(property));
    }
    return array;
  }


  public JsonObject getJsonArtifactObsEvent(){
    JsonObject object = getJsonCartagoEvent();
    ArtifactObsEvent obsEvent = (ArtifactObsEvent) event;
    object.add("artifactId", getJsonArtifactId(obsEvent.getArtifactId()));
    object.add("signal", getJsonTuple(obsEvent.getSignal()));
    object.add("addedProperties", getJsonObsPropertyArray(obsEvent.getAddedProperties()));
    object.add("changedProperties", getJsonObsPropertyArray(obsEvent.getChangedProperties()));
    object.add("removedProperties", getJsonObsPropertyArray(obsEvent.getRemovedProperties()));
    return object;
  }

  public JsonObject getJsonArtifactInfo(ObservableArtifactInfo info){
    JsonObject object = new JsonObject();
    object.add("targetArtifact", getJsonArtifactId(info.getTargetArtifact()));
    return object;
  }

  public JsonArray getJsonArtifactInfoList(List<ObservableArtifactInfo> infos){
    JsonArray array = new JsonArray();
    for (ObservableArtifactInfo info : infos){
      array.add(getJsonArtifactInfo(info));
    }
    return array;
  }

  public JsonObject getJsonObsArtListChangedEvent(){
    JsonObject object = getJsonCartagoEvent();
    ObsArtListChangedEvent changedEvent = (ObsArtListChangedEvent) event;
    object.add("newFocused", getJsonArtifactInfoList(changedEvent.getNewFocused()));
    object.add("noMoreFocused", getJsonArtifactInfoList(changedEvent.getNewFocused()));
    return object;
  }

  public JsonObject getJsonUri(URI uri){
    JsonObject object = new JsonObject();
    return object;
  }

  public JsonObject getJsonUsageProtocol(UsageProtocol protocol){
    JsonObject object = new JsonObject();
    object.addProperty("signature", protocol.getSignature());
    object.addProperty("body", protocol.getBody().toString());
    object.addProperty("function", protocol.getFunction().toString());
    object.addProperty("precondition", protocol.getPrecondition().toString());
    return object;
  }

  public JsonArray getJsonUsageProtocols(List<UsageProtocol> protocols){
    JsonArray array = new JsonArray();
    return array;
  }

  public JsonObject getJsonManual(Manual manual){
    JsonObject object = new JsonObject();
    object.addProperty("name", manual.getName());
    object.addProperty("source", manual.getSource());
    object.add("uri", getJsonUri(manual.getURI()));
    object.add("usageProtocols", getJsonUsageProtocols(manual.getUsageProtocols()));
    return object;
  }

  public JsonObject getJsonConsultManualSucceededEvent(){
    JsonObject object = getJsonActionSucceededEvent();
    ConsultManualSucceededEvent manualEvent = (ConsultManualSucceededEvent) event;
    object.add("manual", getJsonManual(manualEvent.getManual()));
    return object;
  }

  public JsonObject getJsonFocusSucceededEvent(){
    JsonObject object = getJsonActionSucceededEvent();
    FocusSucceededEvent focusEvent = (FocusSucceededEvent) event;
    object.add("targetArtifact", getJsonArtifactId(focusEvent.getTargetArtifact()));
    object.add("obsProperties", getJsonObsPropertyList(focusEvent.getObsProperties()));
    return object;
  }

  public JsonObject getJsonFocussedArtifactDisposedEvent(){
    JsonObject object = getJsonArtifactObsEvent();
    FocussedArtifactDisposedEvent focussedEvent = (FocussedArtifactDisposedEvent) event;
    object.add("obsProperties", getJsonObsPropertyList(focussedEvent.getObsProperties()));
    return object;
  }

  public JsonObject getJsonAgentId(AgentId agentId){
    JsonObject object = new JsonObject();
    object.addProperty("name", agentId.getAgentName());
    object.addProperty("role", agentId.getAgentRole());
    object.addProperty("globalId", agentId.getGlobalId());
    object.addProperty("localId", agentId.getLocalId());
    object.add("workspaceId", getJsonWorkspaceId(agentId.getWorkspaceId()));
    return object;

  }

  public JsonObject getJsonICartagoContext(ICartagoContext context){
    JsonObject object = new JsonObject();
    try {
      object.add("agentId", getJsonAgentId(context.getAgentId()));
      object.add("workspaceId", getJsonWorkspaceId(context.getWorkspaceId()));
    } catch (CartagoException e){
      e.printStackTrace();
    }
    return object;
  }

  public JsonObject getJsonWorkspaceId(WorkspaceId workspaceId){
    JsonObject object = new JsonObject();
    object.addProperty("name", workspaceId.getName());
    object.addProperty("fullName", workspaceId.getFullName());
    object.addProperty("uuid", workspaceId.getUUID().toString());

    return object;
  }

  public JsonObject getJsonJoinWSPSucceededEvent(){
    JsonObject object = getJsonActionSucceededEvent();
    JoinWSPSucceededEvent joinEvent = (JoinWSPSucceededEvent) event;
    object.add("context", getJsonICartagoContext(joinEvent.getContext()));
    object.add("workspaceId", getJsonWorkspaceId(joinEvent.getWorkspaceId()));
    return object;
  }

  public JsonObject getJsonQuitWSPSucceededEvent(){
    JsonObject object = getJsonActionSucceededEvent();
    QuitWSPSucceededEvent quitEvent = (QuitWSPSucceededEvent) event;
    object.add("workspaceId", getJsonWorkspaceId(quitEvent.getWorkspaceId()));
    return object;

  }

  public JsonObject getJsonStopFocusSucceededEvent(){
    JsonObject object = getJsonActionSucceededEvent();
    StopFocusSucceededEvent stopEvent = (StopFocusSucceededEvent) event;
    object.add("targetArtifact", getJsonArtifactId(stopEvent.getTargetArtifact()));
    object.add("obsProperties", getJsonObsPropertyList(stopEvent.getObsProperties()));
    return object;

  }

  @Override
  public String toString(){
    JsonObject object = getJson();
    return object.getAsString();
  }
}
