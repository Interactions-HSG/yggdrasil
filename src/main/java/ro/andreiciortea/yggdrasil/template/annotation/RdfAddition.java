package ro.andreiciortea.yggdrasil.template.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation adding additional rdf triples to a artifact templates description at implementation time
 *
 * @param predicates Array of string representing the predicates of the additional triples
 *
 * @param objects Array of strings representing the objects of the addtional triples.
 *
 *             NOTE: The first predicate is mapped to the first object and so on. The length of predicates and objects have to be equal.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface RdfAddition {
  String[] predicates() default {};
  String[] objects() default {};
}
