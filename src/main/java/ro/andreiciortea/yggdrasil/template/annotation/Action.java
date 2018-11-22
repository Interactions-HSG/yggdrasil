package ro.andreiciortea.yggdrasil.template.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

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

}


/* TODO: annotation @Eventable?
  -> makes automatic firing of event after execution of action with return value as payload?? */
