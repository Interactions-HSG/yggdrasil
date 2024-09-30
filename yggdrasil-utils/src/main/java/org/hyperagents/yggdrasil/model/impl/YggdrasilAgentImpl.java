package org.hyperagents.yggdrasil.model.impl;

import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hyperagents.yggdrasil.model.interfaces.AgentBody;
import org.hyperagents.yggdrasil.model.interfaces.YggdrasilAgent;

/**
 * used for config files.
 */
public class YggdrasilAgentImpl  implements YggdrasilAgent {

  private final String name;
  private final String agentUri;
  private final String agentCallbackUri;
  private final List<AgentBody> bodies;

  /**
   * Default constructor.
   */
  public YggdrasilAgentImpl(final String name, final String agentUri, final String agentCallbackUri,
                            final List<AgentBody> bodies) {
    this.name = name;
    this.agentUri = agentUri;
    this.agentCallbackUri = agentCallbackUri;
    this.bodies = List.copyOf(bodies);
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof YggdrasilAgentImpl that)) {
      return false;
    }
    return name.equals(that.name)
      && Objects.equals(agentUri, that.agentUri)
      && Objects.equals(agentCallbackUri, that.agentCallbackUri)
      && Objects.equals(bodies, that.bodies);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, agentUri, agentCallbackUri, bodies);
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public String getAgentUri() {
    return agentUri;
  }

  @Override
  public Optional<String> getAgentCallbackUri() {
    return Optional.ofNullable(agentCallbackUri);
  }

  @Override
  public List<AgentBody> getBodyConfig() {
    return List.copyOf(bodies);
  }
}
