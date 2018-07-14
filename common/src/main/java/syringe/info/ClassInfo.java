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

import syringe.access.LazyMap;
import syringe.util.ClassName;
import syringe.util.Lazy;

import java.util.List;
import java.util.Optional;

public class ClassInfo implements TransformableInfo<Class> {

    //Note: Intentionally limited to prevent going down a rabbit hole of object constructions (FIXME later)
    private final ClassName name;
    private final int modifiers;
    private final LazyMap<String, FieldInfo> fieldAccessors;
    private final LazyMap<String, MethodInfo> methodAccessors;
    private final List<Lazy<AnnotationInfo>> annotations;
    private final List<ClassName> extendsList;
    private final Lazy<Optional<Class>> transformed;

    public ClassInfo(ClassName name, int modifiers, LazyMap<String, FieldInfo>
            fieldAccessors, LazyMap<String, MethodInfo> methodAccessors, List<Lazy<AnnotationInfo>> annotations,
                     List<ClassName> extendsList, Lazy<Class> transformed) {
        this.name = name;
        this.modifiers = modifiers;
        this.fieldAccessors = fieldAccessors;
        this.methodAccessors = methodAccessors;
        this.annotations = annotations;
        this.extendsList = extendsList;
        this.transformed = transformed.optional();
    }

    public ClassInfo(ClassName name, int modifiers, LazyMap<String, FieldInfo> fieldAccessors, LazyMap<String, MethodInfo> methodAccessors,
                     List<Lazy<AnnotationInfo>> annotations, List<ClassName> extendsList) {
        this(name, modifiers, fieldAccessors, methodAccessors, annotations, extendsList, new Lazy<>());
    }

    public ClassName getName() {
        return name;
    }

    public int getModifiers() {
        return modifiers;
    }

    public LazyMap<String, FieldInfo> getFields() {
        return fieldAccessors;
    }

    public LazyMap<String, MethodInfo> getMethods() {
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
