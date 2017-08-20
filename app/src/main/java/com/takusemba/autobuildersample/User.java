package com.takusemba.autobuildersample;

import com.takusemba.autobuilder.Buildable;
import com.takusemba.autobuilder.BuilderField;

/**
 * Created by takusemba on 2017/08/20.
 */
@Buildable
public class User {

  @BuilderField String name;

  @BuilderField int age;
}
