package org.hyperagents.yggdrasil.cartago;

import cartago.AgentIdCredential;
import cartago.util.agent.CartagoContext;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

public class AgentContextRegistry {

  private static AgentContextRegistry registry;

  private Map<String, CartagoContext> contexts;

  public static AgentContextRegistry getInstance(){
    if (registry == null){
      registry = new AgentContextRegistry();
    }
    return registry;
  }

  private AgentContextRegistry(){
    contexts = new GrowingMap<>();
  }

  public boolean hasContext(String agentUri){
    boolean b = false;
    if (contexts.containsKey(agentUri)){
      b = true;
    }
    return b;
  }

  public CartagoContext getContext(String agentUri){
    return contexts.get(agentUri);
  }

  public void createNewContext(String agentUri){
    contexts.put(agentUri, new CartagoContext(new AgentIdCredential(agentUri)));
  }

  public void printRegistry(){
    System.out.println("print agent context registry");
    for (String key: contexts.keySet()){
      System.out.println("agent "+key+ " has context "+ contexts.get(key).getName());
    }
  }

  private static class GrowingMap<K, V> implements Map {

    public GrowingMap(){
      this.keys = new ArrayList<>();
      this.values = new ArrayList<>();
    }

    private List<K> keys;

    private List<V> values;

    @Override
    public int size() {
      return keys.size();
    }

    @Override
    public boolean isEmpty() {
      return keys.isEmpty();
    }

    @Override
    public boolean containsKey(Object key) {
      return keys.contains(key);
    }

    @Override
    public boolean containsValue(Object value) {
      return values.contains(value);
    }

    @Override
    public Object get(Object key) {
      if (keys.contains(key)){
        int n = keys.indexOf(key);
        Object v = values.get(n);
        return v;
      }
      return null;
    }

    @Nullable
    @Override
    public Object put(Object key, Object value) {
      if (keys.contains(keys)){

      } else {
        keys.add((K) key);
        values.add((V) value);
      }

      return null;
    }

    @Override
    public Object remove(Object key) { //removing is not allowed
      return null;
    }

    @Override
    public void putAll(@NotNull Map m) { //TODO: implement, not needed

    }

    @Override
    public void clear() { //TODO: implement, not needed

    }

    @NotNull
    @Override
    public Set keySet() {
      Set<K> keySet = new HashSet<>(keys);
      return keySet;
    }

    @NotNull
    @Override
    public Collection values() {
      Collection<V> valueSet = new HashSet<>(values);
      return valueSet;
    }

    @NotNull
    @Override
    public Set<Entry> entrySet() { //TODO: implement
      return null;
    }
  }

}
