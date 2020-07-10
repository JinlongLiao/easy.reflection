# 介绍
基于[ ronmamo/reflections ](https://github.com/ronmamo/reflections) 改造，
- 整理规范代码
- 重构部分代码结构
- 新增Class 扫描方式

# 使用

将反射添加到您的项目。对于Maven项目，只需添加以下依赖项：
```xml
<dependency>
    <groupId>io.github.jinlongliao.easy.reflection</groupId>
    <artifactId>easy.reflection</artifactId>
    <version>0.9.0</version>
</dependency>
```

# 示例

Java 运行态 注解扫描工具
```java
Reflections reflections = new Reflections("my.project");

Set<Class<? extends SomeType>> subTypes = reflections.getSubTypesOf(SomeType.class);

Set<Class<?>> annotated = reflections.getTypesAnnotatedWith(SomeAnnotation.class);
```

## 用法

## 基本

基本上，要使用Reflections首先使用url和扫描程序实例化它

```java
//scan urls that contain 'my.package', include inputs starting with 'my.package', use the default scanners
Reflections reflections = new Reflections("my.package");

//or using ConfigurationBuilder
new Reflections(new ConfigurationBuilder()
     .setUrls(ClasspathHelper.forPackage("my.project.prefix"))
     .setScanners(new SubTypesScanner(), 
                  new TypeAnnotationsScanner().filterResultsBy(optionalFilter), ...),
     .filterInputsBy(new FilterBuilder().includePackage("my.project.prefix"))
     ...);
```
Then use the convenient query methods: (depending on the scanners configured)

## 子类扫描

```java
//SubTypesScanner
Set<Class<? extends Module>> modules = 
    reflections.getSubTypesOf(com.google.inject.Module.class);
```
## 类扫描

```java
//TypeAnnotationsScanner 
Set<Class<?>> singletons = 
    reflections.getTypesAnnotatedWith(javax.inject.Singleton.class);
```
## 文件资源扫描
```java
//ResourcesScanner
Set<String> properties = 
    reflections.getResources(Pattern.compile(".*\\.properties"));
```

##  方法扫描
```java
//MethodAnnotationsScanner
Set<Method> resources =
    reflections.getMethodsAnnotatedWith(javax.ws.rs.Path.class);
Set<Constructor> injectables = 
    reflections.getConstructorsAnnotatedWith(javax.inject.Inject.class);
```
## 字段扫描

```java
//FieldAnnotationsScanner
Set<Field> ids = 
    reflections.getFieldsAnnotatedWith(javax.persistence.Id.class);
```
## 方法参数类型扫描

```java
//MethodParameterScanner
Set<Method> someMethods =
    reflections.getMethodsMatchParams(long.class, int.class);
Set<Method> voidMethods =
    reflections.getMethodsReturn(void.class);
Set<Method> pathParamMethods =
    reflections.getMethodsWithAnyParamAnnotated(PathParam.class);
```
## 方法属性扫描
```java
//MethodParameterNamesScanner
List<String> parameterNames = 
    reflections.getMethodParamNames(Method.class)
```
## 方法扫描

```java
//MemberUsageScanner
Set<Member> usages = 
    reflections.getMethodUsages(Method.class)
```


# ReflectionUtils
ReflectionsUtils包含一些便捷的Java反射帮助器方法，用于获取与某些谓词匹配的类型/构造函数/方法/字段/注释，通常采用以下形式： *getAllXXX(type, withYYY)

## 示例

```java


Set<Method> getters = getAllMethods(someClass,
  withModifier(Modifier.PUBLIC), withPrefix("get"), withParametersCount(0));

//or
Set<Method> listMethodsFromCollectionToBoolean = 
  getAllMethods(List.class,
    withParametersAssignableTo(Collection.class), withReturnType(boolean.class));

Set<Field> fields = getAllFields(SomeClass.class, withAnnotation(annotation), withTypeAssignableTo(type));
```
