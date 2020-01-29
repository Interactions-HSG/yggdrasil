package ro.andreiciortea.yggdrasil.template.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import io.vertx.core.http.HttpMethod;

/**
 * Annotation allowing REST methods on an attribute
 *
 * @param httpMethod any of the allowed HTTP methods (GET, POST, PUT, OPTIONS, DELETE, ...)
 * @param path path appended to the uri of the artifact instance to call the DELETE method
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RequestMapping {
  HttpMethod httpMethod() default HttpMethod.GET;
  String path() default "";
}
