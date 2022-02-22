package org.hyperagents.yggdrasil.signifiers.maze;

import com.google.gson.JsonObject;
import io.vertx.core.Vertx;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpRequest;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.apache.hc.core5.http.message.BasicClassicHttpRequest;
import org.hyperagents.yggdrasil.cartago.CartagoEntityHandler;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class MazeCreator {

  public static void launchMaze(){
    createEnvironment("env1");
    createWorkspace("env1", "wksp1");
    createArtifact("env1", "wksp1", "maze", "http://example.org/ArticleMaze");
  }

  public static void createEnvironment(String envName){
    String uri = "http://localhost:8080/environments/";
    String method = "POST";
    Map<String, String> headers = new Hashtable<>();
    headers.put("X-Agent-WebID", "http://example.org/agent");
    headers.put("Slug", envName);
    sendHttpRequest(uri, method, headers, null);
  }



  public static void createWorkspace(String envName, String workspaceName){
    String uri = "http://localhost:8080/environments/"+envName+"/workspaces/";
    String method = "POST";
    Map<String, String> headers = new Hashtable<>();
    headers.put("X-Agent-WebID", "http://example.org/agent");
    headers.put("Slug", workspaceName);
    sendHttpRequest(uri, method, headers, null);
  }

  public static void createArtifact(String envName, String workspaceName, String artifactName, String artifactClass){
    String uri = "http://localhost:8080/environments/"+envName+"/workspaces/"+workspaceName+"/artifacts/";
    String method = "POST";
    Map<String, String> headers = new Hashtable<>();
    headers.put("X-Agent-WebID", "http://example.org/agent");
    headers.put("Content-Type", "application/json");
    JsonObject object = new JsonObject();
    object.addProperty("artifactName", artifactName);
    object.addProperty("artifactClass", artifactClass);
    String body = object.toString();
    sendHttpRequest(uri, method, headers, body);
  }






  public static String sendHttpRequest(String uri, String method, Map<String, String> headers, String body){
    CloseableHttpClient client = HttpClients.createDefault();
    AtomicReference<String> returnValue = new AtomicReference();
    ClassicHttpRequest request = new BasicClassicHttpRequest(method, uri);
    for (String key: headers.keySet()){
      String value = headers.get(key);
      request.addHeader(key, value);
    }
    if (body != null){
      request.setEntity(new StringEntity(body));
    }
    try {
      client.execute(request, response -> {
        HttpEntity entity = response.getEntity();
        //String r = EntityUtils.toString(entity);
        BufferedReader reader = new BufferedReader(new InputStreamReader(entity.getContent()));
        String line = null;
        String s = "";
        while ((line = reader.readLine())!=null){
          s = s + line;
        }
        returnValue.set(s);
        return null;
      });
    } catch(Exception e){
      e.printStackTrace();
    }
    return returnValue.get();

  }
}
