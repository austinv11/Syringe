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

public class MethodSite extends InjectionSite {

    private final TypeInfo returnType;

    private final int parameterCount;
    private final ParameterInfo[] parameters;

    public MethodSite(InjectionTarget target, AnnotationInfo[] annotationInfo, String name, int modifiers, TypeInfo
            returnType, int parameterCount, ParameterInfo[] parameters) {
        super(target, annotationInfo, name, modifiers);
        this.returnType = returnType;
        this.parameterCount = parameterCount;
        this.parameters = parameters;
    }

    public TypeInfo getReturnType() {
        return returnType;
    }

    public int getParameterCount() {
        return parameterCount;
    }

    public ParameterInfo[] getParameters() {
        return parameters;
    }
}
