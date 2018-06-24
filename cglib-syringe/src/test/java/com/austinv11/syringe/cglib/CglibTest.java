/*
 * This file is part of Syringe.
 *
 * Syringe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Syringe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with Syringe.  If not, see <http://www.gnu.org/licenses/>.
 */
package com.austinv11.syringe.cglib;

import com.austinv11.syringe.inject.AnnotationInfo;
import com.austinv11.syringe.inject.Injection;
import com.austinv11.syringe.inject.TypeInfo;
import com.austinv11.syringe.inject.method.*;
import com.austinv11.syringe.inject.sites.ClassSite;
import com.austinv11.syringe.inject.sites.FieldSite;
import com.austinv11.syringe.inject.sites.MethodSite;
import com.austinv11.syringe.proxy.MethodProxy;
import com.austinv11.syringe.util.Lazy;
import com.austinv11.syringe.visitor.InjectionVisitor;

import javax.annotation.Nullable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.Optional;

public class CglibTest {

    public static void main(String[] args) {
        CglibSyringe syringe = new CglibSyringe();

        syringe.addVisitor(new InjectionVisitor() {
            @Override
            public Optional<? extends Injection<ClassSite>> visitClass(Lazy<ClassSite> site) {
                return Optional.empty();
            }

            @Override
            public Optional<? extends Injection<FieldSite>> visitField(Lazy<FieldSite> site) {
                return Optional.empty();
            }

            @Override
            public Optional<? extends Injection<MethodSite>> visitMethod(Lazy<MethodSite> site) {
                return Optional.of(new ReplaceMethodInjection() {
                    @Nullable
                    @Override
                    public Object apply(@Nullable Object thisInstance, Object[] params, MethodProxy original,
                                        Lazy<MethodSite> siteInfo) throws Throwable {
                        AnnotationInfo[] annotations = site.get().getAnnotationInfo().get();
                        for (AnnotationInfo i : annotations) {
                            if (i.materialize().equals(TestAnnotation.class)) {
                                return "Changed!";
                            }
                        }
                        return original.call(params);
                    }
                });
            }
        });

        syringe.addVisitor(new InjectionVisitor() {
            @Override
            public Optional<? extends Injection<ClassSite>> visitClass(Lazy<ClassSite> site) {
                return Optional.empty();
            }

            @Override
            public Optional<? extends Injection<FieldSite>> visitField(Lazy<FieldSite> site) {
                return Optional.empty();
            }

            @Override
            public Optional<? extends Injection<MethodSite>> visitMethod(Lazy<MethodSite> site) {
                return Optional.of(new ErrorRecoveryMethodInjection() {
                    @Override
                    public Object tryRecovery(@Nullable Object thisInstance, Object[] params, Throwable error,
                                              MethodProxy proxy, Lazy<MethodSite> methodInfo) throws Throwable {
                        return "Nope!";
                    }
                });
            }
        });

        syringe.addVisitor(new InjectionVisitor() {
            @Override
            public Optional<? extends Injection<ClassSite>> visitClass(Lazy<ClassSite> site) {
                return Optional.empty();
            }

            @Override
            public Optional<? extends Injection<FieldSite>> visitField(Lazy<FieldSite> site) {
                return Optional.empty();
            }

            @Override
            public Optional<? extends Injection<MethodSite>> visitMethod(Lazy<MethodSite> site) {
                return Optional.of(new PreHookMethodInjection() {
                    @Override
                    public Object[] hookParams(@Nullable Object thisInstance, Object[] params, Lazy<MethodSite> methodInfo) throws Throwable {
                        if (params.length == 1 && params[0] instanceof Integer) {
                            return new Object[]{(Integer) params[0] + 1};
                        }
                        return params;
                    }
                });
            }
        });

        syringe.addVisitor(new InjectionVisitor() {
            @Override
            public Optional<? extends Injection<ClassSite>> visitClass(Lazy<ClassSite> site) {
                return Optional.empty();
            }

            @Override
            public Optional<? extends Injection<FieldSite>> visitField(Lazy<FieldSite> site) {
                return Optional.empty();
            }

            @Override
            public Optional<? extends Injection<MethodSite>> visitMethod(Lazy<MethodSite> site) {
                return Optional.of(new PostHookMethodInjection() {
                    @Nullable
                    @Override
                    public Object hookReturn(@Nullable Object thisInstance, Object[] params, @Nullable Object
                            originalReturn, Lazy<MethodSite> methodInfo) throws Throwable {
                        if (originalReturn instanceof Boolean)
                            return !(Boolean) originalReturn;

                        return originalReturn;
                    }
                });
            }
        });

        syringe.addVisitor(new InjectionVisitor() {
            @Override
            public Optional<? extends Injection<ClassSite>> visitClass(Lazy<ClassSite> site) {
                return Optional.empty();
            }

            @Override
            public Optional<? extends Injection<FieldSite>> visitField(Lazy<FieldSite> site) {
                return Optional.empty();
            }

            @Override
            public Optional<? extends Injection<MethodSite>> visitMethod(Lazy<MethodSite> site) {
                return Optional.of(new IgnoreMethodInjection() {
                    @Override
                    public boolean shouldIgnore(@Nullable Object thisInstance, Object[] params, Lazy<MethodSite>
                            methodInfo) {
                        TypeInfo returnType = methodInfo.get().getReturnType().get();
                        if (returnType.getValue().get().isPresent()) {
                            Class<?> materialized = returnType.getValue().get().get().materialize();
                            if (materialized.equals(Object.class)) {
                                return true;
                            }
                        }
                        return false;
                    }
                });
            }
        });

        Test t = new Test();
        Test proxied = syringe.visit(t);

        if (!t.notAnnotated().equals("Not annotated!"))
            throw new AssertionError();
        if (!t.annotated().equals("Annotated!"))
            throw new AssertionError();
        try {
            t.throwsException();
            throw new AssertionError();
        } catch (RuntimeException e) {

        }
        if (t.interception(1) != 1)
            throw new AssertionError();
        if (!t.returnsTrue())
            throw new AssertionError();
        if (t.returnsNotNull() == null)
            throw new AssertionError();

        if (!proxied.notAnnotated().equals("Not annotated!"))
            throw new AssertionError();
        if (!proxied.annotated().equals("Changed!"))
            throw new AssertionError();
        try {
            if (!proxied.throwsException().equals("Nope!"))
                throw new AssertionError();
        } catch (RuntimeException e) {
            throw new AssertionError();
        }
        if (proxied.interception(1) != 2)
            throw new AssertionError();
        if (proxied.returnsTrue())
            throw new AssertionError();
        if (proxied.returnsNotNull() != null)
            throw new AssertionError();
    }

    public static class Test {

        public String notAnnotated() {
            return "Not annotated!";
        }

        @TestAnnotation
        public String annotated() {
            return "Annotated!";
        }

        public String throwsException() {
            throw new RuntimeException("Exception!");
        }

        public int interception(int i) {
            return i;
        }

        public boolean returnsTrue() {
            return true;
        }

        public Object returnsNotNull() {
            return new Object();
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public @interface TestAnnotation {
    }
}
