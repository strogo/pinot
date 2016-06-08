package com.linkedin.pinot.common.restlet.swagger;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;


/**
 * An example of a value, used to document POJOs.
 */
@Retention(RetentionPolicy.RUNTIME)
public @interface Example {
  /**
   * The contents of the example, usually a string containing JSON.
   */
  String value();
}
