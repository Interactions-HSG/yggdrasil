package ro.andreiciortea.yggdrasil.template;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.classgraph.*;
import io.vertx.core.AbstractVerticle;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.EventBus;
import io.vertx.core.eventbus.Message;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import org.apache.commons.rdf.api.BlankNode;
import org.apache.commons.rdf.api.RDFSyntax;
import org.apache.commons.rdf.api.Triple;
import org.apache.commons.rdf.rdf4j.RDF4J;
import org.apache.commons.rdf.rdf4j.RDF4JIRI;
import org.apache.commons.rdf.rdf4j.RDF4JLiteral;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleValueFactory;
import org.eclipse.rdf4j.model.util.ModelBuilder;
import org.eclipse.rdf4j.rio.*;
import ro.andreiciortea.yggdrasil.core.EventBusMessage;
import ro.andreiciortea.yggdrasil.core.EventBusRegistry;
import ro.andreiciortea.yggdrasil.http.HttpTemplateHandler;
import ro.andreiciortea.yggdrasil.store.RdfStore;
import ro.andreiciortea.yggdrasil.store.impl.RdfStoreFactory;
import ro.andreiciortea.yggdrasil.template.annotation.Action;
import ro.andreiciortea.yggdrasil.template.annotation.ObservableProperty;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.*;
import java.util.*;

/*
 * Handles artifact templates which are either present in form of a Java class in the current folder (ro.andreiciortea.yggdrasil.template)
 * or provided at runtime in RDF representation. Templates can be instantiated
 *
 *
 */
public class TemplateVerticle extends AbstractVerticle {
  private static final Logger LOGGER = LoggerFactory.getLogger(HttpTemplateHandler.class.getName());

  private RdfStore store;
  private List<org.apache.commons.rdf.api.IRI> iris = new ArrayList<>();
  private Map<String, String> classMapping = new HashMap<>();
  private Map<String, Object> objectMapping = new HashMap<>();
  private Map<String, Set<Triple>> objectTriples = new HashMap<>();
  private Map<String, ClassInfo> classInfoMap = new HashMap<>();
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
        handleInstantiateTemplate(requestIRI, request, message);
        break;
      case TEMPLATE_ACTIVITY:
        String entityIRI = request.getHeader(EventBusMessage.Headers.ENTITY_IRI).get();
        String activity =  request.getHeader(EventBusMessage.Headers.ENTITY_ACTIVITY).get();
        String requestMethod = request.getHeader(EventBusMessage.Headers.REQUEST_METHOD).get();
        handleEntityRequest(entityIRI, activity, requestMethod, request, message);
        break;
      case DELETE_INSTANCE:
        String artifactId = request.getHeader(EventBusMessage.Headers.ARTIFACT_ID).get();
        handleDeleteInstance(artifactId, request, message);
        break;
      case GET_TEMPLATE_DESCRIPTION:
        String classId = request.getHeader(EventBusMessage.Headers.CLASS_IRI).get();
        handleGetTemplateDescription(classId, request, message);
        break;
      case ADD_TRIPLES_INSTANCE:
        Optional<String> objectArtifactIdOpt = request.getHeader(EventBusMessage.Headers.ARTIFACT_ID);
        if(objectArtifactIdOpt.isPresent()) {
          handleUpdateTriples(objectArtifactIdOpt.get(), request, message);
        } else {
          replyNotFound(message);
        }
        break;
      default:
        replyError(message);
    }
  }

  private void handleUpdateTriples(String objectArtifactID, EventBusMessage request, Message<String> message) {
    if (!objectMapping.containsKey(objectArtifactID)) {
      replyNotFound(message);
    }
    else {
      Gson gson = new Gson();
      Optional<String> triples = request.getPayload();
      if (request.getPayload().isPresent()) {
        JsonElement jelem = gson.fromJson(triples.get(), JsonElement.class);
        JsonObject jobj = jelem.getAsJsonObject();
        if (jobj.get("additionalTriples") != null ) {
          JsonArray additionsRdf = jobj.get("additionalTriples").getAsJsonArray();
          Set<Triple> additionalTriples = parseAdditionalTriples(additionsRdf, objectArtifactID);
          if (additionalTriples.size() > 0) {
            objectTriples.put(objectArtifactID, additionalTriples);
          }
          else {
            objectTriples.remove(objectArtifactID);
          }
          try {
            updateRepresentation(objectArtifactID, objectMapping.get(objectArtifactID), message);
          } catch (IOException e) {
            e.printStackTrace();
            replyError(message);
          }
          replyWithPayload(message, "ok");
        } else {
          replyBadRequest(message);
        }
      } else {
        objectTriples.remove(objectArtifactID);
      }
    }
  }

  private void handleGetTemplateDescription(String classIri, EventBusMessage request, Message<String> message) {
    // TODO return jsonld or turtle depending on header
    if (classMapping.containsKey(classIri)) {
      org.apache.commons.rdf.api.IRI classIriProper = store.createIRI(classIri);
      org.apache.commons.rdf.api.Graph classDescription = store.getEntityGraph(classIriProper).get();
      try {
        String classDescriptionString = store.graphToString(classDescription,RDFSyntax.TURTLE);
        replyWithPayload(message, classDescriptionString);
      } catch (IOException e) {
        e.printStackTrace();
        replyError(message);
      }

    } else {
      replyNotFound(message);
    }
  }

  private void handleDeleteInstance(String artifactId, EventBusMessage request, Message<String> message) {
    Gson gson = new Gson();
    if (objectMapping.containsKey(artifactId)) {
      objectMapping.remove(artifactId);
      String jsonStr = gson.toJson("ok");
      replyWithPayload(message, jsonStr);
    } else {
      replyNotFound(message);
    }
  }

  private boolean requestMethodAndActionMatches(Method method, String requestMethod, String action) {
    return method.getAnnotation(Action.class).path().equals(action) && method.getAnnotation(Action.class).requestMethod().equals(requestMethod);
  }

  private void handleEntityRequest(String entityIRI, String action, String requestMethod, EventBusMessage request, Message<String> message) {
    Object target = objectMapping.get(entityIRI);
    Gson gson = new Gson();

    if (target == null) {
      replyNotFound(message);
      return;
    }
    for (Method method : target.getClass().getMethods()) {
      if (method.getAnnotation(Action.class) != null && requestMethodAndActionMatches(method, requestMethod, action)) {
        LOGGER.info("invoke action " + action + " on " + entityIRI);
        LOGGER.info(request.getPayload().get());
        try {
          Object[] obj = new Object[method.getParameters().length];
          // check for arguments of method
          if (method.getParameters().length > 0 && request.getPayload().isPresent()) {
            String payload = request.getPayload().get();
            JsonElement jelem = gson.fromJson(payload, JsonElement.class);
            JsonObject jobj = jelem.getAsJsonObject();

            // prepare method to be invoked with arguments
            Parameter[] params = method.getParameters();
            for (int i = 0; i < params.length; i++) {
              Parameter param = params[i];
              Class paramType = param.getType();
              JsonElement paramJson = jobj.get(param.getName());

              // TODO: convert into switch statement?
              if (paramType.equals(int.class)) {
                int paramValInt = paramJson.getAsInt();
                obj[i] = paramValInt;
              } else if (paramType.equals(String.class)) {
                String paramValStr = paramJson.getAsString();
                obj[i] = paramValStr;
              } else if (paramType.equals(double.class)) {
                double paramValDoub = paramJson.getAsDouble();
                obj[i] = paramValDoub;
              } else if (paramType.equals(float.class)) {
                float paramValFlo = paramJson.getAsFloat();
                obj[i] = paramValFlo;
              } else  if (paramType.equals(boolean.class)) {
                boolean paramValBool = paramJson.getAsBoolean();
                obj[i] = paramValBool;
              }
            }
          }

          Object result = method.invoke(target, obj);
          String jsonStr = gson.toJson(result);

          // update representation with new value
          updateRepresentation(entityIRI, target, message);
          // TODO This feature is just added for demo purposes
          // Send update notification
          /*
          vertx.eventBus().publish(EventBusRegistry.NOTIFICATION_DISPATCHER_BUS_ADDRESS,
            new EventBusMessage(EventBusMessage.MessageType.ENTITY_CHANGED_NOTIFICATION)
              .setHeader(EventBusMessage.Headers.REQUEST_IRI, entityIRI)
              .setPayload(jsonStr)
              .toJson()
          );
          */

          replyWithPayload(message, jsonStr);
          return;
        } catch (IllegalAccessException | InvocationTargetException e) {
          replyError(message);
          e.printStackTrace();
        } catch (IOException e) {
          replyError(message);
          e.printStackTrace();
        }
      }
    }
    // check for observable property to be returned
    for (Field field : target.getClass().getFields()) {
      if (field.getAnnotation(ObservableProperty.class) != null && field.getAnnotation(ObservableProperty.class).path().equals(action)) {
        try {
          Object value = field.get(target);

          replyWithPayload(message, value.toString());
          return;
        } catch (IllegalAccessException e) {
          replyError(message);
          e.printStackTrace();
        }
      }
    }
  }

  private void handleInstantiateTemplate(org.apache.commons.rdf.api.IRI requestIRI, EventBusMessage request, Message<String> message) {
    Optional<String> slug = request.getHeader(EventBusMessage.Headers.ENTITY_IRI_HINT);
    String requestArtifactString = requestIRI.getIRIString().replaceAll("/templates", "");
    String classIri = "";
    Set<Triple> additionalTriples = new HashSet<>();

    // 1st let the rdf store do the uri exclusivity generation checks and store it's representation
    if (!request.getPayload().isPresent()){
      replyBadRequest(message);
    } else {
      Optional<String> payload = request.getPayload();

      Gson gson = new Gson();
      JsonElement jelem = gson.fromJson(payload.get(), JsonElement.class);
      JsonObject jobj = jelem.getAsJsonObject();
      classIri = jobj.get("artifactClass").getAsString();
      if (jobj.get("additionalTriples") != null ) {
        JsonArray additionsRdf = jobj.get("additionalTriples").getAsJsonArray();
        additionalTriples = parseAdditionalTriples(additionsRdf, classIri);
      }
    }

    if (classIri.length() > 0 && classMapping.containsKey(classIri)) {
      Class<?> aClass;
      String className = classMapping.get(classIri);
      try {
        aClass = Class.forName(className);
        Constructor<?> ctor = aClass.getConstructor();
        Object object = ctor.newInstance();
        // add artifact instance to rdf store
        ClassInfo classInfo = classInfoMap.get(className);
        org.apache.commons.rdf.api.Graph graph = generateRdfGraphFromTemplate(classInfo, classIri, object);

        String graphString = store.graphToString(graph, RDFSyntax.TURTLE);
        graphString = addAdditionalTriplesRDF(graphString, additionalTriples);

        String instanceTurtle = graphString.replace(classIri, "");

        EventBusMessage rdfStoreMessage = new EventBusMessage(EventBusMessage.MessageType.CREATE_ENTITY)
          .setHeader(EventBusMessage.Headers.REQUEST_IRI, requestArtifactString)
          .setHeader(EventBusMessage.Headers.ENTITY_IRI_HINT, slug.get())
          .setHeader(EventBusMessage.Headers.REQUEST_CONTENT_TYPE, "text/turtle")
          .setPayload(instanceTurtle);

        vertx.eventBus().send(EventBusRegistry.RDF_STORE_ENTITY_BUS_ADDRESS, rdfStoreMessage.toJson(), handleRdfStoreReply(object, message, additionalTriples));

      } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException |InstantiationException | InvocationTargetException e) {
        replyError(message);
        e.printStackTrace();
      } catch (IOException e) {
        e.printStackTrace();
      }
    } else {
      replyNotFound(message);
    }
  }

  private String addAdditionalTriplesRDF(String rdfString, Set<Triple> additionalTriples) {
    String result = rdfString.substring(0, rdfString.lastIndexOf(".")) + " ;";
    Iterator<Triple> itr = additionalTriples.iterator();
    while (itr.hasNext()) {
      Triple current = itr.next();
      String obj = current.getObject().ntriplesString();
      String pred = current.getPredicate().ntriplesString();
      result = result + "\n" + pred + " " + obj + " ;";
    }
    result = result.substring(0, result.lastIndexOf(";")) + " .";
    return result;
  }

  private void replyBadRequest(Message<String> message) {
    // TODO implement
  }

  private Handler<AsyncResult<Message<String>>> handleRdfStoreReply(Object generatedObject, Message<String> message, Set<Triple> additionalTriples) {
    return reply -> {
      if (reply.succeeded()) {
        EventBusMessage storeReply = (new Gson()).fromJson((String) reply.result().body(), EventBusMessage.class);
        // add object to object map
        String payload = storeReply.getPayload().get();
        String entityIri = payload.substring(payload.indexOf("<") + 1, payload.indexOf(">"));

        objectTriples.put(entityIri, additionalTriples);

        if (storeReply.succeded()) {
          objectMapping.put(entityIri, generatedObject);
          replyWithPayload(message, payload);
        }
        else if (storeReply.entityNotFound()) {
          replyNotFound(message);
        }
        else {
          LOGGER.error(storeReply.getHeader(EventBusMessage.Headers.REPLY_STATUS));
          replyError(message);
        }
      }
      else {
        LOGGER.error("Reply failed! " + reply.cause().getMessage());
        replyError(message);
      }
    };
  }

  private Handler<AsyncResult<Message<String>>> handleRdfStoreReply(Message<String> message) {
    return reply -> {
      if (reply.succeeded()) {
        EventBusMessage storeReply = (new Gson()).fromJson((String) reply.result().body(), EventBusMessage.class);
        String payload = storeReply.getPayload().get();

        if (storeReply.succeded()) {
          replyWithPayload(message, payload);
        }
        else if (storeReply.entityNotFound()) {
          replyNotFound(message);
        }
        else {
          LOGGER.error(storeReply.getHeader(EventBusMessage.Headers.REPLY_STATUS));
          replyError(message);
        }
      }
      else {
        LOGGER.error("Reply failed! " + reply.cause().getMessage());
        replyError(message);
      }
    };
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
          replyError(message);
          e.printStackTrace();
        }
      }
    }
    replyWithPayload(message, result);
  }

  private Set<Triple> parseAdditionalTriples(JsonArray additionsRdf, String classIri) {
    Set<Triple> additionalTriples = new HashSet<>();
    for (int i = 0; i < additionsRdf.size(); i++) {
      JsonObject obj = additionsRdf.get(i).getAsJsonObject();
        BlankNode subject = rdfImpl.createBlankNode(classIri);
        RDF4JIRI predicate = rdfImpl.createIRI(obj.get("predicate").getAsString());
        RDF4JLiteral literal = rdfImpl.createLiteral(obj.get("object").getAsString());
        Triple triple = rdfImpl.createTriple(subject, predicate, literal);
        additionalTriples.add(triple);
      }
    return additionalTriples;
  }

  private void updateRepresentation(String entityIRI, Object target,  Message<String> message) throws IOException {
    ClassInfo classInfo = classInfoMap.get(target.getClass().getName());
    org.apache.commons.rdf.api.Graph newGraph = generateRdfGraphFromTemplate(classInfo, entityIRI, target);
    String newRepresentation = store.graphToString(newGraph, RDFSyntax.TURTLE);
    Set<Triple> additionalTriples = objectTriples.get(entityIRI);
    // add additional triples again
    newRepresentation = addAdditionalTriplesRDF(newRepresentation, additionalTriples);

    EventBusMessage rdfStoreMessage = new EventBusMessage(EventBusMessage.MessageType.UPDATE_ENTITY)
      .setHeader(EventBusMessage.Headers.REQUEST_IRI, entityIRI)
      .setPayload(newRepresentation);

    vertx.eventBus().send(EventBusRegistry.RDF_STORE_ENTITY_BUS_ADDRESS, rdfStoreMessage.toJson(), handleRdfStoreReply(message));

  }

  private void scanArtifactTemplates() {
    /* TODO: this path doesn't need to be hardcoded. Could be provided at runtime, which would make it more useful in case
    Yggdrasil becomes a library.
    To scan for applicable templates, ClassGraph is being used and .overrideClassPath(...) might be helpful to achieve the
    configurable template path [https://github.com/classgraph/classgraph/wiki/API:-ClassGraph-Constructor].*/
    String pkg = "ro.andreiciortea.yggdrasil.template";
    String artifactAnnotation = "ro.andreiciortea.yggdrasil.template.annotation.Artifact";
    LOGGER.info("scanning for templates...");
    try (ScanResult scanResult =
           new ClassGraph()
             .enableAllInfo()             // Scan classes, methods, fields, annotations
             .whitelistPackages(pkg)      // Scan com.xyz and subpackages (omit to scan all packages)
             .scan()) {
      for (ClassInfo artifactClassInfo : scanResult.getClassesWithAnnotation(artifactAnnotation)) {
        String className = artifactClassInfo.getName();
        org.apache.commons.rdf.api.IRI genIri = generateTemplateClassIRI(artifactClassInfo);
        org.apache.commons.rdf.api.Graph rdfGraph = generateRdfGraphFromTemplate(artifactClassInfo, genIri.getIRIString());
        store.addEntityGraph(genIri, rdfGraph);
        iris.add(genIri);
        classMapping.put(genIri.getIRIString(), className);
        classInfoMap.put(className, artifactClassInfo);
        LOGGER.info("Generated description for: " + genIri);
      }
    }
  }

  private org.apache.commons.rdf.api.IRI generateTemplateClassIRI(ClassInfo artifactClassInfo) {
    ValueFactory vf = SimpleValueFactory.getInstance();
    String localPrefix = "http://localhost:8080/artifacts/templates/";

    // extract relevant parts of the class
    AnnotationInfoList artifactInfos = artifactClassInfo.getAnnotationInfo().filter(new ArtifactAnnotationFilter());

    AnnotationParameterValueList artifactParameters = artifactInfos.get(0).getParameterValues();
    String artifactNameParam = getArtifactNameParam(artifactParameters, artifactClassInfo);

    org.apache.commons.rdf.api.IRI rdf4JIRI = rdfImpl.createIRI(localPrefix + artifactNameParam);
    return rdf4JIRI;
  }

  /**
   * Parses the prefixes parameter of the @Artifact annotation and adds each provided prefix (which is of the form "<abbreviation>|<prefix>")
   * as a namespace to the provided rdfBuilder ModelBuilder instance.
   */
  private void addPrefixesAsNamespace(ModelBuilder rdfBuilder, AnnotationParameterValueList parameters) {
    if (!parameters.isEmpty()) {
      LOGGER.info(String.format("parsing prefixes from template, found %d", parameters.size()));
      String [] prefixes = (String[]) parameters.get("prefixes");
      Map<String, String> prefixMapping = parseDelimitedStringsToMap("\\|", prefixes);
      for (Map.Entry<String, String> entry : prefixMapping.entrySet()) {
        rdfBuilder.setNamespace(entry.getKey(), entry.getValue());
      }
    }
  }

  /**
   * Parses the additions parameter of the @Artifact annotation and adds the provided rdf additions to the provided rdfBuilder ModelBuilder instance.
   * Additions are "Annotation adding additional rdf triples to a artifact templates description at implementation time"
   */
  private void addAdditions(ModelBuilder rdfBuilder, AnnotationParameterValueList parameters) {
    AnnotationInfo additionsInfo = (AnnotationInfo) parameters.get("additions");
    AnnotationParameterValueList additionsList = additionsInfo.getParameterValues();
    if (!additionsList.isEmpty()) {
      LOGGER.info(String.format("parsing additions from template, found %d", additionsList.size()));
      // TODO: simplify
      String[] predicatesList;
      String[] objectsList;
      String[] zeroElement = (String[]) additionsList.get(0).getValue();
      String[] oneElement = (String[]) additionsList.get(1).getValue();

      if (additionsList.get(0).getName().equals("predicates")) {
        predicatesList = zeroElement;
        objectsList = oneElement;
      } else {
        predicatesList = oneElement;
        objectsList = zeroElement;
      }


      if (predicatesList.length != objectsList.length) {
        throw new Error("Incosistent RDF addition annotation given. Predicates and objects list length doesn't macht!");
      }

      for (int i = 0; i< predicatesList.length; i++) {
        String predicate = predicatesList[i];
        String object = objectsList[i];
        rdfBuilder.add(predicate, object);
      }
    }
  }

  /**
   * For debug purposes: Converts a given rdf model into a string
   */
  private String rdfModelToString(Model m) {
    ByteArrayOutputStream out = new ByteArrayOutputStream();
    Rio.write(m, out, RDFFormat.TURTLE);
    return out.toString();
  }

  /**
   * For debug purposes: Prints the model as well as the graph representation
   * of the given model
   */
  private void printGraphToStringAndModelToString(Model m, String name) {
    LOGGER.info("\nFirst part of RDF representation of " + name);
    LOGGER.info(rdfModelToString(m));
    LOGGER.info("graphToString() function:");
    try {
      LOGGER.info(store.graphToString(rdfImpl.asGraph(m), RDFSyntax.TURTLE));
    } catch(Exception e) {
      LOGGER.error("error while converting the graph to a string");
    }
  }
  /**
   * Initializes the RDF representation of an ArtifactTemplate with the corresponding namespaces and the start
   */
  private void initializeArtifactTemplateModelBuilder(ModelBuilder rdfBuilder, ClassInfo artifactClassInfo,
      AnnotationParameterValueList parameters, String iri, ValueFactory vf) {
    String typeParam = (String) parameters.get("type");
    String artifactNameParam = getArtifactNameParam(parameters, artifactClassInfo);
    IRI artifactName = vf.createIRI(iri);
    rdfBuilder
      .setNamespace("eve", "http://w3id.org/eve#")
      .setNamespace("td", "http://www.w3.org/ns/td#")
      .subject(artifactName)
      .add(org.eclipse.rdf4j.model.vocabulary.RDF.TYPE.stringValue(), typeParam)
      .add("eve:a", "eve:ArtifactTemplate")
      .add("td:name", artifactNameParam);
  }

  private ModelBuilder generateRdfModelBuilderForTemplate(ValueFactory vf, ClassInfo artifactClassInfo, String iri) {
    ModelBuilder artifactBuilder = new ModelBuilder();
    // extract relevant parts of the class
    AnnotationInfoList artifactInfos = artifactClassInfo.getAnnotationInfo().filter(new ArtifactAnnotationFilter());
    AnnotationParameterValueList artifactParameters = artifactInfos.get(0).getParameterValues();
    initializeArtifactTemplateModelBuilder(artifactBuilder, artifactClassInfo, artifactParameters, iri, vf);
    addPrefixesAsNamespace(artifactBuilder, artifactParameters);
    addAdditions(artifactBuilder, artifactParameters);
    return artifactBuilder;
  }

  /**
   * Generates an RDF graph for a given artifactClassInfo and an IRI for an already existing object currentTarget
   */
  private org.apache.commons.rdf.api.Graph generateRdfGraphFromTemplate(ClassInfo artifactClassInfo, String iri, Object currentTarget) {
    ValueFactory vf = SimpleValueFactory.getInstance();
    ModelBuilder artifactBuilder = generateRdfModelBuilderForTemplate(vf, artifactClassInfo, iri);

    addActionRDF(artifactBuilder, artifactClassInfo, vf, iri);
    addPropertyRDF(artifactBuilder, artifactClassInfo, vf, currentTarget);
    addEventsRDF(artifactBuilder, artifactClassInfo, vf);

    Model artifactModel = artifactBuilder.build();
    return rdfImpl.asGraph(artifactModel);
  }

  /**
  * Generates an RDF graph for a given artifactClassInfo and an IRI
  */
  private org.apache.commons.rdf.api.Graph generateRdfGraphFromTemplate(ClassInfo artifactClassInfo, String iri) {
    ValueFactory vf = SimpleValueFactory.getInstance();
    ModelBuilder artifactBuilder = generateRdfModelBuilderForTemplate(vf, artifactClassInfo, iri);

    //TODO: re-activate and fix addPropertyRDF and addEventsRDF, only deactivated to focus on addActionRDF for now
    addActionRDF(artifactBuilder, artifactClassInfo, vf, iri);
    //addPropertyRDF(artifactBuilder, artifactClassInfo, vf);
    //addEventsRDF(artifactBuilder, artifactClassInfo, vf);
    printGraphToStringAndModelToString(artifactBuilder.build(), iri);

    Model artifactModel = artifactBuilder.build();
    //LOGGER.info(artifactModel.toString());
    return rdfImpl.asGraph(artifactModel);
  }

  /**
   * Given an AnnotationInfo this function returns the parameter "name" or, if this parameter is empty, the name of the
   */
  private String getParam(AnnotationInfo annotationInfo, String designator, String defaultValue) {
    String param = (String) annotationInfo.getParameterValues().get(designator);
    if (param.equals("")) {
      return defaultValue;
    } else {
      return param;
    }
  }

  /**
   * adds an input, specified by name and type parameter, to the rdfBuilder ModelBuilder as a child node of the
   * provided inputSchemaNode
   */
  private void addInput(ModelBuilder rdfBuilder, ValueFactory vf, BNode inputSchemaNode, String name, String type) {
    BNode fieldNode = vf.createBNode();
    BNode schemaNode = vf.createBNode();
    rdfBuilder.subject(inputSchemaNode)
      .add("td:field", fieldNode)
      .subject(fieldNode)
        .add("td:name", name)
        .add("td:schema", schemaNode)
        .subject(schemaNode)
          // TODO: add default values e.g. from annotation?
          .add("td:SchemaType", type);
  }

  private Map<String, String> parseDelimitedStringsToMap(String delimiter, String[] strings) {
    Map<String, String> mapping = new HashMap<>();
    for (String str : strings) {
      String[] splitting = str.split(delimiter);
      if (splitting.length == 2) {
        mapping.put(splitting[0], splitting[1]);
      } else {
        throw new Error(String.format("Provided input does not match required format \"<...>" + delimiter + "<...>\": " + str));
      }
    }
    return mapping;
  }

  /**
   * Adds the actions which are provided by the @Action annotation to the rdfBuilder
   */
  private void addActionRDF(ModelBuilder rdfBuilder, ClassInfo artifactClassInfo, ValueFactory vf, String iri) {
    LOGGER.info("IRI = " + iri);
    IRI root = vf.createIRI(iri);
    MethodInfoList actionMethods = artifactClassInfo.getMethodInfo().filter(new ActionMethodFilter());
    for (MethodInfo action: actionMethods) {
      AnnotationInfo annotationInfo = action.getAnnotationInfo().get("ro.andreiciortea.yggdrasil.template.annotation.Action");
      AnnotationParameterValueList actionParameters = annotationInfo.getParameterValues();
      String actionNameParam = getParam(annotationInfo, "name", action.getName());
      String path = getParam(annotationInfo, "path", "/" + actionNameParam);
      String requestMethod = (String) actionParameters.get("requestMethod");

      BNode actionNode = vf.createBNode(actionNameParam);
      BNode formNode = vf.createBNode();
      BNode inputSchemaNode = vf.createBNode();

      rdfBuilder
      .subject(root)
        .add("td:interaction", actionNode)
        .subject(actionNode)
          .add(org.eclipse.rdf4j.model.vocabulary.RDF.TYPE.stringValue(), "td:Action")
          .add("td:name", actionNameParam)
          .add("td:form", formNode)
          .add("td:inputSchema", inputSchemaNode)
          .subject(formNode)
            .add("htv:methodName", requestMethod)
            .add("eve:path", path )
            .add("td:mediaType", "application/json")
            .add("td:rel", "invokeAction")
          .subject(inputSchemaNode)
            .add("td:schemaType", "td:Object");

      String[] inputInfos = (String[]) actionParameters.get("inputs");
      LOGGER.info(String.format("found %d input infos", inputInfos.length));
      Map<String, String> inputTypeMapping = parseDelimitedStringsToMap("\\|", inputInfos);
      for (MethodParameterInfo parameter : action.getParameterInfo()) {
        String inputName = parameter.getName();
        // if no type is provided via annotation, we fall back to parameter java type
        String inputType = inputTypeMapping.getOrDefault(inputName, parameter.getTypeDescriptor().toString());
        addInput(rdfBuilder, vf, inputSchemaNode, inputName, inputType);
      }
    }
  }

  /**
   * Adds the events which are annotated with @Event to rdfBuilder
   */
  private void addEventsRDF(ModelBuilder rdfBuilder, ClassInfo artifactClassInfo, ValueFactory vf) {
    // TODO: add descriptions
    MethodInfoList eventMethods = artifactClassInfo.getMethodInfo().filter(new EventMethodFilter());
    for (MethodInfo event: eventMethods) {
      ModelBuilder eventBuilder = new ModelBuilder();
      AnnotationInfo annotation = event.getAnnotationInfo().get("ro.andreiciortea.yggdrasil.template.annotation.Event");

      String eventName = getParam(annotation, "name", event.getName());
      String path = getParam(annotation, "path", "/events/" + eventName);

      eventBuilder
        .subject(vf.createBNode())
        .add("td:name", eventName)
        .add("eve:path", path);

      rdfBuilder.add("td:events", eventBuilder.build());
    }
  }

  /**
   * Adds the observable properties which are annotated with @ObservableProperty to the rdfBuilder and adds the value of currentTarget
   */
  private void addPropertyRDF(ModelBuilder rdfBuilder, ClassInfo artifactClassInfo, ValueFactory vf, Object currentTarget) {
    FieldInfoList propertyList = artifactClassInfo.getDeclaredFieldInfo().filter(new ObservablePropertyFilter());
    for (FieldInfo property : propertyList) {
      ModelBuilder propertyBuilder = createObservablePropertyModelBuilder(property, vf);
      Object propertyValue;
      try {
        propertyValue = currentTarget.getClass().getField(property.getName()).get(currentTarget);
        if (propertyValue != null) {
          propertyBuilder.add("td:value", propertyValue.toString());
        }
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (NoSuchFieldException e) {
        e.printStackTrace();
      }
      rdfBuilder.add("td:properties", propertyBuilder.build());
    }
  }

  /**
  * Adds the observable properties which are annotated with @ObservableProperty to the rdfBuilder
  */
  private void addPropertyRDF(ModelBuilder rdfBuilder, ClassInfo artifactClassInfo, ValueFactory vf) {
    FieldInfoList propertyList = artifactClassInfo.getDeclaredFieldInfo().filter(new ObservablePropertyFilter());
    for (FieldInfo property : propertyList) {
      ModelBuilder propertyBuilder = createObservablePropertyModelBuilder(property, vf);
      rdfBuilder.add("td:properties", propertyBuilder.build());
    }
  }

  /**
   * Returns a ModelBuilder with the parameters of the ObservableProperty annotation added
   */
  private ModelBuilder createObservablePropertyModelBuilder(FieldInfo property, ValueFactory vf) {
    ModelBuilder propertyBuilder = new ModelBuilder();
    // TODO map Java classes to xml type
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
    return propertyBuilder;
  }

  /**
   * Returns the name parameter, if provided, or else the name of the class
   */
  private String getArtifactNameParam(AnnotationParameterValueList artifactParameters, ClassInfo artifactClassInfo) {
    String artifactNameParam = (String) artifactParameters.get("name");
    if (artifactNameParam.equals("")) {
      // set name to the class name
      String fullClassName = artifactClassInfo.getName();
      artifactNameParam = fullClassName.substring(fullClassName.lastIndexOf(".")+1);
    }
    return artifactNameParam;
  }

  private void replyWithPayload(Message<String> message, String payload) {
    EventBusMessage response = new EventBusMessage(EventBusMessage.MessageType.STORE_REPLY)
      .setHeader(EventBusMessage.Headers.REPLY_STATUS, EventBusMessage.ReplyStatus.SUCCEEDED.name())
      .setPayload(payload);

    message.reply(response.toJson());
  }

  private void replyNotFound(Message<String> message) {
    EventBusMessage response = new EventBusMessage(EventBusMessage.MessageType.STORE_REPLY)
      .setHeader(EventBusMessage.Headers.REPLY_STATUS, EventBusMessage.ReplyStatus.ENTITY_NOT_FOUND.name());

    message.reply(response.toJson());
  }

  private void replyError(Message<String> message) {
    EventBusMessage response = new EventBusMessage(EventBusMessage.MessageType.STORE_REPLY)
      .setHeader(EventBusMessage.Headers.REPLY_STATUS, EventBusMessage.ReplyStatus.FAILED.name());

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
