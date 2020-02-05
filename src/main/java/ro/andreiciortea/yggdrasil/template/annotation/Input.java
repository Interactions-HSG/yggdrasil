package ro.andreiciortea.yggdrasil.template.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for specifying input parameter types
 *
 * @param name the name of the input parameter (same as the one used in the function)
 *
 * @param type the type of the input parameter, an IRI such as http://www.w3.org/2001/XMLSchema#string
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Input {
  String name() default "";
  String type() default "";
}
