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

import syringe.access.FieldAccessor;
import syringe.access.LazyMap;
import syringe.util.ClassName;
import syringe.util.Lazy;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;

public class AnnotationInfo implements TransformableInfo<Annotation> {

    private final ClassName name;
    private final LazyMap<String, FieldAccessor> accessors;
    private final List<AnnotationInfo> annotations;
    private final Lazy<Optional<Annotation>> transformed;

    public AnnotationInfo(ClassName name, LazyMap<String, FieldAccessor> accessors, List
            <AnnotationInfo> annotations, Lazy<Annotation> annotationSupplier) {
        this.name = name;
        this.accessors = accessors;
        this.annotations = annotations;
        this.transformed = annotationSupplier.optional();
    }

    public AnnotationInfo(ClassName name, LazyMap<String, FieldAccessor> accessors,
                          List<AnnotationInfo> annotations) {
        this(name, accessors, annotations, new Lazy<>());
    }

    public ClassName getName() {
        return name;
    }

    public LazyMap<String, FieldAccessor> getAttributes() {
        return accessors;
    }

    public List<AnnotationInfo> getAnnotations() {
        return annotations;
    }

    @Override
    public Optional<Annotation> transform() {
        return transformed.get();
    }
}
