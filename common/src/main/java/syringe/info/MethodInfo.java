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

package syringe.info;

import syringe.access.MethodAccessor;
import syringe.util.ClassName;
import syringe.util.Lazy;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public class MethodInfo implements TransformableInfo<Method> {

    private final String name;
    private final int modifiers;
    private final ClassName returnType;
    private final List<Lazy<AnnotationInfo>> annotations;
    private final List<ParameterInfo> params;
    private final MethodAccessor accessor;
    private final Lazy<Optional<Method>> transformed;

    public MethodInfo(String name, int modifiers, ClassName returnType, List<Lazy<AnnotationInfo>> annotations, List
            <ParameterInfo> params, MethodAccessor accessor, Lazy<Method> transformed) {
        this.name = name;
        this.modifiers = modifiers;
        this.returnType = returnType;
        this.annotations = annotations;
        this.params = params;
        this.accessor = accessor;
        this.transformed = transformed.optional();
    }

    public MethodInfo(String name, int modifiers, ClassName returnType, List<Lazy<AnnotationInfo>> annotations,
                      List<ParameterInfo> params, MethodAccessor accessor) {
        this(name, modifiers, returnType, annotations, params, accessor, new Lazy<>());
    }

    public String getName() {
        return name;
    }

    public int getModifiers() {
        return modifiers;
    }

    public ClassName getReturnType() {
        return returnType;
    }

    public List<Lazy<AnnotationInfo>> getAnnotations() {
        return annotations;
    }

    public List<ParameterInfo> getParams() {
        return params;
    }

    public MethodAccessor getAccessor() {
        return accessor;
    }

    public String getInternalTypeSignature() {
        StringBuilder s = new StringBuilder("(");
        for (ParameterInfo p : getParams()) {
            s.append(p.getType().getAsInternalTypeName());
        }
        s.append(")");
        s.append(getReturnType().getAsInternalTypeName());
        return s.toString();
    }

    @Override
    public Optional<Method> transform() {
        return transformed.get();
    }
}
