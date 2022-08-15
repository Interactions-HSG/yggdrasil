package org.hyperagents.yggdrasil;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.DeliveryOptions;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.hyperagents.yggdrasil.http.HttpEntityHandler;

import java.util.*;

public class PubSubVerticle extends AbstractVerticle {

  public static final String BUS_ADDRESS = "org.hyperagents.yggdrasil.eventbus.pubsub";

  public static final String REQUEST_METHOD = "org.hyperagents.yggdrasil.eventbus.pubsub.requestMethod";

  public static final String PUBLISH = "org.hyperagents.yggdrasil.eventbus.pubsub.publish";

  public static final String SUBSCRIBE = "org.hyperagents.yggdrasil.eventbus.pubsub.subscribe";

  public static final String UNSUBSCRIBE = "org.hyperagents.yggdrasil.eventbus.pubsub.unsubscribe";

  public static final String TOPIC_NAME = "org.hyperagents.yggdrasil.eventbus.pubsub.topic";

  private Map<String, Set<String>> topicMap;

  private Map<String, Set<String>> subscriberMap;

  @Override
  public void start(){
    topicMap = new Hashtable<>();
    subscriberMap = new Hashtable<>();
    EventBus eventBus = vertx.eventBus();
    eventBus.consumer(BUS_ADDRESS, this::handlePubSubRequest);
  }

  public void handlePubSubRequest(Message<String> message){
    String requestMethod = message.headers().get(PubSubVerticle.REQUEST_METHOD);
    String topicName = message.headers().get(PubSubVerticle.TOPIC_NAME);
    String sender = message.replyAddress();
    switch (requestMethod){
      case PUBLISH:
        publish(sender, topicName, message.body());
        System.out.println("publish");
      case SUBSCRIBE:
        subscribe(sender, topicName);
        System.out.println("subscribe");
      case UNSUBSCRIBE:
        unsubscribe(sender, topicName);
    }

  }

  public void publish(String sender, String topicName, String message){
    if (topicMap.containsKey(topicName)){
      Set<String> subscribers = topicMap.get(topicName);
      for (String subscriber: subscribers){
        vertx.eventBus().send(subscriber, message);
      }
    } else {
      topicMap.put(topicName, new HashSet<>());
    }

  }

  public void subscribe(String subscriber, String topicName){
    if (topicMap.containsKey(topicName)){
      Set<String> subscribers = topicMap.get(topicName);
      subscribers.add(subscriber);
      topicMap.put(topicName, subscribers);
    } else {
      Set<String> l = new HashSet<>();
      l.add(subscriber);
      topicMap.put(topicName, l);
    }

  }

  public void unsubscribe(String sender, String topicName){
    if (subscriberMap.containsKey(sender)){
      Set<String> topics = subscriberMap.get(sender);
      topics.add(topicName);
      subscriberMap.put(sender, topics);
    } else {
      Set<String> set = new HashSet<>();
      set.add(topicName);
      subscriberMap.put(sender, set);
    }

  }

}
