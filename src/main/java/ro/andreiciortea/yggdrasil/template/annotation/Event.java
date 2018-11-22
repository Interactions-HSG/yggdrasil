package ro.andreiciortea.yggdrasil.template.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Event {
  // path subscribe/unsubscribe from the event on the artifact to generate the route `artifacts/{id}/path`
  String path() default "";
  // name of the event
  // NOTE: gets replaced by methods name if not defined
  String name() default "";

}
