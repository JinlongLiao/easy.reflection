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

import io.github.jinlongliao.easy.reflection.scanners.impl.FieldAnnotationsScanner;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Test;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.*;
import static io.github.jinlongliao.easy.reflection.ReflectionUtils.*;

/**
 * @author mamo
 */
@SuppressWarnings("unchecked")
public class ReflectionUtilsTest {

    @Test
    public void getAllTest() {
        assertThat(getAllSuperTypes(TestModel.C3.class, withAnnotation(TestModel.AI1.class)), ReflectionsTest.are(TestModel.I1.class));

        Set<Method> allMethods = ReflectionUtils.getAllMethods(TestModel.C4.class, ReflectionUtils.withModifier(Modifier.PUBLIC), ReflectionUtils.withReturnType(void.class));
        Set<Method> allMethods1 = ReflectionUtils.getAllMethods(TestModel.C4.class, ReflectionUtils.withPattern("public.*.void .*"));

        assertTrue(allMethods.containsAll(allMethods1) && allMethods1.containsAll(allMethods));
        assertThat(allMethods1, names("m1"));

        assertThat(ReflectionUtils.getAllMethods(TestModel.C4.class, withAnyParameterAnnotation(TestModel.AM1.class)), names("m4"));

        assertThat(getAllFields(TestModel.C4.class, withAnnotation(TestModel.AF1.class)), names("f1", "f2"));

        assertThat(getAllFields(TestModel.C4.class, withAnnotation(new TestModel.AF1() {
            public String value() {return "2";}
            public Class<? extends Annotation> annotationType() {return TestModel.AF1.class;}})),
                names("f2"));

        assertThat(ReflectionUtils.getAllFields(TestModel.C4.class, ReflectionUtils.withTypeAssignableTo(String.class)), names("f1", "f2", "f3"));

        assertThat(ReflectionUtils.getAllConstructors(TestModel.C4.class, ReflectionUtils.withParametersCount(0)), names(TestModel.C4.class.getName()));

        assertEquals(ReflectionUtils.getAllAnnotations(TestModel.C3.class).size(), 5);

        Method m4 = ReflectionUtils.getMethods(TestModel.C4.class, ReflectionUtils.withName("m4")).iterator().next();
        assertEquals(m4.getName(), "m4");
        assertTrue(ReflectionUtils.getAnnotations(m4).isEmpty());
    }

    @Test public void withParameter() throws Exception {
        Class target = Collections.class;
        Object arg1 = Arrays.asList(1, 2, 3);

        Set<Method> allMethods = new HashSet<>();
        for (Class<?> type : ReflectionUtils.getAllSuperTypes(arg1.getClass())) {
            allMethods.addAll(ReflectionUtils.getAllMethods(target, ReflectionUtils.withModifier(Modifier.STATIC), ReflectionUtils.withParameters(type)));
        }

        Set<Method> allMethods1 = ReflectionUtils.getAllMethods(target, ReflectionUtils.withModifier(Modifier.STATIC), ReflectionUtils.withParametersAssignableTo(arg1.getClass()));

        assertEquals(allMethods, allMethods1);

        for (Method method : allMethods) { //effectively invokable
            //noinspection UnusedDeclaration
            Object invoke = method.invoke(null, arg1);
        }
    }

    @Test
    public void withParametersAssignableFromTest() throws Exception {
        //Check for null safe
        ReflectionUtils.getAllMethods(Collections.class, ReflectionUtils.withModifier(Modifier.STATIC), ReflectionUtils.withParametersAssignableFrom());

        Class target = Collections.class;
        Object arg1 = Arrays.asList(1, 2, 3);

        Set<Method> allMethods = new HashSet<>();
        for (Class<?> type : ReflectionUtils.getAllSuperTypes(arg1.getClass())) {
            allMethods.addAll(ReflectionUtils.getAllMethods(target, ReflectionUtils.withModifier(Modifier.STATIC), ReflectionUtils.withParameters(type)));
        }

        Set<Method> allMethods1 = ReflectionUtils.getAllMethods(target, ReflectionUtils.withModifier(Modifier.STATIC), ReflectionUtils.withParametersAssignableFrom(Iterable.class), ReflectionUtils.withParametersAssignableTo(arg1.getClass()));

        assertEquals(allMethods, allMethods1);

        for (Method method : allMethods) { //effectively invokable
            //noinspection UnusedDeclaration
            Object invoke = method.invoke(null, arg1);
        }
    }

    @Test public void withReturn() {
        Set<Method> returnMember = ReflectionUtils.getAllMethods(Class.class, ReflectionUtils.withReturnTypeAssignableTo(Member.class));
        Set<Method> returnsAssignableToMember = ReflectionUtils.getAllMethods(Class.class, ReflectionUtils.withReturnType(Method.class));

        assertTrue(returnMember.containsAll(returnsAssignableToMember));
        assertFalse(returnsAssignableToMember.containsAll(returnMember));

        returnsAssignableToMember = ReflectionUtils.getAllMethods(Class.class, ReflectionUtils.withReturnType(Field.class));
        assertTrue(returnMember.containsAll(returnsAssignableToMember));
        assertFalse(returnsAssignableToMember.containsAll(returnMember));
    }

    @Test
    public void getAllAndReflections() {
        Reflections reflections = new Reflections(TestModel.class, new FieldAnnotationsScanner());

        Set<Field> af1 = reflections.getFieldsAnnotatedWith(TestModel.AF1.class);
        Set<? extends Field> allFields = ReflectionUtils.getAll(af1, ReflectionUtils.withModifier(Modifier.PROTECTED));
        assertEquals(1, allFields.size());
        assertThat(allFields, names("f2"));
    }

    private Set<String> names(Set<? extends Member> o) {
        return o.stream().map(Member::getName).collect(Collectors.toSet());
    }

    private BaseMatcher<Set<? extends Member>> names(final String... namesArray) {
        return new BaseMatcher<Set<? extends Member>>() {

                public boolean matches(Object o) {
                    Collection<String> transform = names((Set<Member>) o);
                    final Collection<?> names = Arrays.asList(namesArray);
                    return transform.containsAll(names) && names.containsAll(transform);
                }

                public void describeTo(Description description) {
                }
            };
    }
}
