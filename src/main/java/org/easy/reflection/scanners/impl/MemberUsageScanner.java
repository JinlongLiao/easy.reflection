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

package org.easy.reflection.scanners.impl;

import javassist.CannotCompileException;
import javassist.ClassPool;
import javassist.CtBehavior;
import javassist.CtClass;
import javassist.LoaderClassPath;
import javassist.NotFoundException;
import javassist.bytecode.MethodInfo;
import javassist.expr.ConstructorCall;
import javassist.expr.ExprEditor;
import javassist.expr.FieldAccess;
import javassist.expr.MethodCall;
import javassist.expr.NewExpr;
import org.easy.reflection.Store;
import org.easy.reflection.exception.ReflectionsException;
import org.easy.reflection.scanners.AbstractScanner;
import org.easy.reflection.util.ClasspathHelper;
import org.easy.reflection.util.StringUtils;
import org.easy.reflection.adapters.imp.JavassistAdapter;

/**
 * scans methods/constructors/fields usage
 * <p><i> depends on {@link JavassistAdapter} configured </i>
 */
@SuppressWarnings("unchecked")
public class MemberUsageScanner extends AbstractScanner {
    private ClassPool classPool;

    @Override
    public void scan(Object cls, Store store) {
        try {
            CtClass ctClass = getClassPool().get(getMetadataAdapter().getClassName(cls));
            for (CtBehavior member : ctClass.getDeclaredConstructors()) {
                scanMember(member, store);
            }
            for (CtBehavior member : ctClass.getDeclaredMethods()) {
                scanMember(member, store);
            }
            ctClass.detach();
        } catch (Exception e) {
            throw new ReflectionsException("Could not scan method usage for " + getMetadataAdapter().getClassName(cls), e);
        }
    }

    void scanMember(CtBehavior member, Store store) throws CannotCompileException {
        //key contains this$/val$ means local field/parameter closure
        //+ " #" + member.getMethodInfo().getLineNumber(0)
        final String key = member.getDeclaringClass().getName() + "." + member.getMethodInfo().getName() +
                "(" + parameterNames(member.getMethodInfo()) + ")";
        member.instrument(new ExprEditor() {
            @Override
            public void edit(NewExpr e) throws CannotCompileException {
                try {
                    put(store, e.getConstructor().getDeclaringClass().getName() + "." + "<init>" +
                            "(" + parameterNames(e.getConstructor().getMethodInfo()) + ")", e.getLineNumber(), key);
                } catch (NotFoundException e1) {
                    throw new ReflectionsException("Could not find new instance usage in " + key, e1);
                }
            }

            @Override
            public void edit(MethodCall m) throws CannotCompileException {
                try {
                    put(store, m.getMethod().getDeclaringClass().getName() + "." + m.getMethodName() +
                            "(" + parameterNames(m.getMethod().getMethodInfo()) + ")", m.getLineNumber(), key);
                } catch (NotFoundException e) {
                    throw new ReflectionsException("Could not find member " + m.getClassName() + " in " + key, e);
                }
            }

            @Override
            public void edit(ConstructorCall c) throws CannotCompileException {
                try {
                    put(store, c.getConstructor().getDeclaringClass().getName() + "." + "<init>" +
                            "(" + parameterNames(c.getConstructor().getMethodInfo()) + ")", c.getLineNumber(), key);
                } catch (NotFoundException e) {
                    throw new ReflectionsException("Could not find member " + c.getClassName() + " in " + key, e);
                }
            }

            @Override
            public void edit(FieldAccess f) throws CannotCompileException {
                try {
                    put(store, f.getField().getDeclaringClass().getName() + "." + f.getFieldName(), f.getLineNumber(), key);
                } catch (NotFoundException e) {
                    throw new ReflectionsException("Could not find member " + f.getFieldName() + " in " + key, e);
                }
            }
        });
    }

    private void put(Store store, String key, int lineNumber, String value) {
        if (acceptFilter(key)) {
            put(store, key, value + " #" + lineNumber);
        }
    }

    String parameterNames(MethodInfo info) {
        return StringUtils.append(getMetadataAdapter().getParameterNames(info), ", ");
    }

    private ClassPool getClassPool() {
        if (classPool == null) {
            synchronized (this) {
                if (classPool == null) {
                    classPool = new ClassPool();
                    ClassLoader[] classLoaders = getConfiguration().getClassLoaders();
                    if (classLoaders == null) {
                        classLoaders = ClasspathHelper.classLoaders();
                    }
                    for (ClassLoader classLoader : classLoaders) {
                        classPool.appendClassPath(new LoaderClassPath(classLoader));
                    }
                }
            }
        }
        return classPool;
    }
}
