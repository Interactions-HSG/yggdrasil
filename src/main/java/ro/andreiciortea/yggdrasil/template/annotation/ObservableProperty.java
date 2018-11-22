package ro.andreiciortea.yggdrasil.template.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ObservableProperty {
  // NOTE: the type of the property is implicitly defined in the definition
  // TODO: Type checking?? What types are allowed or can be serialized to Turtle/JSON-LD
  // path to get the value of the observable property`artifacts/{id}/path`
  String path() default "";
  // name of the observable property
  // NOTE: gets replaced by the property's name if not defined
  String name() default "";

}
