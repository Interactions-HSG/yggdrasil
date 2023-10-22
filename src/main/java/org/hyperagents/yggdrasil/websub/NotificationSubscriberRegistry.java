package org.hyperagents.yggdrasil.websub;

import java.util.*;

/**
 * A singleton used to manage WebSub subscribers. An equivalent implementation can be obtained with
 * local maps in Vert.x. Can be refactored using async shared maps to run over a cluster.
 *
 * @author Andrei Ciortea
 *
 */
public class NotificationSubscriberRegistry {
  private static NotificationSubscriberRegistry REGISTRY;

  private final Map<String, Set<String>> subscriptions;

  private NotificationSubscriberRegistry() {
    this.subscriptions = Collections.synchronizedMap(new HashMap<>());
  }

  public static synchronized NotificationSubscriberRegistry getInstance() {
    if (REGISTRY == null) {
        REGISTRY = new NotificationSubscriberRegistry();
    }
    return REGISTRY;
  }

  public Set<String> getCallbackIRIs(final String entityIRI) {
    return this.subscriptions.getOrDefault(entityIRI, new HashSet<>());
  }

  public void addCallbackIRI(final String entityIRI, final String callbackIRI) {
    final var callbacks = this.getCallbackIRIs(entityIRI);
    callbacks.add(callbackIRI);
    this.subscriptions.put(entityIRI, callbacks);
  }

  public void removeCallbackIRI(final String entityIRI, final String callbackIRI) {
    final var callbacks = this.getCallbackIRIs(entityIRI);
    callbacks.remove(callbackIRI);
    if (callbacks.isEmpty()) {
      this.subscriptions.remove(entityIRI);
    } else {
      this.subscriptions.put(entityIRI, callbacks);
    }
  }
}
