package org.hyperagents.yggdrasil.model.impl;

import java.util.Objects;
import org.hyperagents.yggdrasil.model.interfaces.KnownArtifact;

/**
 * Implemented class of the knownArtifact interface.
 */
public class KnownArtifactImpl implements KnownArtifact {

  private final String clazz;
  private final String template;

  public KnownArtifactImpl(String clazz, String template) {
    this.clazz = clazz;
    this.template = template;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof KnownArtifactImpl that)) {
      return false;
    }
    return Objects.equals(clazz, that.clazz)
      && Objects.equals(template, that.template);
  }

  @Override
  public int hashCode() {
    return Objects.hash(clazz, template);
  }

  @Override
  public String getClazz() {
    return clazz;
  }

  @Override
  public String getTemplate() {
    return template;
  }
}
