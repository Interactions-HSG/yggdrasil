package ro.andreiciortea.yggdrasil.template;

import com.google.gson.Gson;
import io.github.classgraph.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.model.vocabulary.FOAF;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import ro.andreiciortea.yggdrasil.core.EventBusMessage;
import ro.andreiciortea.yggdrasil.core.EventBusRegistry;
import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;
import sun.java2d.pipe.SpanShapeRenderer;

import java.util.List;

public class TemplateVerticle extends AbstractVerticle {
  @Override
  public void start() {

    EventBus eventBus = vertx.eventBus();

    eventBus.consumer(EventBusRegistry.TEMPLATE_HANDLER_BUS_ADDRESS, this::handleTemplatesRequest);
  }

  private void handleTemplatesRequest(Message<String> message) {
    EventBusMessage request = (new Gson()).fromJson(message.body().toString(), EventBusMessage.class);

    switch (request.getMessageType()) {
      case GET_TEMPLATES:
        handleGetAllTemplates();
    }
    // TODO reply

  }

  private void handleGetAllTemplates() {
    // TODO: move this to the startup of the node! atm the templates are fixed!! And only redo for new artifact templates
    // TODO: generate routes
    System.out.println("Do reflection and stuff!");
    String pkg = "ro.andreiciortea.yggdrasil.template";
    String artifactAnnotation = pkg + ".annotation.Artifact";

    try (ScanResult scanResult =
           new ClassGraph()
     //        .verbose()                   // Log to stderr
             .enableAllInfo()             // Scan classes, methods, fields, annotations
             .whitelistPackages(pkg)      // Scan com.xyz and subpackages (omit to scan all packages)
             .scan()) {                   // Start the scan
      for (ClassInfo artifactClassInfo : scanResult.getClassesWithAnnotation(artifactAnnotation)) {
        generateTemplateRDF(artifactClassInfo);
      }
    }
    // TODO reply all templates
  }

  private void generateTemplateRDF(ClassInfo artifactClassInfo) {
    ModelBuilder builder = new ModelBuilder();


    // extract relevant parts of the class
    AnnotationInfoList artifactInfos = artifactClassInfo.getAnnotationInfo().filter(new ArtifactAnnotationFilter());
    MethodInfoList actionMethods = artifactClassInfo.getMethodInfo().filter(new ActionMethodFilter());
    FieldInfoList propertyList = artifactClassInfo.getDeclaredFieldInfo().filter(new ObservablePropertyFilter());
    MethodInfoList events = artifactClassInfo.getMethodInfo().filter(new EventMethodFilter());

    AnnotationParameterValueList artifactParameters = artifactInfos.get(0).getParameterValues();
    String typeParam = (String) artifactParameters.get("type");

    Model model = builder
      // TODO: get prefixes from annotations
      .setNamespace("eve", "http://w3id.org/eve#")
      .subject("")
      .add("a", typeParam)
      .add(FOAF.FIRST_NAME, "Pablo")
      .build();



    System.out.println(artifactClassInfo.getName() + " is annotated with artifact providing the following actions: "
      + actionMethods + " and properties: " + propertyList +
      " and events: " + events);
    // TODO: generate RDF representation eve:Template and throw it to the store

  }

  private class ArtifactAnnotationFilter implements AnnotationInfoList.AnnotationInfoFilter {

    @Override
    public boolean accept(AnnotationInfo annotationInfo) {
      String annotationName = "ro.andreiciortea.yggdrasil.template.annotation.Artifact";
      return annotationInfo.getName().equals(annotationName);
    }
  }

  private class EventMethodFilter implements MethodInfoList.MethodInfoFilter {

    @Override
    public boolean accept(MethodInfo methodInfo) {
      return !methodInfo.getAnnotationInfo().filter(new EventAnnotationFilter()).isEmpty();
    }
  }

  private class EventAnnotationFilter implements AnnotationInfoList.AnnotationInfoFilter {

    @Override
    public boolean accept(AnnotationInfo annotationInfo) {
      String annotationName = "ro.andreiciortea.yggdrasil.template.annotation.Event";
      return annotationInfo.getName().equals(annotationName);
    }
  }
  private class ObservablePropertyFilter implements FieldInfoList.FieldInfoFilter {

    @Override
    public boolean accept(FieldInfo fieldInfo) {
      return !fieldInfo.getAnnotationInfo().filter(new PropertyAnnotationFilter()).isEmpty();
    }
  }

  private class PropertyAnnotationFilter implements AnnotationInfoList.AnnotationInfoFilter {

    @Override
    public boolean accept(AnnotationInfo annotationInfo) {
      String annotationName = "ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty";
      return annotationInfo.getName().equals(annotationName);
    }
  }

  private class ActionMethodFilter implements MethodInfoList.MethodInfoFilter {
    @Override
    public boolean accept(MethodInfo methodInfo) {
      return !methodInfo.getAnnotationInfo().filter(new ActionAnnotationFilter()).isEmpty();
    }
  }

  private class ActionAnnotationFilter implements AnnotationInfoList.AnnotationInfoFilter {

    @Override
    public boolean accept(AnnotationInfo annotationInfo) {
      String annotationName = "ro.andreiciortea.yggdrasil.template.annotation.Action";
      return annotationInfo.getName().equals(annotationName);
    }
  }
}




