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

package com.austinv11.syringe.asm.util;

import com.austinv11.syringe.inject.AnnotationInfo;
import com.austinv11.syringe.inject.TypeInfo;
import com.austinv11.syringe.inject.sites.ClassSite;
import com.austinv11.syringe.inject.sites.FieldSite;
import com.austinv11.syringe.inject.sites.MethodSite;
import com.austinv11.syringe.inject.sites.PackageInfo;
import com.austinv11.syringe.util.Lazy;
import org.objectweb.asm.*;
import sun.management.MethodInfo;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

public final class AsmClassSiteParser {

    public static Lazy<ClassSite> readClassSite(byte[] byteCode) {
        return new Lazy<>(() -> {
            ClassAttributeCollectingVisitor visitor = new ClassAttributeCollectingVisitor();
            ClassReader cr = new ClassReader(byteCode);
            cr.accept(visitor, ClassReader.SKIP_FRAMES);
            return visitor.compile();
        });
    }

    //FIXME: Some attributes are null
    @SuppressWarnings("unchecked")
    private static final class ClassAttributeCollectingVisitor extends ClassVisitor implements Opcodes {

        private final List<AnnotationAttributeCollectingVisitor> annotationVisitors = new ArrayList<>();
        private final List<MethodAttributeCollectingVisitor> methodVisitors = new ArrayList<>();
        private final List<FieldAttributeCollectingVisitor> fieldVisitors = new ArrayList<>();
        private Lazy<TypeInfo>[] interfaces;
        private Lazy<TypeInfo> superClass;
        private Lazy<Optional<ClassSite>> enclosing = new Lazy<>(Optional.empty());
        private String name;
        private int modifiers;
        private ClassSite.Kind kind;

        public ClassAttributeCollectingVisitor() {
            super(ASM6);
        }

        @Override
        public void visit(int version, int access, String name, String signature, String superName, String[]
                interfaces) {
            this.name = name;
            this.modifiers = access;
            this.kind = (ACC_ENUM & access) != 0 ? ClassSite.Kind.ENUM :
                    ((ACC_INTERFACE & access) != 0 ? ClassSite.Kind.INTERFACE :
                            ((ACC_ABSTRACT & access) != 0 ? ClassSite.Kind.ABSTRACT_CLASS : ClassSite.Kind.CLASS));
            this.interfaces = new Lazy[interfaces.length];
            for (int i = 0; i < interfaces.length; i++) {
                String inter = interfaces[i];
                this.interfaces[i] = new Lazy<>(() -> {
                    return new TypeInfo(false, TypeInfo.Direction.NONE, new Lazy[0], new Lazy[0], new Lazy<>(() -> {
                        try {
                            return Optional.ofNullable(ClassSite.fromClass(Class.forName(inter)).get());
                        } catch (ClassNotFoundException e) {
                            throw new RuntimeException(e);
                        }
                    }));
                });
            }
            this.superClass = new Lazy<>(() -> {
                if (superName == null || superName.isEmpty())
                    return null;
                return new TypeInfo(false, TypeInfo.Direction.NONE, new Lazy[0], new Lazy[0], new Lazy<>(() -> {
                    try {
                        return Optional.ofNullable(ClassSite.fromClass(Class.forName(superName)).get());
                    } catch (ClassNotFoundException e) {
                        throw new RuntimeException(e);
                    }
                }));
            });
            super.visit(version, access, name, signature, superName, interfaces);
        }

        @Override
        public void visitOuterClass(String owner, String name, String descriptor) {
            enclosing = new Lazy<>(() -> { //FIXME
                try {
                    return Optional.ofNullable(ClassSite.fromClass(Class.forName(owner)).get());
                } catch (ClassNotFoundException e) {
                    return Optional.empty();
                }
            });
            super.visitOuterClass(owner, name, descriptor);
        }

        @Override
        public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
            AnnotationAttributeCollectingVisitor v = new AnnotationAttributeCollectingVisitor();
            annotationVisitors.add(v);
            return v;
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[]
                exceptions) {
            MethodAttributeCollectingVisitor v = new MethodAttributeCollectingVisitor();
            methodVisitors.add(v);
            return v;
        }

        @Override
        public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
            FieldAttributeCollectingVisitor v = new FieldAttributeCollectingVisitor();
            fieldVisitors.add(v);
            return v;
        }

        public ClassSite compile() {
            Lazy<AnnotationInfo>[] annotations = new Lazy[annotationVisitors.size()];
            for (int i = 0; i < annotations.length; i++) {
                AnnotationAttributeCollectingVisitor av = annotationVisitors.get(i);
                annotations[i] = new Lazy<>(av::compile);
            }
            Lazy<MethodSite>[] methods = new Lazy[methodVisitors.size()];
            for (int i = 0; i < methods.length; i++) {
                MethodAttributeCollectingVisitor mv = methodVisitors.get(i);
                methods[i] = new Lazy<>(mv::compile);
            }
            Lazy<FieldSite>[] fields = new Lazy[fieldVisitors.size()];
            for (int i = 0; i < fields.length; i++) {
                FieldAttributeCollectingVisitor fv = fieldVisitors.get(i);
                fields[i] = new Lazy<>(fv::compile);
            }
            //FIXME: PackageInfo
            return new ClassSite(annotations, name, modifiers, new Lazy<PackageInfo>(() -> null), kind, fields, methods, new Lazy[0],
                    interfaces, superClass, enclosing, new Lazy<>(() -> {
                try {
                    return Class.forName(name);
                } catch (ClassNotFoundException e) {
                    return null;
                }
            }));
        }
    }

    private static final class AnnotationAttributeCollectingVisitor extends AnnotationVisitor implements Opcodes {

        public AnnotationAttributeCollectingVisitor() {
            super(ASM6);
        }

        public AnnotationInfo compile() {

        }
    }

    private static final class MethodAttributeCollectingVisitor extends MethodVisitor implements Opcodes {

        public MethodAttributeCollectingVisitor() {
            super(ASM6);
        }

        public MethodSite compile() {

        }
    }

    private static final class FieldAttributeCollectingVisitor extends FieldVisitor implements Opcodes {

        public FieldAttributeCollectingVisitor() {
            super(ASM6);
        }

        public FieldSite compile() {

        }
    }
}
