/*
 *  Copyright 2019-2020 author
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package io.github.jinlongliao.easy.reflection;

import io.github.jinlongliao.easy.reflection.exception.ReflectionsException;
import io.github.jinlongliao.easy.reflection.scanners.Scanner;
import io.github.jinlongliao.easy.reflection.serializers.Serializer;
import io.github.jinlongliao.easy.reflection.serializers.impl.JavaCodeSerializer;
import io.github.jinlongliao.easy.reflection.util.*;
import io.github.jinlongliao.easy.reflection.util.*;
import io.github.jinlongliao.easy.reflection.vfs.Vfs;
import io.github.jinlongliao.easy.reflection.serializers.impl.XmlSerializer;
import io.github.jinlongliao.easy.reflection.scanners.impl.FieldAnnotationsScanner;
import io.github.jinlongliao.easy.reflection.scanners.impl.MemberUsageScanner;
import io.github.jinlongliao.easy.reflection.scanners.impl.MethodAnnotationsScanner;
import io.github.jinlongliao.easy.reflection.scanners.impl.MethodParameterNamesScanner;
import io.github.jinlongliao.easy.reflection.scanners.impl.MethodParameterScanner;
import io.github.jinlongliao.easy.reflection.scanners.impl.ResourcesScanner;
import io.github.jinlongliao.easy.reflection.scanners.impl.SubTypesScanner;
import io.github.jinlongliao.easy.reflection.scanners.impl.TypeAnnotationsScanner;
import org.slf4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.Inherited;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static java.lang.String.format;
import static io.github.jinlongliao.easy.reflection.ReflectionUtils.*;
import static io.github.jinlongliao.easy.reflection.util.LogicUtils.*;

/**
 * Reflections one-stop-shop object
 * <p>Reflections scans your classpath, indexes the metadata, allows you to query it on runtime and may save and collect that information for many modules within your project.
 * <p>Using Reflections you can query your metadata such as:
 * <ul>
 *     <li>get all subtypes of some type
 *     <li>get all types/constructors/methods/fields annotated with some annotation, optionally with annotation parameters matching
 *     <li>get all resources matching matching a regular expression
 *     <li>get all methods with specific signature including parameters, parameter annotations and return type
 *     <li>get all methods parameter names
 *     <li>get all fields/methods/constructors usages in code
 * </ul>
 * <p>A typical use of Reflections would be:
 * <pre>
 *      Reflections reflections = new Reflections("my.project.prefix");
 *
 *      Set&lt;Class&lt;? extends SomeType&gt;&gt;  subTypes = reflections.getSubTypesOf(SomeType.class);
 *
 *      Set&lt;Class&lt;?&gt;&gt; annotated = reflections.getTypesAnnotatedWith(SomeAnnotation.class);
 * </pre>
 * <p>Basically, to use Reflections first instantiate it with one of the constructors, then depending on the scanners, use the convenient query methods:
 * <pre>
 *      Reflections reflections = new Reflections("my.package.prefix");
 *      //or
 *      Reflections reflections = new Reflections(ClasspathHelper.forPackage("my.package.prefix"),
 *            new SubTypesScanner(), new TypesAnnotationScanner(), new FilterBuilder().include(...), ...);
 *
 *       //or using the ConfigurationBuilder
 *       new Reflections(new ConfigurationBuilder()
 *            .filterInputsBy(new FilterBuilder().include(FilterBuilder.prefix("my.project.prefix")))
 *            .setUrls(ClasspathHelper.forPackage("my.project.prefix"))
 *            .setScanners(new SubTypesScanner(), new TypeAnnotationsScanner().filterResultsBy(optionalFilter), ...));
 * </pre>
 * And then query, for example:
 * <pre>
 *       Set&lt;Class&lt;? extends Module&gt;&gt; modules = reflections.getSubTypesOf(com.google.inject.Module.class);
 *       Set&lt;Class&lt;?&gt;&gt; singletons =             reflections.getTypesAnnotatedWith(javax.inject.Singleton.class);
 *
 *       Set&lt;String&gt; properties =       reflections.getResources(Pattern.compile(".*\\.properties"));
 *  *       Set&lt;Constructor&gt; injectables = reflections.getConstructorsAnnotatedWith(javax.inject.Inject.class);
 *       Set&lt;Method&gt; deprecateds =      reflections.getMethodsAnnotatedWith(javax.ws.rs.Path.class);
 *       Set&lt;Field&gt; ids =               reflections.getFieldsAnnotatedWith(javax.persistence.Id.class);
 *
 *       Set&lt;Method&gt; someMethods =      reflections.getMethodsMatchParams(long.class, int.class);
 *       Set&lt;Method&gt; voidMethods =      reflections.getMethodsReturn(void.class);
 *       Set&lt;Method&gt; pathParamMethods = reflections.getMethodsWithAnyParamAnnotated(PathParam.class);
 *       Set&lt;Method&gt; floatToString =    reflections.getConverters(Float.class, String.class);
 *       List&lt;String&gt; parameterNames =  reflections.getMethodsParamNames(Method.class);
 *
 *       Set&lt;Member&gt; fieldUsage =       reflections.getFieldUsage(Field.class);
 *       Set&lt;Member&gt; methodUsage =      reflections.getMethodUsage(Method.class);
 *       Set&lt;Member&gt; constructorUsage = reflections.getConstructorUsage(Constructor.class);
 * </pre>
 * <p>You can use other scanners defined in Reflections as well, such as: SubTypesScanner, TypeAnnotationsScanner (both default),
 * ResourcesScanner, MethodAnnotationsScanner, ConstructorAnnotationsScanner, FieldAnnotationsScanner,
 * MethodParameterScanner, MethodParameterNamesScanner, MemberUsageScanner or any custom scanner.
 * <p>Use {@link #getStore()} to access and query the store directly
 * <p>In order to save the store metadata, use {@link #save(String)} or {@link #save(String, Serializer)}
 * for example with {@link XmlSerializer} or {@link JavaCodeSerializer}
 * <p>In order to collect pre saved metadata and avoid re-scanning, use {@link #collect(String, java.util.function.Predicate, Serializer...)}}
 * <p><i>Make sure to scan all the transitively relevant packages.
 * <br>for instance, given your class C extends B extends A, and both B and A are located in another package than C,
 * when only the package of C is scanned - then querying for sub types of A returns nothing (transitive), but querying for sub types of B returns C (direct).
 * In that case make sure to scan all relevant packages a priori.</i>
 * <p><p><p>For Javadoc, source code, and more information about Reflections Library, see http://github.com/ronmamo/reflections/
 */
public class Reflections {
    private static Logger log = LogUtil.findLogger(Reflections.class);
    protected final transient Configuration configuration;
    protected Store store;

    protected Reflections() {
        this(new ConfigurationBuilder(), new Store());
    }

    protected Reflections(Map<String, Map<String, Collection<String>>> storeMap) {
        this.store = new Store();
        store.getStoreMap().putAll(storeMap);
        this.configuration = new ConfigurationBuilder();
        this.afterInit(configuration);
    }

    /**
     * a convenient constructor for scanning within a package prefix.
     * <p>this actually create a {@link Configuration} with:
     * <br> - urls that contain resources with name {@code prefix}
     * <br> - filterInputsBy where name starts with the given {@code prefix}
     * <br> - scanners set to the given {@code scanners}, otherwise defaults to {@link TypeAnnotationsScanner} and {@link SubTypesScanner}.
     *
     * @param prefix   package prefix, to be used with {@link ClasspathHelper#forPackage(String, ClassLoader...)} )}
     * @param scanners optionally supply scanners, otherwise defaults to {@link TypeAnnotationsScanner} and {@link SubTypesScanner}
     */
    public Reflections(final String prefix, final io.github.jinlongliao.easy.reflection.scanners.Scanner... scanners) {
        this((Object) prefix, scanners);
    }

    /**
     * a convenient constructor for Reflections, where given {@code Object...} parameter types can be either:
     * <ul>
     *     <li>{@link String} - would add urls using {@link ClasspathHelper#forPackage(String, ClassLoader...)} ()}</li>
     *     <li>{@link Class} - would add urls using {@link ClasspathHelper#forClass(Class, ClassLoader...)} </li>
     *     <li>{@link ClassLoader} - would use this classloaders in order to find urls in {@link ClasspathHelper#forPackage(String, ClassLoader...)} and {@link ClasspathHelper#forClass(Class, ClassLoader...)}</li>
     *     <li>{@link io.github.jinlongliao.easy.reflection.scanners.Scanner} - would use given scanner, overriding the default scanners</li>
     *     <li>{@link java.net.URL} - would add the given url for scanning</li>
     *     <li>{@link Object[]} - would use each element as above</li>
     * </ul>
     * <p>
     * use any parameter type in any order. this constructor uses instanceof on each param and instantiate a {@link ConfigurationBuilder} appropriately.
     * if you prefer the usual statically typed constructor, don't use this, although it can be very useful.
     *
     * <br><br>for example:
     * <pre>
     *     new Reflections("my.package", classLoader);
     *     //or
     *     new Reflections("my.package", someScanner, anotherScanner, classLoader);
     *     //or
     *     new Reflections(myUrl, myOtherUrl);
     * </pre>
     */
    public Reflections(final Object... params) {
        this(ConfigurationBuilder.build(params));
    }

    /**
     * constructs a Reflections instance and scan according to given {@link Configuration}
     * <p>it is preferred to use {@link ConfigurationBuilder}
     */
    public Reflections(final Configuration configuration) {
        this(configuration, new Store());
    }


    protected Reflections(Configuration configuration, Store store) {
        this.store = store;
        this.configuration = configuration;
        this.afterInit(configuration);
    }

    private void afterInit(Configuration configuration) {
        if (configuration.getScanners() != null && !configuration.getScanners().isEmpty()) {
            //inject to scanners
            configuration.getScanners().forEach(scanner -> {
                scanner.setConfiguration(configuration);
            });
            this.scan();
            if (configuration.shouldExpandSuperTypes()) {
                expandSuperTypes();
            }
        }
    }

    /**
     * 依据配置信息 ，扫描元数据
     */
    protected void scan() {
        if (configuration.getUrls() == null || configuration.getUrls().isEmpty()) {
            log.error("given scan urls are empty. set urls in the configuration");
            return;
        }
        log.debug("going to scan these urls: {}", configuration.getUrls());
        long time = System.currentTimeMillis();
        int scannedUrls = 0;
        ExecutorService executorService = configuration.getExecutorService();
        List<Future<?>> futures = new ArrayList<>();
        for (final URL url : configuration.getUrls()) {
            try {
                if (executorService != null) {
                    futures.add(executorService.submit(() -> {
                        log.debug("[{}] scanning {}", Thread.currentThread().toString(), url);
                        scan(url);
                    }));
                } else {
                    scan(url);
                }
                scannedUrls++;
            } catch (ReflectionsException e) {
                log.warn("could not create Vfs.Dir from url. ignoring the exception and continuing", e);
            }
        }

        //todo use CompletionService
        if (executorService != null) {
            for (Future future : futures) {
                try {
                    future.get();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }
        }

        //gracefully shutdown the parallel scanner executor service.
        if (executorService != null) {
            executorService.shutdown();
        }

        log.info(format("Reflections took %d ms to scan %d urls, producing %s %s",
                System.currentTimeMillis() - time, scannedUrls, producingDescription(store),
                executorService instanceof ThreadPoolExecutor ?
                        format("[using %d cores]", ((ThreadPoolExecutor) executorService).getMaximumPoolSize()) : ""));
    }


    private static String producingDescription(Store store) {
        int keys = 0;
        int values = 0;
        for (String index : store.keySet()) {
            keys += store.keys(index).size();
            values += store.values(index).size();
        }
        return String.format("%d keys and %d values", keys, values);
    }

    protected void scan(URL url) {
        Vfs.Dir dir = Vfs.fromURL(url);
        try {
            for (final Vfs.File file : dir.getFiles()) {
                // scan if inputs filter accepts file relative path or fqn
                Predicate<String> inputsFilter = configuration.getInputsFilter();
                String path = file.getRelativePath();
                String fqn = FileUtils.systemPath2ClassPath(path);
                if (inputsFilter == null || inputsFilter.test(path) || inputsFilter.test(fqn)) {
                    Object classObject = null;
                    for (Scanner scanner : configuration.getScanners()) {
                        try {
                            if (scanner.support(path) || scanner.support(fqn)) {
                                classObject = scanner.scan(file, classObject, store);
                            }
                        } catch (Exception e) {
                            // SLF4J will filter out Throwables from the format string arguments.
                            log.debug("could not scan file {} in url {} with scanner {}", file.getRelativePath(), url.toExternalForm(), scanner.getClass().getSimpleName(), e);
                        }

                    }
                }
            }
        } finally {
            try {
                dir.close();
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
        }
    }

    /**
     * collect saved Reflection xml resources and merge it into a Reflections instance
     * <p>by default, resources are collected from all urls that contains the package META-INF/reflections
     * and includes files matching the pattern .*-reflections.xml
     */
    public static Reflections collect() {
        return collect("META-INF/reflections/", new ScannerFilter().include(".*-reflections.xml"));
    }

    /**
     * collect saved Reflections resources from all urls that contains the given packagePrefix and matches the given resourceNameFilter
     * and de-serializes them using the default serializer {@link XmlSerializer} or using the optionally supplied optionalSerializer
     * <p>
     * it is preferred to use a designated resource prefix (for example META-INF/reflections but not just META-INF),
     * so that relevant urls could be found much faster
     *
     * @param optionalSerializer - optionally supply one serializer instance. if not specified or null, {@link XmlSerializer} will be used
     * @return / /
     */
    public static Reflections collect(final String packagePrefix, final Predicate<String> resourceNameFilter, Serializer... optionalSerializer) {
        Serializer serializer = optionalSerializer != null && optionalSerializer.length == 1 ? optionalSerializer[0] : new XmlSerializer();

        Collection<URL> urls = ClasspathHelper.forPackage(packagePrefix);
        if (urls.isEmpty()) {
            return null;
        }
        long start = System.currentTimeMillis();
        final Reflections reflections = new Reflections();
        Iterable<Vfs.File> files = Vfs.findFiles(urls, packagePrefix, resourceNameFilter);
        for (final Vfs.File file : files) {
            try (InputStream inputStream = file.openInputStream();) {
                reflections.merge(serializer.read(inputStream));
            } catch (IOException e) {
                throw new ReflectionsException("could not merge " + file, e);
            }
        }


        log.info(format("Reflections took %d ms to collect %d url, producing %s",
                System.currentTimeMillis() - start, urls.size(), producingDescription(reflections.store)));

        return reflections;
    }

    /**
     * merges saved Reflections resources from the given input stream, using the serializer configured in this instance's Configuration
     * <br> useful if you know the serialized resource location and prefer not to look it up the classpath
     */
    public Reflections collect(final InputStream inputStream) {
        try {
            merge(configuration.getSerializer().read(inputStream));
            log.info("Reflections collected metadata from input stream using serializer " + configuration.getSerializer().getClass().getName());

        } catch (Exception ex) {
            throw new ReflectionsException("could not merge input stream", ex);
        }
        return this;
    }

    /**
     * merges saved Reflections resources from the given file, using the serializer configured in this instance's Configuration
     * <p> useful if you know the serialized resource location and prefer not to look it up the classpath
     */
    public Reflections collect(final File file) {
        try (InputStream inputStream = new FileInputStream(file)) {
            return collect(inputStream);
        } catch (IOException e) {
            throw new ReflectionsException("could not obtain input stream from file " + file, e);
        }
    }

    /**
     * merges a Reflections instance metadata into this instance
     */
    public Reflections merge(final Reflections reflections) {
        store.merge(reflections.store);
        return this;
    }

    /**
     * expand super types after scanning, for super types that were not scanned.
     * this is helpful in finding the transitive closure without scanning all 3rd party dependencies.
     * it uses {@link ReflectionUtils#getSuperTypes(Class)}.
     * <p>
     * for example, for classes A,B,C where A supertype of B, B supertype of C:
     * <ul>
     *     <li>if scanning C resulted in B (B->C in store), but A was not scanned (although A supertype of B) - then getSubTypes(A) will not return C</li>
     *     <li>if expanding supertypes, B will be expanded with A (A->B in store) - then getSubTypes(A) will return C</li>
     * </ul>
     */
    public void expandSuperTypes() {
        String index = ClassUtils.getClassSimpleName(SubTypesScanner.class);
        Set<String> keys = store.keys(index);
        keys.removeAll(store.values(index));
        for (String key : keys) {
            final Class<?> type = ReflectionUtils.forName(key, loaders());
            if (type != null) {
                expandSupertypes(store, key, type);
            }
        }
    }

    private void expandSupertypes(Store store, String key, Class<?> type) {
        for (Class<?> supertype : ReflectionUtils.getSuperTypes(type)) {
            if (store.put(SubTypesScanner.class, supertype.getName(), key)) {
                log.debug("expanded subtype {} -> {}", supertype.getName(), key);
                expandSupertypes(store, supertype.getName(), supertype);
            }
        }
    }

    //query

    /**
     * gets all sub types in hierarchy of a given type
     * <p/>depends on SubTypesScanner configured
     */
    public <T> Set<Class<? extends T>> getSubTypesOf(final Class<T> type) {
        return ReflectionUtils.forNames(store.getAll(SubTypesScanner.class, type.getName()), loaders());
    }

    /**
     * get types annotated with a given annotation, both classes and annotations
     * <p>{@link java.lang.annotation.Inherited} is not honored by default.
     * <p>when honoring @Inherited, meta-annotation should only effect annotated super classes and its sub types
     * <p><i>Note that this (@Inherited) meta-annotation type has no effect if the annotated type is used for anything other then a class.
     * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i>
     * <p/>depends on TypeAnnotationsScanner and SubTypesScanner configured
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation) {
        return getTypesAnnotatedWith(annotation, false);
    }

    /**
     * get types annotated with a given annotation, both classes and annotations
     * <p>{@link java.lang.annotation.Inherited} is honored according to given honorInherited.
     * <p>when honoring @Inherited, meta-annotation should only effect annotated super classes and it's sub types
     * <p>when not honoring @Inherited, meta annotation effects all subtypes, including annotations interfaces and classes
     * <p><i>Note that this (@Inherited) meta-annotation type has no effect if the annotated type is used for anything other then a class.
     * Also, this meta-annotation causes annotations to be inherited only from superclasses; annotations on implemented interfaces have no effect.</i>
     * <p/>depends on TypeAnnotationsScanner and SubTypesScanner configured
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Class<? extends Annotation> annotation, boolean honorInherited) {
        Set<String> annotated = store.get(TypeAnnotationsScanner.class, annotation.getName());
        annotated.addAll(getAllAnnotated(annotated, annotation, honorInherited));
        return ReflectionUtils.forNames(annotated, loaders());
    }

    /**
     * get types annotated with a given annotation, both classes and annotations, including annotation member values matching
     * <p>{@link java.lang.annotation.Inherited} is not honored by default
     * <p/>depends on TypeAnnotationsScanner configured
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation) {
        return getTypesAnnotatedWith(annotation, false);
    }

    /**
     * get types annotated with a given annotation, both classes and annotations, including annotation member values matching
     * <p>{@link java.lang.annotation.Inherited} is honored according to given honorInherited
     * <p/>depends on TypeAnnotationsScanner configured
     */
    public Set<Class<?>> getTypesAnnotatedWith(final Annotation annotation, boolean honorInherited) {
        Set<String> annotated = store.get(TypeAnnotationsScanner.class, annotation.annotationType().getName());
        Set<Class<?>>
                allAnnotated = LogicUtils.filter(ReflectionUtils.forNames(annotated, loaders()), ReflectionUtils.withAnnotation(annotation));
        Set<Class<?>>
                classes = ReflectionUtils.forNames(LogicUtils.filter(getAllAnnotated(ClassUtils.getClassNames(allAnnotated), annotation.annotationType(), honorInherited), s -> !annotated.contains(s)), loaders());
        allAnnotated.addAll(classes);
        return allAnnotated;
    }

    protected Collection<String> getAllAnnotated(Collection<String> annotated, Class<? extends Annotation> annotation, boolean honorInherited) {
        if (honorInherited) {
            if (annotation.isAnnotationPresent(Inherited.class)) {
                Set<String> subTypes = store.get(SubTypesScanner.class, LogicUtils.filter(annotated, input -> {
                    final Class<?> type = forName(input, loaders());
                    return type != null && !type.isInterface();
                }));
                return store.getAllIncluding(SubTypesScanner.class, subTypes);
            } else {
                return annotated;
            }
        } else {
            Collection<String> subTypes = store.getAllIncluding(TypeAnnotationsScanner.class, annotated);
            return store.getAllIncluding(SubTypesScanner.class, subTypes);
        }
    }

    /**
     * get all methods annotated with a given annotation
     * <p/>depends on MethodAnnotationsScanner configured
     */
    public Set<Method> getMethodsAnnotatedWith(final Class<? extends Annotation> annotation) {
        return ClassUtils.getMethodsFromDescriptors(store.get(MethodAnnotationsScanner.class, annotation.getName()), loaders());
    }

    /**
     * get all methods annotated with a given annotation, including annotation member values matching
     * <p/>depends on MethodAnnotationsScanner configured
     */
    public Set<Method> getMethodsAnnotatedWith(final Annotation annotation) {
        return LogicUtils.filter(getMethodsAnnotatedWith(annotation.annotationType()), ReflectionUtils.withAnnotation(annotation));
    }

    /**
     * get methods with parameter types matching given {@code types}
     */
    public Set<Method> getMethodsMatchParams(Class<?>... types) {
        return ClassUtils.getMethodsFromDescriptors(store.get(MethodParameterScanner.class, ClassUtils.getClassNames(types).toString()), loaders());
    }

    /**
     * get methods with return type match given type
     */
    public Set<Method> getMethodsReturn(Class returnType) {
        return ClassUtils.getMethodsFromDescriptors(store.get(MethodParameterScanner.class, ClassUtils.getClassNames(returnType)), loaders());
    }

    /**
     * get methods with any parameter annotated with given annotation
     */
    public Set<Method> getMethodsWithAnyParamAnnotated(Class<? extends Annotation> annotation) {
        return ClassUtils.getMethodsFromDescriptors(store.get(MethodParameterScanner.class, annotation.getName()), loaders());

    }

    /**
     * get methods with any parameter annotated with given annotation, including annotation member values matching
     */
    public Set<Method> getMethodsWithAnyParamAnnotated(Annotation annotation) {
        return LogicUtils.filter(getMethodsWithAnyParamAnnotated(annotation.annotationType()), ReflectionUtils.withAnyParameterAnnotation(annotation));
    }

    /**
     * get all constructors annotated with a given annotation
     * <p/>depends on MethodAnnotationsScanner configured
     */
    public Set<Constructor> getConstructorsAnnotatedWith(final Class<? extends Annotation> annotation) {
        return ClassUtils.getConstructorsFromDescriptors(store.get(MethodAnnotationsScanner.class, annotation.getName()), loaders());
    }

    /**
     * get all constructors annotated with a given annotation, including annotation member values matching
     * <p/>depends on MethodAnnotationsScanner configured
     */
    public Set<Constructor> getConstructorsAnnotatedWith(final Annotation annotation) {
        return LogicUtils.filter(getConstructorsAnnotatedWith(annotation.annotationType()), ReflectionUtils.withAnnotation(annotation));
    }

    /**
     * get constructors with parameter types matching given {@code types}
     */
    public Set<Constructor> getConstructorsMatchParams(Class<?>... types) {
        return ClassUtils.getConstructorsFromDescriptors(store.get(MethodParameterScanner.class, ClassUtils.getClassNames(types).toString()), loaders());
    }

    /**
     * get constructors with any parameter annotated with given annotation
     */
    public Set<Constructor> getConstructorsWithAnyParamAnnotated(Class<? extends Annotation> annotation) {
        return ClassUtils.getConstructorsFromDescriptors(store.get(MethodParameterScanner.class, annotation.getName()), loaders());
    }

    /**
     * get constructors with any parameter annotated with given annotation, including annotation member values matching
     */
    public Set<Constructor> getConstructorsWithAnyParamAnnotated(Annotation annotation) {
        return LogicUtils.filter(getConstructorsWithAnyParamAnnotated(annotation.annotationType()), ReflectionUtils.withAnyParameterAnnotation(annotation));
    }

    /**
     * get all fields annotated with a given annotation
     * <p/>depends on FieldAnnotationsScanner configured
     */
    public Set<Field> getFieldsAnnotatedWith(final Class<? extends Annotation> annotation) {
        return store.get(FieldAnnotationsScanner.class, annotation.getName()).stream()
                .map(annotated -> ClassUtils.getFieldFromString(annotated, loaders()))
                .collect(Collectors.toSet());
    }

    /**
     * get all methods annotated with a given annotation, including annotation member values matching
     * <p/>depends on FieldAnnotationsScanner configured
     */
    public Set<Field> getFieldsAnnotatedWith(final Annotation annotation) {
        return LogicUtils.filter(getFieldsAnnotatedWith(annotation.annotationType()), ReflectionUtils.withAnnotation(annotation));
    }

    /**
     * get resources relative paths where simple name (key) matches given namePredicate
     * <p>depends on ResourcesScanner configured
     */
    public Set<String> getResources(final Predicate<String> namePredicate) {
        Set<String> resources = filter(store.keys(ClassUtils.getClassSimpleName(ResourcesScanner.class)), namePredicate);
        return store.get(ResourcesScanner.class, resources);
    }

    /**
     * get resources relative paths where simple name (key) matches given regular expression
     * <p>depends on ResourcesScanner configured
     * <pre>Set<String> xmls = reflections.getResources(".*\\.xml");</pre>
     */
    public Set<String> getResources(final Pattern pattern) {
        return getResources(input -> pattern.matcher(input).matches());
    }

    /**
     * get parameter names of given {@code method}
     * <p>depends on MethodParameterNamesScanner configured
     */
    public List<String> getMethodParamNames(Method method) {
        Set<String> names = store.get(MethodParameterNamesScanner.class, ClassUtils.getConstructorName(method));
        return names.size() == 1 ? Arrays.asList(names.iterator().next().split(", ")) : Collections.emptyList();
    }

    /**
     * get parameter names of given {@code constructor}
     * <p>depends on MethodParameterNamesScanner configured
     */
    public List<String> getConstructorParamNames(Constructor constructor) {
        Set<String> names = store.get(MethodParameterNamesScanner.class, ClassUtils.getConstructorName(constructor));
        return names.size() == 1 ? Arrays.asList(names.iterator().next().split(", ")) : Collections.emptyList();
    }

    /**
     * get all given {@code field} usages in methods and constructors
     * <p>depends on MemberUsageScanner configured
     */
    public Set<Member> getFieldUsage(Field field) {
        return ClassUtils.getMembersFromDescriptors(store.get(MemberUsageScanner.class, ClassUtils.getFieldName(field)));
    }

    /**
     * get all given {@code method} usages in methods and constructors
     * <p>depends on MemberUsageScanner configured
     */
    public Set<Member> getMethodUsage(Method method) {
        return ClassUtils.getMembersFromDescriptors(store.get(MemberUsageScanner.class, ClassUtils.getConstructorName(method)));
    }

    /**
     * get all given {@code constructors} usages in methods and constructors
     * <p>depends on MemberUsageScanner configured
     */
    public Set<Member> getConstructorUsage(Constructor constructor) {
        return ClassUtils.getMembersFromDescriptors(store.get(MemberUsageScanner.class, ClassUtils.getConstructorName(constructor)));
    }

    /**
     * get all types scanned. this is effectively similar to getting all subtypes of Object.
     * <p>depends on SubTypesScanner configured with {@code SubTypesScanner(false)}, otherwise {@code ReflectionsException} is thrown
     * <p><i>note using this might be a bad practice. it is better to get types matching some criteria,
     * such as {@link #getSubTypesOf(Class)} or {@link #getTypesAnnotatedWith(Class)}</i>
     *
     * @return / Set of String, and not of Class, in order to avoid definition of all types in PermGen
     */
    public Set<String> getAllTypes() {
        Set<String> allTypes = new HashSet<>(store.getAll(SubTypesScanner.class, Object.class.getName()));
        if (allTypes.isEmpty()) {
            throw new ReflectionsException("Couldn't find subtypes of Object. " +
                    "Make sure SubTypesScanner initialized to include Object class - new SubTypesScanner(false)");
        }
        return allTypes;
    }

    /**
     * returns the {@link Store} used for storing and querying the metadata
     */
    public Store getStore() {
        return store;
    }

    /**
     * returns the {@link Configuration} object of this instance
     */
    public Configuration getConfiguration() {
        return configuration;
    }

    /**
     * serialize to a given directory and filename
     * <p>* it is preferred to specify a designated directory (for example META-INF/reflections),
     * so that it could be found later much faster using the load method
     * <p>see the documentation for the save method on the configured {@link Serializer}
     */
    public File save(final String filename) {
        return save(filename, configuration.getSerializer());
    }

    /**
     * serialize to a given directory and filename using given serializer
     * <p>* it is preferred to specify a designated directory (for example META-INF/reflections),
     * so that it could be found later much faster using the load method
     */
    public File save(final String filename, final Serializer serializer) {
        File file = serializer.save(this, filename);
        log.info("Reflections successfully saved in " + file.getAbsolutePath() + " using " + serializer.getClass().getSimpleName());
        return file;
    }

    private ClassLoader[] loaders() {
        return configuration.getClassLoaders();
    }
}
