package ro.andreiciortea.yggdrasil.template.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation marking a class to be a software artifact. This makes the class to be indexed by Yggdrasil as software artifact.
 * As a consequence a RDF description of this class is generated and instances of this artifacts can be created by issuing a
 * HTTP POST request to '../artifacts/templates/ArtifactName' with the following json payload
 *  {
 * 	"artifactClass" : "http://localhost:8080/artifacts/templates/MyArtifact",
 * 	"additionalTriples" : [{"predicate": "eve:test", "object" : "eve:test2"}]
 * }
 *
 * where "artifactClass" is the uri of the software artifact's description and "additionalTriples" an array of objects defining
 * addtitional rdf triples (predicate and object) to be added to the representation of the artifact.
 *
 * @param type type of the artifact added to the description. E.g. iot:Lightbulb
 *
 * @param name name of the artifact put as artifactName in the generated route to access the template description. If none provided the classname is used instead
 *
 * @param prefixes prefixes to be used as the type for example or in the additional triples. provided as a list of Strings,
 *                 where each String is of the form "<abbreviaton>|<prefix>". e.g. {"eve|http://w3id.org/eve#", "td|http://www.w3.org/ns/td#"}
 *
 * @param additions an Annotation of the type RdfAddition to add more rdf triples to the artifacts description at implementation
 *                  time and not at at runtime (as payload argument)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Artifact {
  // TODO: allow multiple types (better word for type?)
  String[] types();
  String name() default "";
  String[] prefixes() default {};
  RdfAddition additions() default @RdfAddition();
}
