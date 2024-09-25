package org.hyperagents.yggdrasil.model.impl;

import java.io.File;
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
  private final String clazz;
  private final List<?> initializationParameters;
  private final Path representation;
  private final Path metadata;
  private final List<String> focusedBy;

  /**
   *  Default constructor.
   */
  public ArtifactImpl(String name, String clazz, List<?> initializationParameters,
                      String representation, String metadata,
                      List<String> focusedBy) {
    this.name = name;
    this.clazz = clazz;
    this.initializationParameters = initializationParameters;
    this.focusedBy = focusedBy;

    if (representation != null && new File(representation).isFile()) {
      this.representation = Path.of(representation);
    } else {
      this.representation = null;
    }

    if (metadata != null && new File(metadata).isFile()) {
      this.metadata = Path.of(metadata);
    } else {
      this.metadata = null;
    }


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
    return Optional.ofNullable(clazz);
  }

  @Override
  public List<?> getInitializationParameters() {
    return initializationParameters;
  }

  @Override
  public Optional<Path> getRepresentation() {
    return Optional.ofNullable(representation);
  }

  public Optional<Path> getMetaData() {
    return Optional.ofNullable(metadata);
  }

  @Override
  public List<String> getFocusedBy() {
    return focusedBy;
  }
}
