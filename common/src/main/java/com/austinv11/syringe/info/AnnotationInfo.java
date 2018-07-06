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

import com.austinv11.syringe.access.LazyMap;
import com.austinv11.syringe.access.MethodAccessor;
import com.austinv11.syringe.util.ClassName;
import com.austinv11.syringe.util.Lazy;

import java.lang.annotation.Annotation;
import java.util.List;
import java.util.Optional;

public class AnnotationInfo implements TransformableInfo<Annotation> {

    private final ClassName name;
    private final List<String> attributes;
    private final LazyMap<String, MethodAccessor> accessors;
    private final List<Lazy<AnnotationInfo>> annotations;
    private final Lazy<Optional<Annotation>> transformed;

    public AnnotationInfo(ClassName name, List<String> attributes, LazyMap<String, MethodAccessor> accessors, List
            <Lazy<AnnotationInfo>> annotations, Lazy<Annotation> annotationSupplier) {
        this.name = name;
        this.attributes = attributes;
        this.accessors = accessors;
        this.annotations = annotations;
        this.transformed = annotationSupplier.optional();
    }

    public AnnotationInfo(ClassName name, List<String> attributes, LazyMap<String, MethodAccessor> accessors,
                          List<Lazy<AnnotationInfo>> annotations) {
        this(name, attributes, accessors, annotations, new Lazy<>());
    }

    public ClassName getName() {
        return name;
    }

    public List<String> getAttributes() {
        return attributes;
    }

    public LazyMap<String, MethodAccessor> getAccessors() {
        return accessors;
    }

    public List<Lazy<AnnotationInfo>> getAnnotations() {
        return annotations;
    }

    @Override
    public Optional<Annotation> transform() {
        return transformed.get();
    }
}
