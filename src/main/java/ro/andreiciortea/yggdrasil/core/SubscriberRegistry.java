package ro.andreiciortea.yggdrasil.core;

import java.util.HashSet;
import java.util.Hashtable;
import java.util.Map;
import java.util.Set;

public class SubscriberRegistry {

  private static SubscriberRegistry registry;
  private Map<String,Set<String>> subscriptions;

  
  private SubscriberRegistry() {
    subscriptions = new Hashtable<String,Set<String>>();
  }
  
  public static synchronized SubscriberRegistry getInstance() {
    if (registry == null) {
        registry = new SubscriberRegistry();
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
