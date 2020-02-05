package ro.andreiciortea.yggdrasil.template.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation allowing REST methods on an attribute
 *
 * @param requestMethod any of the allowed HTTP methods (GET, POST, PUT, OPTIONS, DELETE, ...)
 * @param path path appended to the uri of the artifact instance to call the DELETE method
 * @param name name of the action put as name in the generated turtle/json+ld description of the artifact.
 * @param type type of the action put as type triple in the generated turtle/json+ld description of the artifact. (E.g. iot:switchOn)
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Action {
  String requestMethod() default "GET";
  String path() default "";
  String type() default "";
  String name() default "";
  Input[] inputs() default {};
  Output output() default @Output();
}
