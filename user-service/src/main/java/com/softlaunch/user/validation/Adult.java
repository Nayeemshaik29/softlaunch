package com.softlaunch.user.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = AdultValidator.class)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface Adult {

    String message() default "you must be at least 18 years old";

    int minAge() default 18;

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}