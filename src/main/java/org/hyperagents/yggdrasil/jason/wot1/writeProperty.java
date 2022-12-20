package org.hyperagents.yggdrasil.jason.wot1;

import ch.unisg.ics.interactions.wot.td.ThingDescription;
import ch.unisg.ics.interactions.wot.td.affordances.Form;
import ch.unisg.ics.interactions.wot.td.affordances.PropertyAffordance;
import ch.unisg.ics.interactions.wot.td.clients.TDHttpRequest;
import ch.unisg.ics.interactions.wot.td.io.TDGraphReader;
import ch.unisg.ics.interactions.wot.td.vocabularies.TD;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import jason.asSemantics.TransitionSystem;
import jason.asSemantics.Unifier;
import jason.asSyntax.StringTerm;
import jason.asSyntax.Term;
import jason.asSyntax.VarTerm;
import org.hyperagents.yggdrasil.jason.YAgentArch;

import java.util.Hashtable;
import java.util.Map;
import java.util.Optional;

public class writeProperty extends WotAction{

  @Override
  public Object execute(TransitionSystem ts,
                        final Unifier un,
                        final Term[] arg) throws Exception {
    StringTerm t1 = (StringTerm) arg[0];
    String tdUrl = t1.getString();
    StringTerm t2 = (StringTerm) arg[1];
    String propertyName = t2.getString();
    StringTerm t3 = (StringTerm) arg[2];
    String payload = t3.getString();
    VarTerm var = (VarTerm) arg[3];
    Map<String, String> headers = new Hashtable<>();
    if (ts.getAgArch() instanceof YAgentArch) {
      YAgentArch agArch = (YAgentArch) ts.getAgArch();
      headers = agArch.getHeaders();
    }
    writeProperty(tdUrl, propertyName, headers, payload, un,ts);
    return null;

  }




  public void writeProperty(String tdUrl, String propertyName, Map<String, String> headers, String payload, Unifier un, TransitionSystem ts){
    tdUrl = tdUrl.replace("\"","");
    try {
      ThingDescription td = TDGraphReader.readFromURL(ThingDescription.TDFormat.RDF_TURTLE, tdUrl);
      Optional<PropertyAffordance> opProperty = td.getPropertyByName(propertyName);
      if (opProperty.isPresent()){
        PropertyAffordance property = opProperty.get();
        Optional<Form> opForm = property.getFirstFormForOperationType(TD.writeProperty);
        if (opForm.isPresent()){
          Form form = opForm.get();
          TDHttpRequest request = new TDHttpRequest(form, TD.readProperty);
          for (String key: headers.keySet()){
            request.addHeader(key, headers.get(key));
          }
          JsonElement jsonElement = getAsJsonElement(payload);
          if (jsonElement.isJsonObject()){
            JsonObject jsonObject = jsonElement.getAsJsonObject();
            request.setObjectPayload(getObjectSchemaFromJsonObject(jsonObject), getJsonObjectAsMap(jsonObject));

          } else if (jsonElement.isJsonArray()){
            JsonArray jsonArray = jsonElement.getAsJsonArray();
            request.setArrayPayload(getArraySchemaFromJsonArray(jsonArray), getJsonArrayAsList(jsonArray));

          } else if (jsonElement.isJsonPrimitive()){
            JsonPrimitive jsonPrimitive =  jsonElement.getAsJsonPrimitive();
            if (jsonPrimitive.isNumber()){
              request.setPrimitivePayload(getDataSchemaFromJsonPrimitive(jsonPrimitive), jsonPrimitive.getAsDouble());
            } else if (jsonPrimitive.isString()){
              request.setPrimitivePayload(getDataSchemaFromJsonPrimitive(jsonPrimitive), jsonPrimitive.getAsString());
            } else if (jsonPrimitive.isBoolean()){
              request.setPrimitivePayload(getDataSchemaFromJsonPrimitive(jsonPrimitive), jsonPrimitive.getAsBoolean());
            }

          }

          request.execute();
        }
      }

    } catch(Exception e){
      e.printStackTrace();
    }

  }
}
