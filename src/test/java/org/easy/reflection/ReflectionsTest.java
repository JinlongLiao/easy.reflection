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

package org.easy.reflection;

import org.easy.reflection.exception.ReflectionsException;
import org.easy.reflection.util.ClassUtils;
import org.easy.reflection.util.ClasspathHelper;
import org.easy.reflection.util.ConfigurationBuilder;
import org.easy.reflection.util.ScannerFilter;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.BeforeClass;
import org.junit.Test;
import org.easy.reflection.scanners.impl.FieldAnnotationsScanner;
import org.easy.reflection.scanners.impl.MemberUsageScanner;
import org.easy.reflection.scanners.impl.MethodAnnotationsScanner;
import org.easy.reflection.scanners.impl.MethodParameterNamesScanner;
import org.easy.reflection.scanners.impl.MethodParameterScanner;
import org.easy.reflection.scanners.impl.ResourcesScanner;
import org.easy.reflection.scanners.impl.SubTypesScanner;
import org.easy.reflection.scanners.impl.TypeAnnotationsScanner;

import java.io.File;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 *
 */
public class ReflectionsTest {
    public static final ScannerFilter TestModelFilter = new ScannerFilter().include("org.easy.reflection.TestModel\\$.*");
    static Reflections reflections;

    @BeforeClass
    public static void init() {
        reflections = new Reflections(new ConfigurationBuilder()
                .setUrls(Collections.singletonList(ClasspathHelper.forClass(TestModel.class)))
                .filterInputsBy(TestModelFilter)
                .setScanners(
                        new SubTypesScanner(false),
                        new TypeAnnotationsScanner(),
                        new FieldAnnotationsScanner(),
                        new MethodAnnotationsScanner(),
                        new MethodParameterScanner(),
                        new MethodParameterNamesScanner(),
                        new MemberUsageScanner()));
    }

    @Test
    public void testSubTypesOf() {
        org.hamcrest.MatcherAssert.assertThat(reflections.getSubTypesOf(TestModel.I1.class), are(TestModel.I2.class, TestModel.C1.class, TestModel.C2.class, TestModel.C3.class, TestModel.C5.class));
        org.hamcrest.MatcherAssert.assertThat(reflections.getSubTypesOf(TestModel.C1.class), are(TestModel.C2.class, TestModel.C3.class, TestModel.C5.class));

        assertFalse("getAllTypes should not be empty when Reflections is configured with SubTypesScanner(false)",
                reflections.getAllTypes().isEmpty());
    }

    @Test
    public void testTypesAnnotatedWith() {
        org.hamcrest.MatcherAssert.assertThat(reflections.getTypesAnnotatedWith(TestModel.MAI1.class, true), are(TestModel.AI1.class));
        org.hamcrest.MatcherAssert.assertThat(reflections.getTypesAnnotatedWith(TestModel.MAI1.class, true), annotatedWith(TestModel.MAI1.class));

        org.hamcrest.MatcherAssert.assertThat(reflections.getTypesAnnotatedWith(TestModel.AI2.class, true), are(TestModel.I2.class));
        org.hamcrest.MatcherAssert.assertThat(reflections.getTypesAnnotatedWith(TestModel.AI2.class, true), annotatedWith(TestModel.AI2.class));

        org.hamcrest.MatcherAssert.assertThat(reflections.getTypesAnnotatedWith(TestModel.AC1.class, true), are(TestModel.C1.class, TestModel.C2.class, TestModel.C3.class, TestModel.C5.class));
        org.hamcrest.MatcherAssert.assertThat(reflections.getTypesAnnotatedWith(TestModel.AC1.class, true), annotatedWith(TestModel.AC1.class));

        org.hamcrest.MatcherAssert.assertThat(reflections.getTypesAnnotatedWith(TestModel.AC1n.class, true), are(TestModel.C1.class));
        org.hamcrest.MatcherAssert.assertThat(reflections.getTypesAnnotatedWith(TestModel.AC1n.class, true), annotatedWith(TestModel.AC1n.class));

        org.hamcrest.MatcherAssert.assertThat(reflections.getTypesAnnotatedWith(TestModel.MAI1.class), are(TestModel.AI1.class, TestModel.I1.class, TestModel.I2.class, TestModel.C1.class, TestModel.C2.class, TestModel.C3.class, TestModel.C5.class));
        org.hamcrest.MatcherAssert.assertThat(reflections.getTypesAnnotatedWith(TestModel.MAI1.class), metaAnnotatedWith(TestModel.MAI1.class));

        org.hamcrest.MatcherAssert.assertThat(reflections.getTypesAnnotatedWith(TestModel.AI1.class), are(TestModel.I1.class, TestModel.I2.class, TestModel.C1.class, TestModel.C2.class, TestModel.C3.class, TestModel.C5.class));
        org.hamcrest.MatcherAssert.assertThat(reflections.getTypesAnnotatedWith(TestModel.AI1.class), metaAnnotatedWith(TestModel.AI1.class));

        org.hamcrest.MatcherAssert.assertThat(reflections.getTypesAnnotatedWith(TestModel.AI2.class), are(TestModel.I2.class, TestModel.C1.class, TestModel.C2.class, TestModel.C3.class, TestModel.C5.class));
        org.hamcrest.MatcherAssert.assertThat(reflections.getTypesAnnotatedWith(TestModel.AI2.class), metaAnnotatedWith(TestModel.AI2.class));

        org.hamcrest.MatcherAssert.assertThat(reflections.getTypesAnnotatedWith(TestModel.AM1.class), isEmpty);

        //annotation member value matching
        TestModel.AC2 ac2 = new TestModel.AC2() {
            public String value() {
                return "ugh?!";
            }

            public Class<? extends Annotation> annotationType() {
                return TestModel.AC2.class;
            }
        };

        org.hamcrest.MatcherAssert.assertThat(reflections.getTypesAnnotatedWith(ac2), are(TestModel.C3.class, TestModel.C5.class, TestModel.I3.class, TestModel.C6.class, TestModel.AC3.class, TestModel.C7.class));

        org.hamcrest.MatcherAssert.assertThat(reflections.getTypesAnnotatedWith(ac2, true), are(TestModel.C3.class, TestModel.I3.class, TestModel.AC3.class));
    }

    @Test
    public void testMethodsAnnotatedWith() {
        try {
            org.hamcrest.MatcherAssert.assertThat(reflections.getMethodsAnnotatedWith(TestModel.AM1.class),
                    are(TestModel.C4.class.getDeclaredMethod("m1"),
                            TestModel.C4.class.getDeclaredMethod("m1", int.class, String[].class),
                            TestModel.C4.class.getDeclaredMethod("m1", int[][].class, String[][].class),
                            TestModel.C4.class.getDeclaredMethod("m3")));

            TestModel.AM1 am1 = new TestModel.AM1() {
                public String value() {
                    return "1";
                }

                public Class<? extends Annotation> annotationType() {
                    return TestModel.AM1.class;
                }
            };
            org.hamcrest.MatcherAssert.assertThat(reflections.getMethodsAnnotatedWith(am1),
                    are(TestModel.C4.class.getDeclaredMethod("m1"),
                            TestModel.C4.class.getDeclaredMethod("m1", int.class, String[].class),
                            TestModel.C4.class.getDeclaredMethod("m1", int[][].class, String[][].class)));
        } catch (NoSuchMethodException e) {
            fail();
        }
    }

    @Test
    public void testConstructorsAnnotatedWith() {
        try {
            org.hamcrest.MatcherAssert.assertThat(reflections.getConstructorsAnnotatedWith(TestModel.AM1.class),
                    are(TestModel.C4.class.getDeclaredConstructor(String.class)));

            TestModel.AM1 am1 = new TestModel.AM1() {
                public String value() {
                    return "1";
                }

                public Class<? extends Annotation> annotationType() {
                    return TestModel.AM1.class;
                }
            };
            org.hamcrest.MatcherAssert.assertThat(reflections.getConstructorsAnnotatedWith(am1),
                    are(TestModel.C4.class.getDeclaredConstructor(String.class)));
        } catch (NoSuchMethodException e) {
            fail();
        }
    }

    @Test
    public void testFieldsAnnotatedWith() {
        try {
            org.hamcrest.MatcherAssert.assertThat(reflections.getFieldsAnnotatedWith(TestModel.AF1.class),
                    are(TestModel.C4.class.getDeclaredField("f1"),
                            TestModel.C4.class.getDeclaredField("f2")
                    ));

            org.hamcrest.MatcherAssert.assertThat(reflections.getFieldsAnnotatedWith(new TestModel.AF1() {
                        public String value() {
                            return "2";
                        }

                        public Class<? extends Annotation> annotationType() {
                            return TestModel.AF1.class;
                        }
                    }),
                    are(TestModel.C4.class.getDeclaredField("f2")));
        } catch (NoSuchFieldException e) {
            fail();
        }
    }

    @Test
    public void testMethodParameter() {
        try {
            org.hamcrest.MatcherAssert.assertThat(reflections.getMethodsMatchParams(String.class),
                    are(TestModel.C4.class.getDeclaredMethod("m4", String.class), TestModel.Usage.C1.class.getDeclaredMethod("method", String.class)));

            org.hamcrest.MatcherAssert.assertThat(reflections.getMethodsMatchParams(),
                    are(TestModel.C4.class.getDeclaredMethod("m1"), TestModel.C4.class.getDeclaredMethod("m3"),
                            TestModel.AC2.class.getMethod("value"), TestModel.AF1.class.getMethod("value"), TestModel.AM1.class.getMethod("value"),
                            TestModel.Usage.C1.class.getDeclaredMethod("method"), TestModel.Usage.C2.class.getDeclaredMethod("method")));

            org.hamcrest.MatcherAssert.assertThat(reflections.getMethodsMatchParams(int[][].class, String[][].class),
                    are(TestModel.C4.class.getDeclaredMethod("m1", int[][].class, String[][].class)));

            org.hamcrest.MatcherAssert.assertThat(reflections.getMethodsReturn(int.class),
                    are(TestModel.C4.class.getDeclaredMethod("add", int.class, int.class)));

            org.hamcrest.MatcherAssert.assertThat(reflections.getMethodsReturn(String.class),
                    are(TestModel.C4.class.getDeclaredMethod("m3"), TestModel.C4.class.getDeclaredMethod("m4", String.class),
                            TestModel.AC2.class.getMethod("value"), TestModel.AF1.class.getMethod("value"), TestModel.AM1.class.getMethod("value")));

            org.hamcrest.MatcherAssert.assertThat(reflections.getMethodsReturn(void.class),
                    are(TestModel.C4.class.getDeclaredMethod("m1"), TestModel.C4.class.getDeclaredMethod("m1", int.class, String[].class),
                            TestModel.C4.class.getDeclaredMethod("m1", int[][].class, String[][].class), TestModel.Usage.C1.class.getDeclaredMethod("method"),
                            TestModel.Usage.C1.class.getDeclaredMethod("method", String.class), TestModel.Usage.C2.class.getDeclaredMethod("method")));

            org.hamcrest.MatcherAssert.assertThat(reflections.getMethodsWithAnyParamAnnotated(TestModel.AM1.class),
                    are(TestModel.C4.class.getDeclaredMethod("m4", String.class)));

            org.hamcrest.MatcherAssert.assertThat(reflections.getMethodsWithAnyParamAnnotated(
                    new TestModel.AM1() {
                        public String value() {
                            return "2";
                        }

                        public Class<? extends Annotation> annotationType() {
                            return TestModel.AM1.class;
                        }
                    }),
                    are(TestModel.C4.class.getDeclaredMethod("m4", String.class)));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    public void testConstructorParameter() throws NoSuchMethodException {
        org.hamcrest.MatcherAssert.assertThat(reflections.getConstructorsMatchParams(String.class),
                are(TestModel.C4.class.getDeclaredConstructor(String.class)));

        org.hamcrest.MatcherAssert.assertThat(reflections.getConstructorsMatchParams(),
                are(TestModel.C1.class.getDeclaredConstructor(), TestModel.C2.class.getDeclaredConstructor(), TestModel.C3.class.getDeclaredConstructor(),
                        TestModel.C4.class.getDeclaredConstructor(), TestModel.C5.class.getDeclaredConstructor(), TestModel.C6.class.getDeclaredConstructor(),
                        TestModel.C7.class.getDeclaredConstructor(), TestModel.Usage.C1.class.getDeclaredConstructor(), TestModel.Usage.C2.class.getDeclaredConstructor()));

        org.hamcrest.MatcherAssert.assertThat(reflections.getConstructorsWithAnyParamAnnotated(TestModel.AM1.class),
                are(TestModel.C4.class.getDeclaredConstructor(String.class)));

        org.hamcrest.MatcherAssert.assertThat(reflections.getConstructorsWithAnyParamAnnotated(
                new TestModel.AM1() {
                    public String value() {
                        return "1";
                    }

                    public Class<? extends Annotation> annotationType() {
                        return TestModel.AM1.class;
                    }
                }),
                are(TestModel.C4.class.getDeclaredConstructor(String.class)));
    }

    @Test
    public void testResourcesScanner() {
        Predicate<String> filter = new ScannerFilter().include(".*\\.xml").exclude(".*testModel-reflections.*\\.xml");
        Reflections reflections = new Reflections(new ConfigurationBuilder()
                .filterInputsBy(filter)
                .setScanners(new ResourcesScanner())
                .setUrls(Collections.singletonList(ClasspathHelper.forClass(TestModel.class))));

        Set<String> resolved = reflections.getResources(Pattern.compile(".*resource1-reflections\\.xml"));
        org.hamcrest.MatcherAssert.assertThat(resolved, are("META-INF/reflections/resource1-reflections.xml"));

        Set<String> resources = reflections.getStore().keys(ClassUtils.getClassSimpleName(ResourcesScanner.class));
        org.hamcrest.MatcherAssert.assertThat(resources, are("resource1-reflections.xml", "resource2-reflections.xml"));
    }

    @Test
    public void testMethodParameterNames() throws NoSuchMethodException {
        assertEquals(reflections.getMethodParamNames(TestModel.C4.class.getDeclaredMethod("m3")),
                Collections.emptyList());

        assertEquals(reflections.getMethodParamNames(TestModel.C4.class.getDeclaredMethod("m4", String.class)),
                Collections.singletonList("string"));

        assertEquals(reflections.getMethodParamNames(TestModel.C4.class.getDeclaredMethod("add", int.class, int.class)),
                Arrays.asList("i1", "i2"));

        assertEquals(reflections.getConstructorParamNames(TestModel.C4.class.getDeclaredConstructor(String.class)),
                Collections.singletonList("f1"));
    }

    @Test
    public void testMemberUsageScanner() throws NoSuchFieldException, NoSuchMethodException {
        //field usage
        org.hamcrest.MatcherAssert.assertThat(reflections.getFieldUsage(TestModel.Usage.C1.class.getDeclaredField("c2")),
                are(TestModel.Usage.C1.class.getDeclaredConstructor(),
                        TestModel.Usage.C1.class.getDeclaredConstructor(TestModel.Usage.C2.class),
                        TestModel.Usage.C1.class.getDeclaredMethod("method"),
                        TestModel.Usage.C1.class.getDeclaredMethod("method", String.class)));

        //method usage
        org.hamcrest.MatcherAssert.assertThat(reflections.getMethodUsage(TestModel.Usage.C1.class.getDeclaredMethod("method")),
                are(TestModel.Usage.C2.class.getDeclaredMethod("method")));

        org.hamcrest.MatcherAssert.assertThat(reflections.getMethodUsage(TestModel.Usage.C1.class.getDeclaredMethod("method", String.class)),
                are(TestModel.Usage.C2.class.getDeclaredMethod("method")));

        //constructor usage
        org.hamcrest.MatcherAssert.assertThat(reflections.getConstructorUsage(TestModel.Usage.C1.class.getDeclaredConstructor()),
                are(TestModel.Usage.C2.class.getDeclaredConstructor(),
                        TestModel.Usage.C2.class.getDeclaredMethod("method")));

        org.hamcrest.MatcherAssert.assertThat(reflections.getConstructorUsage(TestModel.Usage.C1.class.getDeclaredConstructor(TestModel.Usage.C2.class)),
                are(TestModel.Usage.C2.class.getDeclaredMethod("method")));
    }

    @Test
    public void testScannerNotConfigured() {
        try {
            new Reflections(TestModel.class, TestModelFilter).getMethodsAnnotatedWith(TestModel.AC1.class);
            fail();
        } catch (ReflectionsException e) {
            assertEquals(e.getMessage(), "Scanner " + MethodAnnotationsScanner.class.getSimpleName() + " was not configured");
        }
    }


    public static String getUserDir() {
        File file = new File(System.getProperty("user.dir"));
        //a hack to fix user.dir issue(?) in surfire
        if (Arrays.asList(file.list()).contains("reflections")) {
            file = new File(file, "reflections");
        }
        return file.getAbsolutePath();
    }

    private final BaseMatcher<Set<Class<?>>> isEmpty = new BaseMatcher<Set<Class<?>>>() {
        public boolean matches(Object o) {
            return ((Collection<?>) o).isEmpty();
        }

        public void describeTo(Description description) {
            description.appendText("empty collection");
        }
    };

    private abstract static class Match<T> extends BaseMatcher<T> {
        public void describeTo(Description description) {
        }
    }

    public static <T> Matcher<Set<? super T>> are(final T... ts) {
        final Collection<?> c1 = Arrays.asList(ts);
        return new Match<Set<? super T>>() {
            public boolean matches(Object o) {
                Collection<?> c2 = (Collection<?>) o;
                return c1.containsAll(c2) && c2.containsAll(c1);
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(Arrays.toString(ts));
            }
        };
    }

    private Matcher<Set<Class<?>>> annotatedWith(final Class<? extends Annotation> annotation) {
        return new Match<Set<Class<?>>>() {
            public boolean matches(Object o) {
                for (Class<?> c : (Iterable<Class<?>>) o) {
                    if (!annotationTypes(Arrays.asList(c.getAnnotations())).contains(annotation)) return false;
                }
                return true;
            }
        };
    }

    private Matcher<Set<Class<?>>> metaAnnotatedWith(final Class<? extends Annotation> annotation) {
        return new Match<Set<Class<?>>>() {
            public boolean matches(Object o) {
                for (Class<?> c : (Iterable<Class<?>>) o) {
                    Set<Class> result = new HashSet<>();
                    List<Class> stack = new ArrayList<>(ReflectionUtils.getAllSuperTypes(c));
                    while (!stack.isEmpty()) {
                        Class next = stack.remove(0);
                        if (result.add(next)) {
                            for (Class<? extends Annotation> ac : annotationTypes(Arrays.asList(next.getDeclaredAnnotations()))) {
                                if (!result.contains(ac) && !stack.contains(ac)) stack.add(ac);
                            }
                        }
                    }
                    if (!result.contains(annotation)) return false;
                }
                return true;
            }
        };
    }

    private List<Class<? extends Annotation>> annotationTypes(Collection<Annotation> annotations) {
        return annotations.stream().filter(Objects::nonNull).map(Annotation::annotationType).collect(Collectors.toList());
    }
}
