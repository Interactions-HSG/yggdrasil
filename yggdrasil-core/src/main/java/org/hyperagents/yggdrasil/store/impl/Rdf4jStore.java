package org.hyperagents.yggdrasil.store.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.query.BooleanQuery;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.MalformedQueryException;
import org.eclipse.rdf4j.query.QueryEvaluationException;
import org.eclipse.rdf4j.query.QueryResults;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLBooleanJSONWriter;
import org.eclipse.rdf4j.query.resultio.sparqljson.SPARQLResultsJSONWriter;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLBooleanXMLWriter;
import org.eclipse.rdf4j.query.resultio.sparqlxml.SPARQLResultsXMLWriter;
import org.eclipse.rdf4j.query.resultio.text.BooleanTextWriter;
import org.eclipse.rdf4j.query.resultio.text.csv.SPARQLResultsCSVWriter;
import org.eclipse.rdf4j.query.resultio.text.tsv.SPARQLResultsTSVWriter;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.sail.Sail;
import org.hyperagents.yggdrasil.store.RdfStore;
import org.hyperagents.yggdrasil.utils.RdfModelUtils;

public class Rdf4jStore implements RdfStore {
  private final Repository repository;
  private final RepositoryConnection connection;

  Rdf4jStore(final Sail store) {
    this.repository = new SailRepository(store);
    this.repository.init();
    this.connection = this.repository.getConnection();
  }

  @Override
  public boolean containsEntityModel(final IRI entityIri) throws IOException {
    try {
      return this.connection.hasStatement(
        null,
        null,
        null,
        false,
        entityIri
      );
    } catch (final RepositoryException e) {
      throw new IOException(e);
    }
  }

  @Override
  public Optional<Model> getEntityModel(final IRI entityIri) throws IOException {
    try {
      final Model model = QueryResults.asModel(this.connection.getStatements(null, null, null, entityIri));
      this.connection.getNamespaces().forEach(model::setNamespace);
      return Optional.of(model).filter(r -> !r.isEmpty());
    } catch (final RepositoryException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void addEntityModel(final IRI entityIri, final Model entityModel) throws IOException {
    try {
      this.connection.add(entityModel, entityIri);
      entityModel.getNamespaces().forEach(namespace -> this.connection.setNamespace(namespace.getPrefix(), namespace.getName()));
    } catch (final RepositoryException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void replaceEntityModel(final IRI entityIri, final Model entityModel) throws IOException {
    this.removeEntityModel(entityIri);
    this.addEntityModel(entityIri, entityModel);
  }

  @Override
  public void removeEntityModel(final IRI entityIri) throws IOException {
    try {
      this.connection.clear(entityIri);
    } catch (final RepositoryException e) {
      throw new IOException(e);
    }
  }

  @Override
  public void close() throws IOException {
    try {
      this.connection.close();
      this.repository.shutDown();
    } catch (final RepositoryException e) {
      throw new IOException(e);
    }
  }

  @Override
  public String queryGraph(
      final String query,
      final List<String> defaultGraphUris,
      final List<String> namedGraphUris,
      final String responseContentType
  ) throws IllegalArgumentException, IOException {
    try (var out = new ByteArrayOutputStream()) {
      final var preparedQuery = this.connection.prepareQuery(query);
      final var originalQueryDataset =
          Optional.ofNullable(preparedQuery.getDataset()).orElse(new SimpleDataset());
      final var queryDataset = new SimpleDataset();
      originalQueryDataset
          .getDefaultRemoveGraphs()
          .forEach(queryDataset::addDefaultRemoveGraph);
      queryDataset.setDefaultInsertGraph(originalQueryDataset.getDefaultInsertGraph());
      if (!defaultGraphUris.isEmpty()) {
        defaultGraphUris.forEach(s -> queryDataset.addDefaultGraph(RdfModelUtils.createIri(s)));
      } else {
        originalQueryDataset.getDefaultGraphs().forEach(queryDataset::addDefaultGraph);
      }
      if (!namedGraphUris.isEmpty()) {
        namedGraphUris.forEach(s -> queryDataset.addNamedGraph(RdfModelUtils.createIri(s)));
      } else {
        originalQueryDataset.getNamedGraphs().forEach(queryDataset::addNamedGraph);
      }
      preparedQuery.setDataset(queryDataset);
      if (preparedQuery instanceof TupleQuery preparedTupleQuery) {
        preparedTupleQuery.evaluate(
            responseContentType.equals("application/sparql-results+xml")
            ? new SPARQLResultsXMLWriter(out)
            : (responseContentType.equals("application/sparql-results+json")
               ? new SPARQLResultsJSONWriter(out)
               : (responseContentType.equals("text/tab-separated-values")
                  ? new SPARQLResultsTSVWriter(out)
                  : new SPARQLResultsCSVWriter(out)
                 )
              )
        );
      } else if (preparedQuery instanceof BooleanQuery preparedBooleanQuery) {
        (
          responseContentType.equals("application/sparql-results+xml")
          ? new SPARQLBooleanXMLWriter(out)
          : (responseContentType.equals("application/sparql-results+json")
             ? new SPARQLBooleanJSONWriter(out)
             : new BooleanTextWriter(out)
            )
        )
        .handleBoolean(preparedBooleanQuery.evaluate());
      } else if (preparedQuery instanceof GraphQuery preparedGraphQuery) {
        out.writeBytes(
            RdfModelUtils.modelToString(QueryResults.asModel(preparedGraphQuery.evaluate()),
                                        RDFFormat.TURTLE)
                         .getBytes(StandardCharsets.UTF_8)
        );
      }
      return out.toString(StandardCharsets.UTF_8);
    } catch (final MalformedQueryException e) {
      throw new IllegalArgumentException(e);
    } catch (final RepositoryException
                   | QueryEvaluationException
                   | TupleQueryResultHandlerException e) {
      throw new IOException(e);
    }
  }
}
