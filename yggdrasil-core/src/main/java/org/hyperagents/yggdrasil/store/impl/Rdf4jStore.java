package org.hyperagents.yggdrasil.store.impl;

import io.vertx.core.json.JsonObject;
import java.io.File;
import java.util.Optional;
import java.util.stream.Collectors;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.rdf4j.RDF4J;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.hyperagents.yggdrasil.store.RdfStore;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;

public class Rdf4jStore implements RdfStore {
  private static final Logger LOGGER = LogManager.getLogger(RdfStore.class);

  private final Dataset dataset;
  private final RDF4J rdf4j;

  Rdf4jStore(final JsonObject config) {
    this.rdf4j = new RDF4J();
    this.dataset = this.rdf4j.asDataset(
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
  public boolean containsEntityModel(final IRI entityIri) {
    return this.dataset.contains(
      Optional.of(this.rdf4j.asRDFTerm(entityIri)),
      null,
      null,
      null
    );
  }

  @Override
  public Optional<Model> getEntityModel(final IRI entityIri) {
    return !this.containsEntityModel(entityIri)
           ? Optional.empty()
           : this.dataset
                 .getGraph(this.rdf4j.asRDFTerm(entityIri))
                 .map(g -> new LinkedHashModel(g.stream()
                                                .map(this.rdf4j::asStatement)
                                                .collect(Collectors.toSet())));
  }

  @Override
  public void addEntityModel(final IRI entityIri, final Model entityModel) {
    entityModel.forEach(t -> {
      final var triple = this.rdf4j.asTriple(t);
      this.dataset.add(
          this.rdf4j.asRDFTerm(entityIri),
          triple.getSubject(),
          triple.getPredicate(),
          triple.getObject()
      );
    });
  }

  @Override
  public void replaceEntityModel(final IRI entityIri, final Model entityModel) {
    this.removeEntityModel(entityIri);
    this.addEntityModel(entityIri, entityModel);
  }

  @Override
  public void removeEntityModel(final IRI entityIri) {
    this.dataset.remove(Optional.of(this.rdf4j.asRDFTerm(entityIri)), null, null, null);
  }

  @Override
  public void close() {
    try {
      this.dataset.close();
    } catch (final Exception e) {
      LOGGER.error(e);
    }
  }
}
