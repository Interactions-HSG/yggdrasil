package ro.andreiciortea.yggdrasil.template.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface Artifact {
  // TODO: allow multiple types (better word for type?)
  String type();
  // Name of the templated artifact -> gets replaced by classname if not defined..
  String name() default "";
  String[] prefixes() default {};
}
