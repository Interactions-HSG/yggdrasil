package org.hyperagents.yggdrasil.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFHandlerException;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.UnsupportedRDFormatException;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

public final class RdfModelUtils {
  private RdfModelUtils() {}

  @SuppressWarnings("removal")
  public static String modelToString(final Model model, final RDFFormat format)
      throws IllegalArgumentException, IOException {
    try (var out = new ByteArrayOutputStream()) {
      final var writer = Rio.createWriter(format, out);
      if (format.equals(RDFFormat.JSONLD)) {
        writer.getWriterConfig()
              .set(JSONLDSettings.JSONLD_MODE, org.eclipse.rdf4j.rio.helpers.JSONLDMode.FLATTEN)
              .set(JSONLDSettings.USE_NATIVE_TYPES, true)
              .set(JSONLDSettings.OPTIMIZE, true);
      }

      writer.getWriterConfig()
            .set(BasicWriterSettings.PRETTY_PRINT, true)
            .set(BasicWriterSettings.RDF_LANGSTRING_TO_LANG_LITERAL, true)
            .set(BasicWriterSettings.XSD_STRING_TO_PLAIN_LITERAL, true)
            .set(BasicWriterSettings.INLINE_BLANK_NODES, true);
      try {
        writer.startRDF();

        writer.handleNamespace("hmas", "https://purl.org/hmas/");
        writer.handleNamespace("td", "https://www.w3.org/2019/wot/td#");
        writer.handleNamespace("htv", "http://www.w3.org/2011/http#");
        writer.handleNamespace("hctl", "https://www.w3.org/2019/wot/hypermedia#");
        writer.handleNamespace("wotsec", "https://www.w3.org/2019/wot/security#");
        writer.handleNamespace("dct", "http://purl.org/dc/terms/");
        writer.handleNamespace("js", "https://www.w3.org/2019/wot/json-schema#");
        writer.handleNamespace("saref", "https://w3id.org/saref#");

        model.forEach(writer::handleStatement);

        writer.endRDF();
      } catch (final RDFHandlerException e) {
        throw new IOException("RDF handler exception: " + e.getMessage());
      }
      return out.toString(StandardCharsets.UTF_8);
    } catch (final UnsupportedRDFormatException e) {
      throw new IllegalArgumentException("Unsupported RDF syntax: " + e.getMessage());
    }
  }

  public static Model stringToModel(
      final String graphString,
      final IRI baseIri,
      final RDFFormat format
  ) throws IllegalArgumentException, IOException {
    try (var stringReader = new StringReader(graphString)) {
      final var rdfParser = Rio.createParser(format);
      final var model = new LinkedHashModel();
      rdfParser.setRDFHandler(new StatementCollector(model));
      rdfParser.parse(stringReader, baseIri.toString());
      return model;
    } catch (final RDFParseException e) {
      throw new IllegalArgumentException("RDF parse error: " + e.getMessage());
    } catch (final RDFHandlerException e) {
      throw new IOException("RDF handler exception: " + e.getMessage());
    }
  }

  public static IRI createIri(final String iriString) throws IllegalArgumentException {
    return SimpleValueFactory.getInstance().createIRI(iriString);
  }
}
