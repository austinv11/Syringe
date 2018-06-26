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
package com.austinv11.syringe.inject.sites;

import com.austinv11.syringe.inject.*;
import com.austinv11.syringe.util.Lazy;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Type;

public class MethodSite extends InjectionSite {

    private final Lazy<TypeInfo> returnType;

    private final int parameterCount;
    private final Lazy<ParameterInfo>[] parameters;

    private final Lazy<TypeInfo>[] exceptions;

    public static Lazy<MethodSite> fromMethod(Method method) {
        return new Lazy<>(() -> {
            method.setAccessible(true);

            Lazy<AnnotationInfo>[] annotations = AnnotationInfo.fromAnnotatedElement(method);

            Lazy<TypeInfo> returnType = TypeInfo.fromType(method.getGenericReturnType());

            Parameter[] parameters = method.getParameters();
            Lazy<ParameterInfo>[] params = new Lazy[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                params[i] = ParameterInfo.fromParameter(parameters[i]);
            }

            Type[] eTypes = method.getGenericExceptionTypes();
            Lazy<TypeInfo>[] exceptions = new Lazy[eTypes.length];
            for (int i = 0; i < eTypes.length; i++) {
                exceptions[i] = TypeInfo.fromType(eTypes[i]);
            }

            return new MethodSite(annotations, method.getName(), method.getModifiers(), returnType,
                    method.getParameterCount(), params, exceptions);
        });
    }

    public MethodSite(Lazy<AnnotationInfo>[] annotationInfo, String name, int modifiers, Lazy<TypeInfo>
            returnType, int parameterCount, Lazy<ParameterInfo>[] parameters, Lazy<TypeInfo>[] exceptions) {
        super(InjectionTarget.METHOD, annotationInfo, name, modifiers);
        this.returnType = returnType;
        this.parameterCount = parameterCount;
        this.parameters = parameters;
        this.exceptions = exceptions;
    }

    public Lazy<TypeInfo> getReturnType() {
        return returnType;
    }

    public int getParameterCount() {
        return parameterCount;
    }

    public Lazy<ParameterInfo>[] getParameters() {
        return parameters;
    }

    public Lazy<TypeInfo>[] getExceptions() {
        return exceptions;
    }
}
