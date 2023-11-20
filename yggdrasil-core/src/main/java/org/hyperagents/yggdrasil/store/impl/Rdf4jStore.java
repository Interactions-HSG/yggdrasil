package org.hyperagents.yggdrasil.store.impl;

import io.vertx.core.json.JsonObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import org.apache.commons.rdf.api.Dataset;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.rdf4j.RDF4J;
import org.apache.commons.rdf.rdf4j.RDF4JGraph;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;
import org.hyperagents.yggdrasil.store.RdfStore;
import org.hyperagents.yggdrasil.utils.JsonObjectUtils;

public class Rdf4jStore implements RdfStore {
  private static final Logger LOGGER = LogManager.getLogger(RdfStore.class);

  private final RDF4J rdfImpl;
  private final Dataset dataset;

  Rdf4jStore(final JsonObject config) {
    this.rdfImpl = new RDF4J();
    this.dataset = this.rdfImpl.asDataset(
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
  public IRI createIri(final String iriString) throws IllegalArgumentException {
    return this.rdfImpl.createIRI(iriString);
  }

  @Override
  public String graphToString(final Graph graph, final RDFSyntax syntax)
      throws IllegalArgumentException, IOException {
    if (graph instanceof RDF4JGraph rdf4jGraph) {
      try (var out = new ByteArrayOutputStream()) {
        final RDFWriter writer;

        if (syntax.equals(RDFSyntax.TURTLE)) {
          writer = Rio.createWriter(RDFFormat.TURTLE, out);
        } else if (syntax.equals(RDFSyntax.JSONLD)) {
          writer = Rio.createWriter(RDFFormat.JSONLD, out);
          writer.getWriterConfig()
                .set(JSONLDSettings.JSONLD_MODE, JSONLDMode.FLATTEN)
                .set(JSONLDSettings.USE_NATIVE_TYPES, true)
                .set(JSONLDSettings.OPTIMIZE, true);
        } else {
          throw new IllegalArgumentException("Unsupported RDF serialization format.");
        }

        writer.getWriterConfig()
              .set(BasicWriterSettings.PRETTY_PRINT, true)
              .set(BasicWriterSettings.RDF_LANGSTRING_TO_LANG_LITERAL, true)
              .set(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL, true)
              .set(BasicWriterSettings.INLINE_BLANK_NODES, true);
        try {
          writer.startRDF();

          writer.handleNamespace("hmas", "https://purl.org/hmas/core/");
          writer.handleNamespace("td", "https://www.w3.org/2019/wot/td#");
          writer.handleNamespace("htv", "http://www.w3.org/2011/http#");
          writer.handleNamespace("hctl", "https://www.w3.org/2019/wot/hypermedia#");
          writer.handleNamespace("wotsec", "https://www.w3.org/2019/wot/security#");
          writer.handleNamespace("dct", "http://purl.org/dc/terms/");
          writer.handleNamespace("js", "https://www.w3.org/2019/wot/json-schema#");
          writer.handleNamespace("saref", "https://w3id.org/saref#");

          try (var stream = rdf4jGraph.stream()) {
            stream.forEach(triple -> writer.handleStatement(triple.asStatement()));
          }
          writer.endRDF();
        } catch (final RDFHandlerException e) {
          throw new IOException("RDF handler exception: " + e.getMessage());
        }
        return out.toString(StandardCharsets.UTF_8);
      } catch (final UnsupportedRDFormatException e) {
        throw new IllegalArgumentException("Unsupported RDF syntax: " + e.getMessage());
      }
    } else {
      throw new IllegalArgumentException("Unsupported RDF graph implementation");
    }
  }

  @Override
  public Graph stringToGraph(final String graphString, final IRI baseIri, final RDFSyntax syntax)
      throws IllegalArgumentException, IOException {
    try (var stringReader = new StringReader(graphString)) {
      final RDFFormat format =
          syntax.equals(RDFSyntax.TURTLE)
          ? RDFFormat.TURTLE
          : RDFFormat.JSONLD;
      final var rdfParser = Rio.createParser(format);
      final var model = new LinkedHashModel();
      rdfParser.setRDFHandler(new StatementCollector(model));
      rdfParser.parse(stringReader, baseIri.getIRIString());
      return this.rdfImpl.asGraph(model);
    } catch (final RDFParseException e) {
      throw new IllegalArgumentException("RDF parse error: " + e.getMessage());
    } catch (final RDFHandlerException e) {
      throw new IOException("RDF handler exception: " + e.getMessage());
    }
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
