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
package com.austinv11.syringe.inject;

import com.austinv11.syringe.inject.sites.ClassSite;
import com.austinv11.syringe.util.Lazy;

import javax.annotation.Nullable;
import java.lang.reflect.*;
import java.util.Optional;

public class TypeInfo {

    private final boolean generic;
    private final Direction genericDirection;
    private final Lazy<Optional<TypeInfo[]>> genericBounds;
    private final Lazy<AnnotationInfo[]> genericAnnotations;

    private final Lazy<Optional<ClassSite>> value;

    public static Lazy<TypeInfo> fromType(@Nullable AnnotatedType type) {
        if (type == null)
            return new Lazy<>(() -> null);

        return fromType(type.getType(), AnnotationInfo.fromAnnotatedElement(type));
    }

    public static Lazy<TypeInfo> fromType(@Nullable Type type) {
        return fromType(type, new Lazy<>(() -> new AnnotationInfo[0]));
    }

    private static Lazy<TypeInfo> fromType(@Nullable Type type, Lazy<AnnotationInfo[]> hint) {
        return new Lazy<>(() -> {
            if (type == null)
                return null;

            if (type instanceof Class<?>) { //Non-generic
                Class<?> clazz = (Class<?>) type;
                return new TypeInfo(false, Direction.NONE,
                        new Lazy<>(() -> Optional.of(new TypeInfo[]{TypeInfo.fromType(Object.class).get()})),
                        hint, ClassSite.fromClass(clazz).optional());
            } else if (type instanceof ParameterizedType) {
                ParameterizedType ptype = (ParameterizedType) type;
                //                Type[] args = ptype.getActualTypeArguments();
                //                if (args.length == 0) {
                //                    return fromType(Object.class).get(); //Object if we can't determine the bounds
                //                }
                return fromType(ptype.getRawType(), hint).get();
            } else if (type instanceof TypeVariable) {
                TypeVariable tvar = (TypeVariable) type;
                Lazy<Optional<TypeInfo[]>> bounds = new Lazy<>(() -> {
                    AnnotatedType[] annotatedTypes = tvar.getAnnotatedBounds();
                    TypeInfo[] bs = new TypeInfo[annotatedTypes.length];
                    for (int i = 0; i < annotatedTypes.length; i++) {
                        bs[i] = TypeInfo.fromType(annotatedTypes[i]).get();
                    }
                    return Optional.of(bs);
                });
                Lazy<AnnotationInfo[]> annotations = AnnotationInfo.fromAnnotatedElement(tvar).or(hint);
                return new TypeInfo(true, Direction.EXTENDS, bounds, annotations, new Lazy<>(Optional::empty));
            } else if (type instanceof WildcardType) {
                WildcardType wtype = (WildcardType) type;
                if (wtype.getUpperBounds().length == 1 && wtype.getLowerBounds().length == 0) { //Bounds are ?, no explicit bounds or ? extends Object
                    return new TypeInfo(true, Direction.WILDCARD,
                            new Lazy<>(() -> new TypeInfo[]{TypeInfo.fromType(Object.class).get()}).optional(),
                            hint, new Lazy<>(Optional::empty));
                } else if (wtype.getLowerBounds().length > 0) { //Super
                    Lazy<TypeInfo[]> bounds = new Lazy<>(() -> {
                        Type[] jBounds = wtype.getLowerBounds();
                        TypeInfo[] ts = new TypeInfo[jBounds.length];
                        for (int i = 0; i < ts.length; i++) {
                            ts[i] = TypeInfo.fromType(jBounds[i]).get();
                        }
                        return ts;
                    });
                    return new TypeInfo(true, Direction.SUPER, bounds.optional(),
                            hint, new Lazy<>(Optional::empty));
                } else {
                    Lazy<TypeInfo[]> bounds = new Lazy<>(() -> {
                        Type[] jBounds = wtype.getUpperBounds();
                        TypeInfo[] ts = new TypeInfo[jBounds.length];
                        for (int i = 0; i < ts.length; i++) {
                            ts[i] = TypeInfo.fromType(jBounds[i]).get();
                        }
                        return ts;
                    });
                    return new TypeInfo(true, Direction.EXTENDS, bounds.optional(),
                            hint, new Lazy<>(Optional::empty));
                }
            } else if (type instanceof GenericArrayType) {
                GenericArrayType atype = (GenericArrayType) type;
                return TypeInfo.fromType(atype.getGenericComponentType(), hint).get();
            } else {
                throw new IllegalArgumentException("Cannot handle type of class " + type.getClass());
            }
        });
    }


    public TypeInfo(boolean generic, Direction genericDirection, Lazy<Optional<TypeInfo[]>> genericBounds,
                    Lazy<AnnotationInfo[]> genericAnnotations, Lazy<Optional<ClassSite>> value) {
        this.generic = generic;
        this.genericDirection = genericDirection;
        this.genericBounds = genericBounds;
        this.genericAnnotations = genericAnnotations;
        this.value = value;
    }

    public boolean isGeneric() {
        return generic;
    }

    public Direction getGenericDirection() {
        return genericDirection;
    }

    public Lazy<Optional<TypeInfo[]>> getGenericBounds() {
        return genericBounds;
    }

    public Lazy<Optional<ClassSite>> getValue() {
        return value;
    }

    public Lazy<AnnotationInfo[]> getGenericAnnotations() {
        return genericAnnotations;
    }

    public enum Direction {
        SUPER, EXTENDS, WILDCARD, NONE
    }
}
