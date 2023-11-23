package org.hyperagents.yggdrasil.store.impl;

import io.vertx.core.json.JsonObject;
import java.io.File;
import java.util.Optional;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.rdf4j.RDF4J;
import org.apache.commons.rdf.rdf4j.RDF4JGraph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.hyperagents.yggdrasil.store.RdfStore;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;

public class Rdf4jStore implements RdfStore {
  private static final Logger LOGGER = LogManager.getLogger(RdfStore.class);

  private final Dataset dataset;

  Rdf4jStore(final JsonObject config) {
    this.dataset = new RDF4J().asDataset(
      Optional.ofNullable(config)
              .flatMap(c -> JsonObjectUtils.getBoolean(c, "in-memory", LOGGER))
              .orElse(false)
      ? new SailRepository(new NativeStore(new File(
          JsonObjectUtils.getString(config, "store-path", LOGGER).orElse("data/")
        )))
      : new SailRepository(new MemoryStore()),
      RDF4J.Option.handleInitAndShutdown
    );
  }

  @Override
  public boolean containsEntityGraph(final IRI entityIri) {
    return this.dataset.contains(Optional.of(entityIri), null, null, null);
  }

  @Override
  public Optional<Graph> getEntityGraph(final IRI entityIri) {
    return this.dataset.getGraph(entityIri);
  }

  @Override
  public void createEntityGraph(final IRI entityIri, final Graph entityGraph) {
    if (entityGraph instanceof RDF4JGraph) {
      this.addEntityGraph(entityIri, entityGraph);
    } else {
      throw new IllegalArgumentException("Unsupported RDF graph implementation");
    }
  }

  @Override
  public void updateEntityGraph(final IRI entityIri, final Graph entityGraph) {
    if (entityGraph instanceof RDF4JGraph) {
      this.deleteEntityGraph(entityIri);
      this.addEntityGraph(entityIri, entityGraph);
    } else {
      throw new IllegalArgumentException("Unsupported RDF graph implementation");
    }
  }

  @Override
  public void deleteEntityGraph(final IRI entityIri) {
    this.dataset.remove(Optional.of(entityIri), null, null, null);
  }

  @Override
  public void addEntityGraph(final IRI entityIri, final Graph entityGraph) {
    try (var stream = ((RDF4JGraph) entityGraph).stream()) {
      stream.forEach(triple -> this.dataset.add(
          entityIri,
          triple.getSubject(),
          triple.getPredicate(),
          triple.getObject()
      ));
    }
  }
}
