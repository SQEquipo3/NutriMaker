package com.javafx.nutrimaker.qa;

import java.lang.annotation.*;

/** Liga una prueba JUnit con su caso de prueba y su requisito funcional. */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface TC {
    String id();
    String rf() default "";
    String modulo() default "";
}
