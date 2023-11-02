package org.hyperagents.yggdrasil.websub;

import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;

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

  private final SetMultimap<String, String> subscriptions;

  private NotificationSubscriberRegistry() {
    this.subscriptions = Multimaps.synchronizedSetMultimap(Multimaps.newSetMultimap(new HashMap<>(), HashSet::new));
  }

  public static synchronized NotificationSubscriberRegistry getInstance() {
    if (REGISTRY == null) {
        REGISTRY = new NotificationSubscriberRegistry();
    }
    return REGISTRY;
  }

  public Set<String> getCallbackIRIs(final String entityIRI) {
    return new HashSet<>(this.subscriptions.get(entityIRI));
  }

  public void addCallbackIRI(final String entityIRI, final String callbackIRI) {
    this.subscriptions.put(entityIRI, callbackIRI);
  }

  public void removeCallbackIRI(final String entityIRI, final String callbackIRI) {
    this.subscriptions.remove(entityIRI, callbackIRI);
  }
}
