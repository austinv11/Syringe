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

import com.austinv11.syringe.inject.*;
import com.austinv11.syringe.util.Lazy;

import java.lang.reflect.*;

public class ClassSite extends InjectionSite {

    private final Lazy<PackageInfo> packageInfo;
    private final Kind kind;

    private final Lazy<FieldSite>[] fields;

    private final Lazy<MethodSite>[] methods;

    private final Lazy<TypeInfo>[] types;

    private final Lazy<TypeInfo>[] interfaces;

    private final Lazy<TypeInfo> superClass;

    private final Lazy<ClassSite> enclosingClass;

    private final Lazy<Class<?>> materialized;

    public static Lazy<ClassSite> fromClass(Class<?> clazz) {
        return new Lazy<>(() -> {
            Lazy<AnnotationInfo>[] annotations = AnnotationInfo.fromAnnotatedElement(clazz);

            Field[] jFields = clazz.getFields();
            Lazy<FieldSite>[] fields = new Lazy[jFields.length];
            for (int i = 0; i < jFields.length; i++) {
                fields[i] = FieldSite.fromField(jFields[i]);
            }

            Method[] jMethods = clazz.getMethods();
            Lazy<MethodSite>[] methods = new Lazy[jMethods.length];
            for (int i = 0; i < jMethods.length; i++) {
                methods[i] = MethodSite.fromMethod(jMethods[i]);
            }

            TypeVariable[] jTypes = clazz.getTypeParameters();
            Lazy<TypeInfo>[] types = new Lazy[jTypes.length];
            for (int i = 0; i < jTypes.length; i++) {
                types[i] = TypeInfo.fromType(jTypes[i]);
            }

            Type[] jInterfaces = clazz.getGenericInterfaces();
            Lazy<TypeInfo>[] interfaces = new Lazy[jInterfaces.length];
            for (int i = 0; i < jInterfaces.length; i++) {
                interfaces[i] = TypeInfo.fromType(jInterfaces[i]);
            }

            Lazy<TypeInfo> superClass = new Lazy<>(() -> {
                Type jSuper = clazz.getGenericSuperclass();
                return TypeInfo.fromType(jSuper).get();
            });

            Lazy<ClassSite> enclosing = new Lazy<>(() -> {
                Class<?> jEnclosing = clazz.getEnclosingClass();
                return ClassSite.fromClass(jEnclosing).get();
            });

            Kind k;
            if (clazz.isAnnotation()) {
                k = Kind.ANNOTATION;
            } else if (clazz.isInterface()) {
                k = Kind.INTERFACE;
            } else if (clazz.isEnum()) {
                k = Kind.ENUM;
            } else if (Modifier.isAbstract(clazz.getModifiers())) {
                k = Kind.ABSTRACT_CLASS;
            } else {
                k = Kind.CLASS;
            }

            return new ClassSite(annotations, clazz.getCanonicalName(), clazz.getModifiers(),
                    PackageInfo.fromPackage(clazz.getPackage()), k, fields, methods, types, interfaces, superClass,
                    enclosing, new Lazy<>(clazz));
        });
    }

    public ClassSite(Lazy<AnnotationInfo>[] annotationInfo, String name, int modifiers, Lazy<PackageInfo>
            packageInfo, Kind kind, Lazy<FieldSite>[] fields, Lazy<MethodSite>[] methods, Lazy<TypeInfo>[] types,
                     Lazy<TypeInfo>[] interfaces, Lazy<TypeInfo> superClass, Lazy<ClassSite> enclosingClass, Lazy
                             <Class<?>> materialized) {
        super(InjectionTarget.CLASS, annotationInfo, name, modifiers);
        this.packageInfo = packageInfo;
        this.kind = kind;
        this.fields = fields;
        this.methods = methods;
        this.types = types;
        this.interfaces = interfaces;
        this.superClass = superClass;
        this.enclosingClass = enclosingClass;
        this.materialized = materialized;
    }

    public Lazy<PackageInfo> getPackage() {
        return packageInfo;
    }

    public Lazy<FieldSite>[] getFields() {
        return fields;
    }

    public Lazy<MethodSite>[] getMethods() {
        return methods;
    }

    public Lazy<TypeInfo>[] getTypes() {
        return types;
    }

    public Kind getKind() {
        return kind;
    }

    public Lazy<TypeInfo>[] getInterfaces() {
        return interfaces;
    }

    public Lazy<TypeInfo> getSuperClass() {
        return superClass;
    }

    public Lazy<ClassSite> getEnclosingClass() {
        return enclosingClass;
    }

    public Class<?> materialize() {
        return materialized.get();
    }

    public enum Kind {
        INTERFACE, CLASS, ENUM, ABSTRACT_CLASS, ANNOTATION
    }
}
