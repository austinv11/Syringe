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
import com.austinv11.syringe.inject.InjectionTarget;
import com.austinv11.syringe.inject.TypeInfo;
import com.austinv11.syringe.util.Lazy;

import java.lang.reflect.Field;

public class FieldSite extends InjectionSite {

    private final Lazy<TypeInfo> typeInfo;

    //TODO add constant info?

    public static Lazy<FieldSite> fromField(Field field) {
        return new Lazy<>(() -> {
            field.setAccessible(true);
            Lazy<AnnotationInfo[]> annotations = AnnotationInfo.fromAnnotatedElement(field);
            Lazy<TypeInfo> typeInfo = TypeInfo.fromType(field.getGenericType());
            return new FieldSite(annotations, field.getName(), field.getModifiers(), typeInfo);
        });
    }

    public FieldSite(Lazy<AnnotationInfo[]> annotationInfo, String name, int modifiers, Lazy<TypeInfo> typeInfo) {
        super(InjectionTarget.FIELD, annotationInfo, name, modifiers);
        this.typeInfo = typeInfo;
    }

    public Lazy<TypeInfo> getTypeInfo() {
        return typeInfo;
    }
}
