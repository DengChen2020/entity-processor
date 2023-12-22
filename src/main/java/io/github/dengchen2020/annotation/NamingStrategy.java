package io.github.dengchen2020.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.*;

/**
 * 列的命名策略
 * @author dengchen
 */
@Target({TYPE})
@Retention(RetentionPolicy.CLASS)
public @interface NamingStrategy {

    /**
     * 蛇形（小写下划线）
     */
    public static final String SNAKE_CASE = "snakeCase";

    /**
     * 帕斯卡（首字母大写）
     */
    public static final String PASCAL_CASE = "pascalCase";

    /**
     * 不做处理
     */
    public static final String NONE = "none";

    String value() default "snakeCase";

}
