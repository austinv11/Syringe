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

package com.austinv11.syringe.info;

import com.austinv11.syringe.access.FieldAccessor;
import com.austinv11.syringe.access.LazyMap;
import com.austinv11.syringe.access.MethodAccessor;
import com.austinv11.syringe.util.ClassName;
import com.austinv11.syringe.util.Lazy;

import java.util.List;
import java.util.Optional;

public class ClassInfo implements TransformableInfo<Class> {

    //Note: Intentionally limited to prevent going down a rabbit hole of object constructions (FIXME later)
    private final ClassName name;
    private final int modifiers;
    private final List<String> fields;
    private final List<String> methods;
    private final LazyMap<String, FieldAccessor> fieldAccessors;
    private final LazyMap<String, MethodAccessor> methodAccessors;
    private final List<Lazy<AnnotationInfo>> annotations;
    private final List<ClassName> extendsList;
    private final Lazy<Optional<Class>> transformed;

    public ClassInfo(ClassName name, int modifiers, List<String> fields, List<String> methods, LazyMap<String, FieldAccessor>

            fieldAccessors, LazyMap<String, MethodAccessor> methodAccessors, List<Lazy<AnnotationInfo>> annotations,
                     List<ClassName> extendsList, Lazy<Class> transformed) {
        this.name = name;
        this.modifiers = modifiers;
        this.fields = fields;
        this.methods = methods;
        this.fieldAccessors = fieldAccessors;
        this.methodAccessors = methodAccessors;
        this.annotations = annotations;
        this.extendsList = extendsList;
        this.transformed = transformed.optional();
    }

    public ClassInfo(ClassName name, int modifiers, List<String> fields, List<String> methods, LazyMap<String,
            FieldAccessor> fieldAccessors, LazyMap<String, MethodAccessor> methodAccessors,
                     List<Lazy<AnnotationInfo>> annotations, List<ClassName> extendsList) {
        this(name, modifiers, fields, methods, fieldAccessors, methodAccessors, annotations, extendsList, new Lazy<>());
    }

    public ClassName getName() {
        return name;
    }

    public int getModifiers() {
        return modifiers;
    }

    public List<String> getFields() {
        return fields;
    }

    public List<String> getMethods() {
        return methods;
    }

    public LazyMap<String, FieldAccessor> getFieldAccessors() {
        return fieldAccessors;
    }

    public LazyMap<String, MethodAccessor> getMethodAccessors() {
        return methodAccessors;
    }

    public List<Lazy<AnnotationInfo>> getAnnotations() {
        return annotations;
    }

    public List<ClassName> getExtendsList() {
        return extendsList;
    }

    @Override
    public Optional<Class> transform() {
        return transformed.get();
    }
}
