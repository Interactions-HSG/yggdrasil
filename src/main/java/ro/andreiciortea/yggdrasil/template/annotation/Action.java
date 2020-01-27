package ro.andreiciortea.yggdrasil.template.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation marking a method of a software artifact as an action that can be invoked.
 *
 * @param path suffix being added to `../artifacts/{id}` resulting in the route `.//artifacts/{id}/path`.
 *             The action can be called by issuing a HTTP PUT request to this endpoint.
 *             The parameters have to be given in a Json object using their names.
 *
 * @param name name of the action put as name in the generated turtle/json+ld description of the artifact.
 *
 * @param type type of the action put as type triple in the generated turtle/json+ld description of the artifact. (E.g. iot:switchOn)
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Action {
  // path to invoke the action on the artifact to generate the route `artifacts/{id}/path`
  String path() default "";
  // name of the action
  // NOTE: gets replaced by actions name if not defined
  String name() default "";
  // iot:switchOn
  String type() default "";

  // TODO add another property to make the action firing an event automatically when it gets invoked.
}
