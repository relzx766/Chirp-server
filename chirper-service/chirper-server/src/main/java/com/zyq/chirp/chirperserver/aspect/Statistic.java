package com.zyq.chirp.chirperserver.aspect;

import com.zyq.chirp.chirperserver.domain.enums.CacheKey;
import org.intellij.lang.annotations.Language;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Statistic {
    @Language("SpEL")
    String id();

    int delta() default 1;

    CacheKey[] key();
}
