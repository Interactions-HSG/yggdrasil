package org.hyperagents.yggdrasil.websub;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

/**
 * A singleton used to manage WebSub subscribers. An equivalent implementation can be obtained with
 * local maps in Vert.x. Can be refactored using async shared maps to run over a cluster. 
 * 
 * @author Andrei Ciortea
 *
 */
public class NotificationSubscriberRegistry {

  private static NotificationSubscriberRegistry registry;
  private Map<String,Set<String>> subscriptions;
  
  private NotificationSubscriberRegistry() {
    subscriptions = new Hashtable<String,Set<String>>();
  }
  
  public static synchronized NotificationSubscriberRegistry getInstance() {
    if (registry == null) {
        registry = new NotificationSubscriberRegistry();
    }
    
    return registry;
  }
  
  public Set<String> getCallbackIRIs(String entityIRI) {
    return subscriptions.getOrDefault(entityIRI, new HashSet<String>());
  }
  
  public void addCallbackIRI(String entityIRI, String callbackIRI) {
    Set<String> callbacks = registry.getCallbackIRIs(entityIRI);
    callbacks.add(callbackIRI);
    
    subscriptions.put(entityIRI, callbacks);
  }
  
  public void removeCallbackIRI(String entityIRI, String callbackIRI) {
    Set<String> callbacks = registry.getCallbackIRIs(entityIRI);
    callbacks.remove(callbackIRI);
    
    if (callbacks.isEmpty()) {
      subscriptions.remove(entityIRI);
    } else {
      subscriptions.put(entityIRI, callbacks);
    }
  }
}
