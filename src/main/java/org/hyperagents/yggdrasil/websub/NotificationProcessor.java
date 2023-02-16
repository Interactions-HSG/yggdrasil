package org.hyperagents.yggdrasil.websub;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

public class NotificationProcessor {

  public static boolean containsFromIndex(String notification, int index, String pattern){
    boolean b = false;
    String newNotification = notification.substring(index);
    b = newNotification.contains(pattern);
    return b;
  }

  public static int getIndexFrom(String notification, int index, String pattern){
    int newIndex;
    String newNotification = notification.substring(index);
    newIndex = newNotification.indexOf(pattern);
    return index + newIndex;
  }

  public static String getUriFromIndex(String notification, int index){
    int endIndex = index;
    boolean b = true;
    int i = index;
    while (b && i<notification.length()){
      if (notification.charAt(i) == ',' || notification.charAt(i)==')' ||notification.charAt(i)== ' '){
        b = false;
        endIndex = i;
      } else {
        i++;
      }
    }
    return notification.substring(index, endIndex);
  }

  public static Set<String> getUrisFromNotification(String notification){
    Set<String> uris = new HashSet<>();
    int index = 0;
    boolean b = containsFromIndex(notification, index, "http://");
    while (b){
      index = getIndexFrom(notification, index, "http://");
      String uri = getUriFromIndex(notification, index);
      uris.add(uri);
      index = index + uri.length();
      b = containsFromIndex(notification, index, "http://");

    }
    return uris;
  }

  public static Set<Integer> allIndexes(String notification, String uri){
    int n = uri.length();
    Set<Integer> allIndexes = new HashSet<>();
    for (int i=0; i<notification.length();i++ ){
      if (notification.substring(i, i+n).equals(uri)){
        allIndexes.add(i);
      }
    }
    return allIndexes;
  }

  public static String replace(String notification, String uri, int index){
    String newNotification = notification.substring(0, index);
    newNotification = newNotification + "\"" + uri + "\"" + notification.substring(index + uri.length());
    return newNotification;
  }

  public static String replace(String notification, String uri){
    String newNotifaction = notification;
    String returnNotification = notification;
    boolean b = true;
    while (b){
      int index = newNotifaction.lastIndexOf(uri);
      if (index<0){
        b = false;
      } else if (index > 0 && newNotifaction.charAt(index-1)== '\"') {
        newNotifaction = notification.substring(0, index-1);
      } else {
        newNotifaction = notification.substring(0, index-1);
        returnNotification = replace(returnNotification, uri, index);

      }
    }
    return returnNotification;
  }


  public static String transformNotification(String notification){
    String newNotification = notification;
    Set<String> uris = getUrisFromNotification(notification);
    Iterator<String> uriIterator= uris.stream().sorted((o1, o2) -> {
      int n1 = o1.length();
      int n2 = o2.length();
      int r = 0;
      if (n1>n2){
        r = -1;
      }
      if (n1<n2){
        r = 1;
      }
      return r;
    }).distinct().iterator();
    for (Iterator<String> it = uriIterator; it.hasNext(); ) {
      String uri = it.next();
      newNotification = replace(newNotification, uri);
    }
    return newNotification;
  }

}
