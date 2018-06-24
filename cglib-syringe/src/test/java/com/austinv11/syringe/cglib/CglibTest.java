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
import com.austinv11.syringe.inject.method.ReplaceMethodInjection;
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

        Test t = new Test();
        Test proxied = syringe.visit(t);

        if (!t.notAnnotated().equals("Not annotated!"))
            throw new AssertionError();
        if (!t.annotatated().equals("Annotated!"))
            throw new AssertionError();
        if (!proxied.notAnnotated().equals("Not annotated!"))
            throw new AssertionError();
        if (!proxied.annotatated().equals("Changed!"))
            throw new AssertionError();
    }

    public static class Test {

        public String notAnnotated() {
            return "Not annotated!";
        }

        @TestAnnotation
        public String annotatated() {
            return "Annotated!";
        }
    }

    @Retention(RetentionPolicy.RUNTIME)
    public static @interface TestAnnotation {

    }
}
