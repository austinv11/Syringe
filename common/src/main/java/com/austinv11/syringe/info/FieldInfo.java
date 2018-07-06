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
import com.austinv11.syringe.util.ClassName;
import com.austinv11.syringe.util.Lazy;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

public class FieldInfo implements TransformableInfo<Field> {

    private final String name;
    private final int modifiers;
    private final ClassName type;
    private final List<Lazy<AnnotationInfo>> annotations;
    private final FieldAccessor accessor;
    private final Lazy<Optional<Field>> transformed;

    public FieldInfo(String name, int modifiers, ClassName type, List<Lazy<AnnotationInfo>> annotations,
                     FieldAccessor accessor, Lazy<Field> transformed) {
        this.name = name;
        this.modifiers = modifiers;
        this.type = type;
        this.annotations = annotations;
        this.accessor = accessor;
        this.transformed = transformed.optional();
    }

    public FieldInfo(String name, int modifiers, ClassName type, List<Lazy<AnnotationInfo>> annotations,
                     FieldAccessor accessor) {
        this(name, modifiers, type, annotations, accessor, new Lazy<>());
    }

    public String getName() {
        return name;
    }

    public int getModifiers() {
        return modifiers;
    }

    public ClassName getType() {
        return type;
    }

    public List<Lazy<AnnotationInfo>> getAnnotations() {
        return annotations;
    }

    public FieldAccessor getAccessor() {
        return accessor;
    }

    @Override
    public Optional<Field> transform() {
        return transformed.get();
    }
}
