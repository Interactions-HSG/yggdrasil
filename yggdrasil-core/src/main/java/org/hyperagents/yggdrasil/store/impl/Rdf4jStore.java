package org.hyperagents.yggdrasil.store.impl;

import io.vertx.core.json.JsonObject;
import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Set;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.rdf4j.RDF4J;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.hyperagents.yggdrasil.store.RdfStore;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;

public class Rdf4jStore implements RdfStore {
  private static final Logger LOGGER = LogManager.getLogger(RdfStore.class);

  private final Repository repository;
  private final Dataset dataset;
  private final RDF4J rdf4j;

  Rdf4jStore(final JsonObject config) {
    this.rdf4j = new RDF4J();
    this.repository =
      Optional.ofNullable(config)
              .flatMap(c -> JsonObjectUtils.getBoolean(c, "in-memory", LOGGER))
              .orElse(false)
      ? new SailRepository(new NativeStore(new File(
          JsonObjectUtils.getString(config, "store-path", LOGGER).orElse("data/")
        )))
      : new SailRepository(new MemoryStore());
    this.dataset = this.rdf4j.asDataset(
      this.repository,
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

  @Override
  public Set<Map<String, Optional<String>>> queryGraph(final String query)
      throws IllegalArgumentException, IOException {
    try (var connection = this.repository.getConnection()) {
      try (var result = connection.prepareTupleQuery(query).evaluate()) {
        return QueryResults.asSet(result)
                           .stream()
                           .map(s -> result.getBindingNames()
                                           .stream()
                                           .map(n -> Map.entry(
                                             n,
                                             Optional.ofNullable(s.getValue(n))
                                                     .map(Value::stringValue)
                                           ))
                                           .collect(Collectors.toMap(
                                             Map.Entry::getKey,
                                             Map.Entry::getValue
                                           )))
                           .collect(Collectors.toSet());
      } catch (final IllegalArgumentException | MalformedQueryException e) {
        throw new IllegalArgumentException(e);
      } catch (final QueryEvaluationException e) {
        throw new IOException(e);
      }
    } catch (final RepositoryException e) {
      throw new IOException(e);
    }
  }
}
