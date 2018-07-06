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

import syringe.util.ClassName;
import syringe.util.Lazy;

import java.lang.reflect.Parameter;
import java.util.List;
import java.util.Optional;

public class ParameterInfo implements TransformableInfo<Parameter> {

    private final String name; //May not be accurate depending on javac flags
    private final int modifiers;
    private final ClassName type;
    private final List<Lazy<AnnotationInfo>> annotations;
    private final Lazy<Optional<Parameter>> transformed;

    public ParameterInfo(String name, int modifiers, ClassName type, List<Lazy<AnnotationInfo>> annotations, Lazy
            <Parameter> transformed) {
        this.name = name;
        this.modifiers = modifiers;
        this.type = type;
        this.annotations = annotations;
        this.transformed = transformed.optional();
    }

    public ParameterInfo(String name, int modifiers, ClassName type, List<Lazy<AnnotationInfo>> annotations) {
        this(name, modifiers, type, annotations, new Lazy<>());
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

    @Override
    public Optional<Parameter> transform() {
        return transformed.get();
    }
}
