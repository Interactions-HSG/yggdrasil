package ro.andreiciortea.yggdrasil.template;

import com.google.gson.Gson;
import io.github.classgraph.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.apache.commons.rdf.rdf4j.RDF4J;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import ro.andreiciortea.yggdrasil.core.EventBusMessage;
import ro.andreiciortea.yggdrasil.core.EventBusRegistry;
import ro.andreiciortea.yggdrasil.store.RdfStore;
import ro.andreiciortea.yggdrasil.store.impl.RdfStoreFactory;

public class TemplateVerticle extends AbstractVerticle {
  private RdfStore store;
  private RDF4J rdfImpl;

  @Override
  public void start() {
    // TODO: chain it before starting the http verticle
    EventBus eventBus = vertx.eventBus();
    eventBus.consumer(EventBusRegistry.TEMPLATE_HANDLER_BUS_ADDRESS, this::handleTemplatesRequest);

    // create separate store for artifact templates to not mess them up with artifact instances
    store = RdfStoreFactory.createStore("template_store");
    rdfImpl = new RDF4J();

    // scan for annotated artifact classes and add them to the RDF store
    scanArtifactTemplates();
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
    // TODO implement
  }

  private void scanArtifactTemplates() {
    // TODO: generate routes
    String pkg = "ro.andreiciortea.yggdrasil.template";
    String artifactAnnotation = pkg + ".annotation.Artifact";

    try (ScanResult scanResult =
           new ClassGraph()
             .enableAllInfo()             // Scan classes, methods, fields, annotations
             .whitelistPackages(pkg)      // Scan com.xyz and subpackages (omit to scan all packages)
             .scan()) {
      for (ClassInfo artifactClassInfo : scanResult.getClassesWithAnnotation(artifactAnnotation)) {
        generateTemplateRDF(artifactClassInfo);
      }
    }
  }

  private void generateTemplateRDF(ClassInfo artifactClassInfo) {
    ValueFactory vf = SimpleValueFactory.getInstance();
    ModelBuilder artifactBuilder = new ModelBuilder();
    String localPrefix = "http://localhost:8080/artifacts/templates/";

    // extract relevant parts of the class
    AnnotationInfoList artifactInfos = artifactClassInfo.getAnnotationInfo().filter(new ArtifactAnnotationFilter());

    AnnotationParameterValueList artifactParameters = artifactInfos.get(0).getParameterValues();
    String typeParam = (String) artifactParameters.get("type");
    String artifactNameParam = (String) artifactParameters.get("name");
    if (artifactNameParam.equals("")) {
      // set name to the class name
      String fullClassName = artifactClassInfo.getName();
      artifactNameParam = fullClassName.substring(fullClassName.lastIndexOf(".")+1);
    }
    IRI artifactName = vf.createIRI(localPrefix + artifactNameParam);

    LinkedHashModel graph = new LinkedHashModel();

    // generate model for actions
    artifactBuilder
      // TODO: get prefixes from annotations
      .setNamespace("eve", "http://w3id.org/eve#")
      .setNamespace("td", "http://www.w3.org/ns/td#")
      .subject(artifactName)
      .add("eve:a", typeParam)
      .add("eve:a", "eve:ArtifactTemplate")
      .add("td:name", artifactNameParam)
      .build();

    MethodInfoList actionMethods = artifactClassInfo.getMethodInfo().filter(new ActionMethodFilter());
    addActionRDF(artifactBuilder, actionMethods, vf);

    //properties
    FieldInfoList propertyList = artifactClassInfo.getDeclaredFieldInfo().filter(new ObservablePropertyFilter());
    addPropertyRDF(artifactBuilder, propertyList, vf);

    MethodInfoList events = artifactClassInfo.getMethodInfo().filter(new EventMethodFilter());
    addEventsRDF(artifactBuilder, events, vf);

    // put created graph into store
    Model artifactModel = artifactBuilder.build();
    org.apache.commons.rdf.api.Graph rdf4JGraph = rdfImpl.asGraph(artifactModel);
    org.apache.commons.rdf.api.IRI rdf4JIRI = rdfImpl.createIRI(localPrefix + artifactNameParam);
    store.addEntityGraph(rdf4JIRI, rdf4JGraph);

  }

  private void addEventsRDF(ModelBuilder artifactBuilder, MethodInfoList eventList, ValueFactory vf) {
    // TODO: add descriptions
    for (MethodInfo event: eventList) {
      ModelBuilder eventBuilder = new ModelBuilder();
      AnnotationInfo annotation = event.getAnnotationInfo().get("ro.andreiciortea.yggdrasil.template.annotation.Event");

      String eventName = (String) annotation.getParameterValues().get("name");
      if (eventName.equals("")) {
        eventName = event.getName();
      }

      String path = (String) annotation.getParameterValues().get("path");
      if (path.equals("")) {
        path = "/events/" + eventName;
      }

      eventBuilder
        .subject(vf.createBNode())
        .add("td:name", eventName)
        .add("eve:path", path);

      artifactBuilder.add("td:events", eventBuilder.build());
    }
  }

  private void addPropertyRDF(ModelBuilder artifactBuilder, FieldInfoList propertyList, ValueFactory vf) {
    for (FieldInfo property : propertyList) {
      ModelBuilder propertyBuilder = new ModelBuilder();
      // TODO map Java types
      String propertyType = property.getTypeDescriptor().toString();
      AnnotationInfo annotation = property.getAnnotationInfo().get("ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty");

      String propertyName = (String) annotation.getParameterValues().get("name");
      if (propertyName.equals("")) {
        propertyName = property.getName();
      }

      String path = (String) annotation.getParameterValues().get("path");
      if (path.equals("")) {
        path = "/properties/" + propertyName;
      }

      propertyBuilder
        .subject(vf.createBNode())
        .add("td:name", propertyName)
        .add("td:type", propertyType)
        .add("eve:path", path);

      artifactBuilder.add("td:properties", propertyBuilder.build());
    }


  }

  private void addActionRDF(ModelBuilder artifactBuilder, MethodInfoList actionMethods, ValueFactory vf) {
    for (MethodInfo action: actionMethods) {
      ModelBuilder actionBuilder = new ModelBuilder();
      ModelBuilder inputFieldBuilder = new ModelBuilder();

      AnnotationInfoList annotationInfo = action.getAnnotationInfo();
      AnnotationParameterValueList actionParameters = annotationInfo.get(0).getParameterValues();
      String actionNameParam = (String) actionParameters.get("name");
      if (actionNameParam.equals("")) {
        // set name to the class name
        actionNameParam = action.getName();
      }

      String path = (String ) actionParameters.get("path");
      if (path.equals("")) {
        path = "/actions/" + actionNameParam;
      }

      BNode formNode = vf.createBNode("td:form");
      BNode inputSchemaNode = vf.createBNode("td:inputForm");

      actionBuilder
        .subject("td:interaction")
        .add("td:name", actionNameParam)
        .add("td:form", formNode)
        .add("td:inputSchema", inputSchemaNode)
        .subject(formNode)
        .add("http:methodName", "PUT")
        .add("eve:path", path )
        .add("td:mediaType", "application/json")
        .add("td:rel", "invokeAction")
        .subject(inputSchemaNode)
        .add("td:schemaType", "td:Objcect");

      MethodParameterInfo[] parameterInfos = action.getParameterInfo();
      for (MethodParameterInfo parameter : parameterInfos) {
        BNode fieldNode = vf.createBNode("td:field");
        BNode schemaNode = vf.createBNode("td:schema");
        String inputName = parameter.getName();
        String inputType = parameter.getTypeDescriptor().toString();
        inputFieldBuilder
          .subject(fieldNode)
          .add("td:name", inputName)
          .add("td:schema", schemaNode)
          .subject(schemaNode)
          // TODO add default values, specified by annotation?
          .add("td:SchemaType", "td:" + inputType);
      }
      Model inputModel = inputFieldBuilder.build();
      actionBuilder
        .subject(inputSchemaNode)
        .add("td:field", inputModel);
      Model actionModel = actionBuilder.build();
      artifactBuilder
        .add("td:interactions", actionModel);
    }
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




