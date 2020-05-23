package org.hyperagents.yggdrasil.template.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation marking a class member of a software artifact class as an observable property.
 *
 * @param path path getting appended to an instantiated artifacts uri. A HTTP POST request to this combined uri returns the
 *             current value of the property. E.g. ../artifacts/myArtifactId/myProp
 *             If not given, 'property/classMemberName' is used.
 *
 * @param name name representing the property in the generated TD. Replaced by the class members identifier if not given.
 *
 *             NOTE: At them moment, only the simple types like int, double, float, string, bool that are present in the json
 *             specification are working. But there's no typechecking happening!
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface ObservableProperty {
  // path to get the value of the observable property`artifacts/{id}/path`
  String path() default "";
  // name of the observable property
  String name() default "";
}
