package ro.andreiciortea.yggdrasil.template;

import com.fasterxml.jackson.databind.introspect.AnnotatedParameter;
import com.google.gson.Gson;
import io.github.classgraph.*;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.rdf4j.RDF4J;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import ro.andreiciortea.yggdrasil.core.EventBusMessage;
import ro.andreiciortea.yggdrasil.core.EventBusRegistry;
import ro.andreiciortea.yggdrasil.store.RdfStore;
import ro.andreiciortea.yggdrasil.store.impl.RdfStoreFactory;
import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.*;

public class TemplateVerticle extends AbstractVerticle {
  private RdfStore store;
  private List<org.apache.commons.rdf.api.IRI> iris = new ArrayList<>();
  private Map<String, String> classMapping = new HashMap<>();
  private Map<String, Object> objectMapping = new HashMap<>();
  private ClassInfoList artifactClasses;
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
        handleGetAllTemplates(message);
        break;
      case INSTANTIATE_TEMPLATE:
        String requestIRIString = request.getHeader(EventBusMessage.Headers.REQUEST_IRI).get();
        org.apache.commons.rdf.api.IRI requestIRI = store.createIRI(requestIRIString);
        handleInstatiateTemplate(requestIRI, request, message);
        break;
      case TEMPLATE_ACTIVITY:
        String entityIRI = request.getHeader(EventBusMessage.Headers.ENTITY_IRI).get();
        String activity =  request.getHeader(EventBusMessage.Headers.ENTITY_ACTIVITY).get();
        handleEntityActivity(entityIRI, activity, request, message);
    }
    // TODO reply 500 if no matches
  }

  private void handleEntityActivity(String entityIRI, String activity, EventBusMessage request, Message<String> message) {
    Object target = objectMapping.get(entityIRI);
    if (target == null) {
      // TODO 404 target not found!
      return;
    }
    for (Method method : target.getClass().getMethods()) {
      if (method.getAnnotation(Action.class) != null && method.getAnnotation(Action.class).path().equals(activity)) {
        System.out.println("invoke action");
        try {
          // TODO add params
          method.invoke(target);
          // TODO: reply with better message/return value
          replyWithPayload(message, "ok");
          return;

        } catch (IllegalAccessException e) {
          e.printStackTrace();
        } catch (InvocationTargetException e) {
          // TODO: reply fail
          e.printStackTrace();
        }
      }
    }
    // check for observable property to be returned
    for (Field field : target.getClass().getFields()) {
      if (field.getAnnotation(ObservableProperty.class) != null && field.getAnnotation(ObservableProperty.class).path().equals(activity)) {
        try {
          Object value = field.get(target);
          replyWithPayload(message, value.toString());
          return;
        } catch (IllegalAccessException e) {
          e.printStackTrace();
          // TODO reply fail
        }
      }
    }
  }

  private void handleInstatiateTemplate(org.apache.commons.rdf.api.IRI requestIRI, EventBusMessage request, Message<String> message) {
    Optional<String> slug = request.getHeader(EventBusMessage.Headers.ENTITY_IRI_HINT);
    String entityIRIString = generateEntityIRI(requestIRI.getIRIString().replaceAll("/templates", ""), slug);
    org.apache.commons.rdf.api.IRI entityIRI = store.createIRI(entityIRIString);

    Optional<String> classIri = request.getPayload();
    if (classIri.isPresent() && classIri.get().length() > 0 && classMapping.containsKey(classIri.get())) {
      Class<?> aClass;
      String className = classMapping.get(classIri.get());
      try {
        aClass = Class.forName(className);
        Constructor<?> ctor = aClass.getConstructor();
        Object object = ctor.newInstance();
        objectMapping.put(entityIRI.getIRIString(), object);
        replyWithPayload(message, entityIRI.getIRIString());

      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      } catch (NoSuchMethodException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (InstantiationException e) {
        e.printStackTrace();
      } catch (InvocationTargetException e) {
        e.printStackTrace();
      }
    }
    // TODO: handle fail!
  }

  private void handleGetAllTemplates(Message<String> message) {
    RDFSyntax syntax = RDFSyntax.TURTLE;
    String result = "";
    for (org.apache.commons.rdf.api.IRI iri : iris) {
      Optional<org.apache.commons.rdf.api.Graph> graph = store.getEntityGraph(iri);
      if (graph.isPresent() && graph.get().size() > 0) {
        try {
          result = result + store.graphToString(graph.get(), syntax);
        } catch (IOException e) {
          e.printStackTrace();
        }
      }
    }
    replyWithPayload(message, result);
  }

  private String generateEntityIRI(String requestIRI, Optional<String> hint) {
    if (!requestIRI.endsWith("/")) {
      requestIRI = requestIRI.concat("/");
    }

    String candidateIRI;

    // Try to generate an IRI using the hint provided in the initial request
    if (hint.isPresent() && !hint.get().isEmpty()) {
      candidateIRI = requestIRI.concat(hint.get());
      if (!iris.contains(store.createIRI(candidateIRI))) {
        return candidateIRI;
      }
    }

    // Generate a new IRI
    do {
      candidateIRI = requestIRI.concat(UUID.randomUUID().toString());
    } while (store.containsEntityGraph(store.createIRI(candidateIRI)));

    return candidateIRI;
  }

  private void scanArtifactTemplates() {
    String pkg = "ro.andreiciortea.yggdrasil.template";
    String artifactAnnotation = pkg + ".annotation.Artifact";

    try (ScanResult scanResult =
           new ClassGraph()
             .enableAllInfo()             // Scan classes, methods, fields, annotations
             .whitelistPackages(pkg)      // Scan com.xyz and subpackages (omit to scan all packages)
             .scan()) {
      artifactClasses = scanResult.getClassesWithAnnotation(artifactAnnotation);
      for (ClassInfo artifactClassInfo : scanResult.getClassesWithAnnotation(artifactAnnotation)) {
        String className = artifactClassInfo.getName();
        org.apache.commons.rdf.api.IRI genIri = generateTemplateRDF(artifactClassInfo);
        iris.add(genIri);
        classMapping.put(genIri.getIRIString(), className);
        System.out.println("Generated description for: " + genIri);
      }
    }
  }

  private org.apache.commons.rdf.api.IRI generateTemplateRDF(ClassInfo artifactClassInfo) {
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
    return rdf4JIRI;

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

  private void replyWithPayload(Message<String> message, String payload) {
    EventBusMessage response = new EventBusMessage(EventBusMessage.MessageType.STORE_REPLY)
      .setHeader(EventBusMessage.Headers.REPLY_STATUS, EventBusMessage.ReplyStatus.SUCCEEDED.name())
      .setPayload(payload);

    message.reply(response.toJson());
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
