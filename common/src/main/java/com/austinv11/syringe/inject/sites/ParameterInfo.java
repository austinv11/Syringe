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
package com.austinv11.syringe.inject.sites;

import com.austinv11.syringe.inject.AnnotationInfo;
import com.austinv11.syringe.inject.TypeInfo;
import com.austinv11.syringe.util.Lazy;

import java.lang.reflect.Parameter;

public class ParameterInfo {

    private final Lazy<AnnotationInfo>[] paramAnnotations;
    private final Lazy<TypeInfo> parameterType;
    private final String parameterName;
    private final boolean isVarargs;

    public static Lazy<ParameterInfo> fromParameter(Parameter p) {
        return new Lazy<>(() -> {
            Lazy<AnnotationInfo>[] annotations = AnnotationInfo.fromAnnotatedElement(p);
            Lazy<TypeInfo> parameterType = TypeInfo.fromType(p.getParameterizedType());
            return new ParameterInfo(annotations, parameterType, p.getName(), p.isVarArgs());
        });
    }

    public ParameterInfo(Lazy<AnnotationInfo>[] paramAnnotations, Lazy<TypeInfo> parameterType, String parameterName,
                         boolean isVarargs) {
        this.paramAnnotations = paramAnnotations;
        this.parameterType = parameterType;
        this.parameterName = parameterName;
        this.isVarargs = isVarargs;
    }

    public Lazy<AnnotationInfo>[] getParameterAnnotations() {
        return paramAnnotations;
    }

    public Lazy<TypeInfo> getParameterType() {
        return parameterType;
    }

    public String getParameterName() {
        return parameterName;
    }

    public boolean isVarargs() {
        return isVarargs;
    }
}
