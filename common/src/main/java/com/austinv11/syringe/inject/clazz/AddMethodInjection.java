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
package com.austinv11.syringe.inject.clazz;

import com.austinv11.syringe.direct.*;
import com.austinv11.syringe.inject.Injection;
import com.austinv11.syringe.inject.InjectionDelta;
import com.austinv11.syringe.inject.InjectionTarget;
import com.austinv11.syringe.inject.sites.ClassSite;
import com.austinv11.syringe.util.Lazy;

import javax.annotation.Nullable;
import java.util.Map;
import java.util.Optional;

public abstract class AddMethodInjection extends Injection<ClassSite> {

    public AddMethodInjection() {
        super(InjectionTarget.CLASS, InjectionDelta.ADDITION);
    }

    public abstract Optional<MethodDefinition> defineMethod(Lazy<ClassSite> clazz);

    public interface MethodDefinition {

        int modifiers();

        String name();

        TypeSignature[] parameterTypes();

        TypeSignature returnType();

        TypeSignature[] throwTypes();

        TypeSignature[] annotatedTypes();

        //Hooks to allow for potential reflection optimizations
        FieldIdentifier[] preloadedFields();

        MethodIdentifier[] preloadedMethods();

        default String descriptor() {
            String params = "";
            for (TypeSignature param : parameterTypes())
                params += param.toString();
            return String.format("(%s)%s", params, returnType().toString());
        }

        @Nullable
        Object callback(@Nullable Object instance,
                        Object[] params,
                        Map<String, DirectFieldAccessor> preloadedFields,
                        Map<String, DirectMethodAccessor> preloadedMethods) throws Throwable;
    }
}
