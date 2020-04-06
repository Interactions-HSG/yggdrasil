package ro.andreiciortea.yggdrasil.template.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Annotation to specify an action on a template artifact
 *
 * @param requestMethod any of the allowed HTTP methods (GET, POST, PUT, OPTIONS, DELETE, ...)
 * @param path path appended to the uri of the artifact instance to call the DELETE method
 * @param name name of the action put as name in the generated turtle/json+ld description of the artifact.
 * @param type type of the action put as type triple in the generated turtle/json+ld description of the artifact. (E.g. iot:switchOn)
 * @param inputs input parameters along with their types, provided as strings, where each String is of the form "<input_parameter_name>|<type>". e.g. {"param1|http://www.w3.org/2001/XMLSchema#double"}
 * @param output output parameter name and type, see the Output annotation for more details
 * @param additions an Annotation of the type RdfAddition to add more rdf triples to the RDF graph on the Action level at implementation time and not at at runtime (as payload argument)
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Action {
  String requestMethod() default "GET";
  String path() default "";
  String type() default "";
  String name() default "";
  String[] inputs() default {};
  Output output() default @Output();
  RdfAddition additions() default @RdfAddition();
}
