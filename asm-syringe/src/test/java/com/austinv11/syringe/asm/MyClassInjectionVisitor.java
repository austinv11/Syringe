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

package com.austinv11.syringe.asm;

import com.austinv11.syringe.direct.*;
import com.austinv11.syringe.inject.Injection;
import com.austinv11.syringe.inject.clazz.AddMethodInjection;
import com.austinv11.syringe.inject.sites.ClassSite;
import com.austinv11.syringe.util.Lazy;
import com.austinv11.syringe.util.services.InjectionService;
import com.austinv11.syringe.visitor.ClassInjectionVisitor;
import com.austinv11.syringe.visitor.InjectionVisitor;
import com.google.auto.service.AutoService;

import javax.annotation.Nullable;
import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Optional;

@AutoService(InjectionService.class)
public class MyClassInjectionVisitor implements ClassInjectionVisitor, InjectionService {

    @Override
    public Optional<? extends Injection<ClassSite>> visitClass(Lazy<ClassSite> site) {
        if (!site.get().getName().contains("austinv11"))
            return Optional.empty();
        return Optional.of(new AddMethodInjection() {
            @Override
            public Optional<MethodDefinition> defineMethod(Lazy<ClassSite> clazz) {
                return Optional.of(new MethodDefinition() {
                    @Override
                    public int modifiers() {
                        return Modifier.PUBLIC | Modifier.STATIC;
                    }

                    @Override
                    public String name() {
                        return "generated";
                    }

                    @Override
                    public TypeSignature[] parameterTypes() {
                        return new TypeSignature[]{new TypeSignature(String.class)};
                    }

                    @Override
                    public TypeSignature returnType() {
                        return new TypeSignature(String.class);
                    }

                    @Override
                    public TypeSignature[] throwTypes() {
                        return new TypeSignature[0];
                    }

                    @Override
                    public TypeSignature[] annotatedTypes() {
                        return new TypeSignature[0];
                    }

                    @Override
                    public FieldIdentifier[] preloadedFields() {
                        return new FieldIdentifier[] {new FieldIdentifier(new TypeSignature(TestClass.class), true, new TypeSignature(String.class), "test2")};
                    }

                    @Override
                    public MethodIdentifier[] preloadedMethods() {
                        return new MethodIdentifier[] {new MethodIdentifier(new TypeSignature(TestClass.class), true, new TypeSignature(String.class), "test", new TypeSignature[0])};
                    }

                    @Override
                    public Object callback(@Nullable Object instance, Object[] params, Map<String, DirectFieldAccessor> preloadedFields, Map<String, DirectMethodAccessor> preloadedMethods) throws Throwable {
                        return "Hi! " + preloadedFields.get("test2").get() + " " + preloadedMethods.get("test").invoke(new Object[0]);
                    }
                });
            }
        });
    }

    @Override
    public Collection<InjectionVisitor> visitors() {
        return Collections.singleton(this);
    }
}
