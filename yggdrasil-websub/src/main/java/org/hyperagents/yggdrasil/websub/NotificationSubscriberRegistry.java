package org.hyperagents.yggdrasil.websub;

import com.google.common.collect.Multimaps;
import com.google.common.collect.SetMultimap;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

/**
 * A singleton used to manage WebSub subscribers. An equivalent implementation can be obtained with
 * local maps in Vert.x. Can be refactored using async shared maps to run over a cluster.
 *
 * @author Andrei Ciortea
 *
 */
public final class NotificationSubscriberRegistry {
  private static NotificationSubscriberRegistry REGISTRY;

  private final SetMultimap<String, String> subscriptions;

  private NotificationSubscriberRegistry() {
    this.subscriptions =
      Multimaps.synchronizedSetMultimap(Multimaps.newSetMultimap(new HashMap<>(), HashSet::new));
  }

  @SuppressFBWarnings("MS_EXPOSE_REP")
  public static synchronized NotificationSubscriberRegistry getInstance() {
    if (REGISTRY == null) {
      REGISTRY = new NotificationSubscriberRegistry();
    }
    return REGISTRY;
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
