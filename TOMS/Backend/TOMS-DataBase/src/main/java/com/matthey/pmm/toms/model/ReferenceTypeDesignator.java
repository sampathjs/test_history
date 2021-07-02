package com.matthey.pmm.toms.model;
import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import com.matthey.pmm.toms.enums.DefaultReferenceType;

/*
 * History:
 * 2018-06-26   V1.0    jwaechter   - Initial Version
 */

/**
 * Designates fields to contain a {@link Reference} of a certain {@link ReferenceTypeTo}. 
 * @author jwaechter
 * @version 1.0
 */
@Retention(RUNTIME)
@Target(FIELD)
public @interface ReferenceTypeDesignator {
  DefaultReferenceType referenceType();
}
