package ro.andreiciortea.yggdrasil.store.impl;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.*;
import org.apache.commons.rdf.rdf4j.RDF4J;
import org.apache.commons.rdf.rdf4j.RDF4JGraph;
import org.apache.commons.rdf.rdf4j.RDF4JTriple;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import ro.andreiciortea.yggdrasil.store.RdfStore;

public class Rdf4jStore implements RdfStore {

//  private final static Logger LOGGER = LoggerFactory.getLogger(Rdf4jStore.class.getName());

  private RDF4J rdfImpl;
  private Dataset dataset;

  public Rdf4jStore() {
    Repository repository = new SailRepository(new MemoryStore());

    rdfImpl = new RDF4J();
    dataset = rdfImpl.asDataset(repository, RDF4J.Option.handleInitAndShutdown);
  }

  @Override
  public boolean containsEntityGraph(IRI entityIri) {
    return dataset.contains(Optional.of(entityIri), null, null, null);
  }

  @Override
  public Optional<Graph> getEntityGraph(IRI entityIri) {
    return dataset.getGraph(entityIri);
  }

  @Override
  public void createEntityGraph(IRI entityIri, Graph entityGraph) {
    if (entityGraph instanceof RDF4JGraph) {
      addEntityGraph(entityIri, entityGraph);
    } else {
      throw new IllegalArgumentException("Unsupported RDF graph implementation");
    }
  }

  @Override
  public void updateEntityGraph(IRI entityIri, Graph entityGraph) {
    if (entityGraph instanceof RDF4JGraph) {
      deleteEntityGraph(entityIri);
      addEntityGraph(entityIri, entityGraph);
    } else {
      throw new IllegalArgumentException("Unsupported RDF graph implementation");
    }
  }

  @Override
  public void deleteEntityGraph(IRI entityIri) {
    dataset.remove(Optional.of(entityIri), null, null, null);
  }

  @Override
  public IRI createIRI(String iriString) throws IllegalArgumentException {
    return rdfImpl.createIRI(iriString);
  }

  @Override
  public String graphToString(Graph graph, RDFSyntax syntax) throws IllegalArgumentException, IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    RDFWriter writer;

    if (syntax.equals(RDFSyntax.JSONLD)) {
      writer = Rio.createWriter(RDFFormat.JSONLD, out);
      writer.getWriterConfig().set(JSONLDSettings.JSONLD_MODE, JSONLDMode.FLATTEN);
      writer.getWriterConfig().set(JSONLDSettings.USE_NATIVE_TYPES, true);
      writer.getWriterConfig().set(JSONLDSettings.OPTIMIZE, true);
    } else {
      writer = Rio.createWriter(RDFFormat.TURTLE, out);
    }
    writer.getWriterConfig().set(BasicWriterSettings.INLINE_BLANK_NODES, true);

    if (graph instanceof RDF4JGraph) {
      try {
        writer.startRDF();
        try (Stream<RDF4JTriple> stream = ((RDF4JGraph) graph).stream()) {
          stream.forEach(triple -> {
            writer.handleStatement(triple.asStatement());
          });
        }
        writer.endRDF();
      }
      catch (RDFHandlerException e) {
        throw new IOException("RDF handler exception: " + e.getMessage());
      }
      catch (UnsupportedRDFormatException e) {
        throw new IllegalArgumentException("Unsupported RDF syntax: " + e.getMessage());
      }
      finally {
        out.close();
      }
    } else {
      throw new IllegalArgumentException("Unsupported RDF graph implementation");
    }

    return out.toString();
  }

  @Override
  public Graph stringToGraph(String graphString, IRI baseIRI, RDFSyntax syntax) throws IllegalArgumentException, IOException {
    StringReader stringReader = new StringReader(graphString);

    RDFFormat format = RDFFormat.JSONLD;
    if (syntax.equals(RDFSyntax.TURTLE)) {
      format = RDFFormat.TURTLE;
    }

    RDFParser rdfParser = Rio.createParser(format);
    Model model = new LinkedHashModel();
    rdfParser.setRDFHandler(new StatementCollector(model));

    try {
      rdfParser.parse(stringReader, baseIRI.getIRIString());
    }
    catch (RDFParseException e) {
      throw new IllegalArgumentException("RDF parse error: " + e.getMessage());
    }
    catch (RDFHandlerException e) {
      throw new IOException("RDF handler exception: " + e.getMessage());
    }
    finally {
      stringReader.close();
    }

    return rdfImpl.asGraph(model);
  }

  public void addEntityGraph(IRI entityIri, Graph entityGraph) {
    try(Stream<RDF4JTriple> stream = ((RDF4JGraph) entityGraph).stream()) {
      stream.forEach(triple -> {
        dataset.add(entityIri, triple.getSubject(), triple.getPredicate(), triple.getObject());
      });
    }
  }
}
