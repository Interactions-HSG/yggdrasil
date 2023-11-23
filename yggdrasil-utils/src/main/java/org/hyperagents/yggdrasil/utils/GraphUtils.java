package org.hyperagents.yggdrasil.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.apache.commons.rdf.api.Graph;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.rdf4j.RDF4J;
import org.apache.commons.rdf.rdf4j.RDF4JGraph;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
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

public class GraphUtils {
  private static final RDF4J RDF = new RDF4J();

  public static String graphToString(final Graph graph, final RDFSyntax syntax)
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

  public static Graph stringToGraph(final String graphString, final IRI baseIri, final RDFSyntax syntax)
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
      return RDF.asGraph(model);
    } catch (final RDFParseException e) {
      throw new IllegalArgumentException("RDF parse error: " + e.getMessage());
    } catch (final RDFHandlerException e) {
      throw new IOException("RDF handler exception: " + e.getMessage());
    }
  }

  public static IRI createIri(final String iriString) throws IllegalArgumentException {
    return RDF.createIRI(iriString);
  }
}
