package org.hyperagents.yggdrasil.websub;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.SetMultimap;
import java.util.HashSet;
import java.util.Set;

/**
 * A class used to manage WebSub subscribers.
 *
 * @author Andrei Ciortea
 *
 */
public final class NotificationSubscriberRegistry {
  private final SetMultimap<String, String> subscriptions;

  NotificationSubscriberRegistry() {
    this.subscriptions = HashMultimap.create();
  }

  public Set<String> getCallbackIris(final String entityIri) {
    return new HashSet<>(this.subscriptions.get(entityIri));
  }

  public void addCallbackIri(final String entityIri, final String callbackIri) {
    this.subscriptions.put(entityIri, callbackIri);
  }

  public void removeCallbackIri(final String entityIri, final String callbackIri) {
    this.subscriptions.remove(entityIri, callbackIri);
  }
}
