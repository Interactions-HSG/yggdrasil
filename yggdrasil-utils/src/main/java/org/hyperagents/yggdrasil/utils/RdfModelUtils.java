package org.hyperagents.yggdrasil.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.rio.*;
import org.eclipse.rdf4j.rio.helpers.BasicWriterSettings;
import org.eclipse.rdf4j.rio.helpers.JSONLDMode;
import org.eclipse.rdf4j.rio.helpers.JSONLDSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;

/**
 * Utility class for working with RDF models.
 */
public final class RdfModelUtils {
  private RdfModelUtils() {}

  /**
   * Converts a given RDF model to a string representation in the specified format.
   *
   * @param model  the RDF model to convert
   * @param format the format in which the model should be serialized
   * @return the string representation of the RDF model
   * @throws IllegalArgumentException if the RDF format is not supported
   * @throws IOException              if an I/O error occurs during serialization
   */
  public static String modelToString(final Model model, final RDFFormat format)
      throws IllegalArgumentException, IOException {
    try (var out = new ByteArrayOutputStream()) {
      Optional<String> base = Optional.empty();
      try {
        var statements = model.getStatements(null, createIri("http://www.w3.org/1999/02/22-rdf-syntax-ns#type"), createIri("https://purl.org/hmas/ResourceProfile"));
        var firstStatement = statements.iterator().next();
        base = String.valueOf(firstStatement.getSubject()).describeConstable();
      } catch (Exception ignored) {}

      RDFWriter writer;

      if (base.isPresent()) {
        writer = Rio.createWriter(format, out, base.get());
      }
      else {
        writer = Rio.createWriter(format,out);
      }
      if (format.equals(RDFFormat.JSONLD)) {
        writer.getWriterConfig()
              .set(JSONLDSettings.JSONLD_MODE,
                JSONLDMode.FLATTEN)
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
        model.getNamespaces().forEach(namespace ->
            writer.handleNamespace(namespace.getPrefix(), namespace.getName()));
        model.forEach(writer::handleStatement);
        writer.endRDF();
      } catch (final RDFHandlerException e) {
        throw new IOException("RDF handler exception: " + e.getMessage());
      }
      return out.toString(StandardCharsets.UTF_8);
    } catch (final UnsupportedRDFormatException e) {
      throw new IllegalArgumentException("Unsupported RDF syntax: " + e.getMessage());
    } catch (URISyntaxException e) {
      throw new IllegalArgumentException("Could not set Base for Rio Writer: " + e.getMessage());
    }
  }

  /**
   * Converts a string representation of an RDF graph to an RDF model.
   *
   * @param graphString the string representation of the RDF graph
   * @param baseIri     the base IRI for resolving relative IRIs
   * @param format      the format of the RDF graph
   * @return the RDF model
   * @throws IllegalArgumentException if RDF format is not supported / the graph string is invalid
   * @throws IOException              if an I/O error occurs during parsing
   */
  public static Model stringToModel(
      final String graphString,
      final IRI baseIri,
      final RDFFormat format
  ) throws IllegalArgumentException, IOException {
    try (var stringReader = new StringReader(graphString)) {
      final var rdfParser = Rio.createParser(format);
      final var model = new LinkedHashModel();
      rdfParser.setRDFHandler(new StatementCollector(model));
      rdfParser.parse(stringReader, baseIri.stringValue());
      return model;
    } catch (final RDFParseException e) {
      throw new IllegalArgumentException("RDF parse error: " + e.getMessage());
    } catch (final RDFHandlerException e) {
      throw new IOException("RDF handler exception: " + e.getMessage());
    }
  }

  /**
   * Creates an IRI object from the given string.
   *
   * @param iriString the string representation of the IRI
   * @return the IRI object
   * @throws IllegalArgumentException if the string representation is not a valid IRI
   */
  public static IRI createIri(final String iriString) throws IllegalArgumentException {
    return SimpleValueFactory.getInstance().createIRI(iriString);
  }
}
