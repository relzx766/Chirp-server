package com.zyq.chirp.chirperserver.aspect;

import org.intellij.lang.annotations.Language;

import java.lang.annotation.*;

/**
 * 检测推文中被@的用户,提及的标签
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
@Documented
public @interface ParseMentioned {
    @Language("SpEL")
    String value() default "";
}
