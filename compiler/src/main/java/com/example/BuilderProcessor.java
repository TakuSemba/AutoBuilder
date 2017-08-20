package com.example;

import com.google.auto.service.AutoService;
import com.squareup.javapoet.ClassName;
import com.squareup.javapoet.FieldSpec;
import com.squareup.javapoet.JavaFile;
import com.squareup.javapoet.MethodSpec;
import com.squareup.javapoet.TypeName;
import com.squareup.javapoet.TypeSpec;
import com.takusemba.autobuilder.Buildable;
import com.takusemba.autobuilder.BuilderField;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.Filer;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.Diagnostic;

@AutoService(Processor.class)
@SupportedAnnotationTypes({
    "com.takusemba.autobuilder.Buildable", "com.takusemba.autobuilder.BuilderField"
})
@SupportedSourceVersion(SourceVersion.RELEASE_7)
public class BuilderProcessor extends AbstractProcessor {

  private Filer filer;
  private Messager messager;
  private Elements elements;

  @Override public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.messager = processingEnv.getMessager();
    this.filer = processingEnv.getFiler();
    this.elements = processingEnv.getElementUtils();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    for (Element element : roundEnv.getElementsAnnotatedWith(Buildable.class)) {
      String packageName = elements.getPackageOf(element).getQualifiedName().toString();
      ClassName builderClass =
          ClassName.get(packageName, String.format("%sBuilder", element.getSimpleName()));
      List<FieldSpec> fieldSpecs = new ArrayList<>();
      List<MethodSpec> setterSpecs = new ArrayList<>();

      Class<BuilderField> builderFieldClass = BuilderField.class;
      for (Element el : element.getEnclosedElements()) {
        if (el.getAnnotation(builderFieldClass) != null) {
          if (el.getModifiers().contains(Modifier.PRIVATE)) {
            messager.printMessage(Diagnostic.Kind.ERROR,
                BuilderField.class.getName() + " cannot be added to a private field");
          }

          TypeName typeName = TypeName.get(el.asType());

          String name = el.getSimpleName().toString();

          fieldSpecs.add(FieldSpec.builder(typeName, name, Modifier.PRIVATE).build());

          setterSpecs.add(MethodSpec.methodBuilder(name)
              .addModifiers(Modifier.PUBLIC)
              .returns(builderClass)
              .addParameter(typeName, name)
              .addStatement("this.$N = $N", name, name)
              .addStatement("return this")
              .build());
        }
      }

      TypeName targetType = TypeName.get(element.asType());
      String targetName = "target";
      MethodSpec.Builder builderMethodBuilder = MethodSpec.methodBuilder("build")
          .addModifiers(Modifier.PUBLIC)
          .returns(targetType)
          .addStatement("$1T $2N = new $1T()", targetType, targetName);
      for (FieldSpec fieldSpec : fieldSpecs) {
        builderMethodBuilder.addStatement("$1N.$2N = this.$2N", targetName, fieldSpec);
      }
      builderMethodBuilder.addStatement("return $N", targetName);
      MethodSpec buildMethod = builderMethodBuilder.build();

      TypeSpec classSpec = TypeSpec.classBuilder(builderClass.simpleName())
          .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
          .addFields(fieldSpecs)
          .addMethods(setterSpecs)
          .addMethod(buildMethod)
          .build();

      JavaFile file = JavaFile.builder(builderClass.packageName(), classSpec).build();

      try {
        file.writeTo(filer);
      } catch (IOException e) {
        messager.printMessage(Diagnostic.Kind.ERROR,
            "Failed to create file " + builderClass.packageName());
      }
    }
    return true;
  }
}
