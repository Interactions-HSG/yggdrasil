package org.hyperagents.yggdrasil.template.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Annotation marking a method of the artifact template class to fire an event on invocation.
 * NOTE: The generation of the event description in the artifacts TD is implemented. The actual firing of the event from
 * JAVA not yet.
 *
 * @param path path appended to the uri of the artifact instance to subscribe/unsubscribe from the current event.
 *             If no path given, the route '../events/methodName' is generated.
 *
 * @param name name of the event used in the thing description of the artifact template / instances
 *             If not given, it gets replaced by the annotated method's name.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Event {
  // path subscribe/unsubscribe from the event on the artifact to generate the route `artifacts/{id}/path`
  String path() default "";
  // name of the event
  String name() default "";

}
