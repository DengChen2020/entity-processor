package io.github.dengchen2020.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.TYPE;

/**
 * 给列名加表前缀
 * @author dengchen
 */
@Target({TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface ColumnTablePrefix {

}
