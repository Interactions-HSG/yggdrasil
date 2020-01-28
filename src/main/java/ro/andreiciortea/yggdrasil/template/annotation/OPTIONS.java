package ro.andreiciortea.yggdrasil.template.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
/**
 * Annotation marking a method of the artifact template class to retrieve allowed options
 *
 * @param path path appended to the uri of the artifact instance to call the OPTIONS method
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface OPTIONS {
  String path() default "";
}
