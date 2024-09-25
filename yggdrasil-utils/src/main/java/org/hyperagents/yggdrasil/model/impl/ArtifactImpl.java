package org.hyperagents.yggdrasil.model.impl;

import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.hyperagents.yggdrasil.model.interfaces.Artifact;

/**
 * Implements Artifact interface, used to build environment from config file.
 */
public class ArtifactImpl implements Artifact {

  private final String name;
  private final Optional<String> clazz;
  private final List<?> initializationParameters;
  private final Optional<Path> representation;
  private final Optional<Path> metadata;
  private final List<String> focusedBy;

  /**
   *  Default constructor.
   */
  public ArtifactImpl(String name, Optional<String> clazz, List<?> initializationParameters,
                      Optional<Path> representation, Optional<Path> metadata,
                      List<String> focusedBy) {
    this.name = name;
    this.clazz = clazz;
    this.initializationParameters = initializationParameters;
    this.representation = representation;
    this.metadata = metadata;
    this.focusedBy = focusedBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ArtifactImpl artifact)) {
      return false;
    }
    return Objects.equals(name, artifact.name)
      && Objects.equals(clazz, artifact.clazz)
      && Objects.equals(initializationParameters, artifact.initializationParameters)
      && Objects.equals(representation, artifact.representation)
      && Objects.equals(metadata, artifact.metadata)
      && Objects.equals(focusedBy, artifact.focusedBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, clazz, initializationParameters, representation, metadata, focusedBy);
  }

  @Override
  public String getName() {
    return name;
  }

  public Optional<String> getClazz() {
    return clazz;
  }

  @Override
  public List<?> getInitializationParameters() {
    return initializationParameters;
  }

  @Override
  public Optional<Path> getRepresentation() {
    return representation;
  }

  public Optional<Path> getMetaData() {
    return metadata;
  }

  @Override
  public List<String> getFocusedBy() {
    return focusedBy;
  }
}
