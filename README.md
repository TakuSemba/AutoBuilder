# AutoBuilder
This is an sample project of Annotation Processing. AutoBuilder generates Buidler of the class you annotated.

## Usage
Add an `@Buildable` annotation to your class that you want to be a builder and `@BuilderField` annotations to your fields.
```java
@Buildable 
class User {

  @BuilderField String name;

  @BuilderField int age;
}
```

You can use a generated builder after compiled.
```java
User user = new UserBuilder().age(19).name("taku").build();
```

## Generated code
```java
// app/build/generated/source/apt/debug/com/takusemba/autobuildersample/UserBuilder.java
public final class UserBuilder {
  private String name;
  private int age;

  public UserBuilder name(String name) {
    this.name = name;
    return this;
  }

  public UserBuilder age(int age) {
    this.age = age;
    return this;
  }

  public User build() {
    User target = new User();
    target.name = this.name;
    target.age = this.age;
    return target;
  }
}
```
