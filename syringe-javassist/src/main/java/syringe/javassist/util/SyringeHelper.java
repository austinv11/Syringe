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

package syringe.javassist.util;

import javassist.*;
import syringe.access.FieldAccessor;
import syringe.util.LazyMap;
import syringe.access.MethodAccessor;
import syringe.info.*;
import syringe.util.ClassName;
import syringe.util.Lazy;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Method;
import java.util.*;
import java.util.function.Function;

public final class SyringeHelper {

    private SyringeHelper() {}

    private static Lazy<AnnotationInfo> annotationFromObject(Object o) {
        if (!(o instanceof Annotation))
            return new Lazy<>();

        return new Lazy<>(() -> {
            Class<? extends Annotation> type = ((Annotation) o).annotationType();
            //TODO: non-reflection based accessors
            Method[] methods = type.getDeclaredMethods();
            Map<String, FieldAccessor> accessors = new HashMap<>();
            for (Method m : methods) {
                if (m.getParameterCount() != 0)
                    continue;
                try {
                    accessors.put(m.getName(), new FieldAccessor() {
                        private final MethodHandle handle = MethodHandles.lookup().unreflect(m).bindTo(o);

                        @Nullable
                        @Override
                        public Object get() {
                            try {
                                return handle.invokeExact();
                            } catch (Throwable throwable) {
                                throwable.printStackTrace();
                                return null;
                            }
                        }
                    });
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                    return null;
                }
            }
            LazyMap<String, FieldAccessor> fields = new LazyMap<>(accessors);
            List<Lazy<AnnotationInfo>> annotations = new ArrayList<>();
            for (Annotation a : type.getAnnotations()) {
                annotations.add(annotationFromObject(a));
            }
            return new AnnotationInfo(new ClassName(type), fields, annotations, new Lazy<>((Annotation) o));
        });
    }

    public static Lazy<MethodInfo> buildMethod(CtMethod method, MethodAccessor accessor) { // TODO: transformers?
        return new Lazy<>(() -> {
            String name = method.getName();
            int modifiers = method.getModifiers();
            ClassName returns;
            try {
                returns = new ClassName(method.getReturnType().getName());
            } catch (NotFoundException e) {
                e.printStackTrace();
                return null;
            }
            List<Lazy<AnnotationInfo>> annotations = new ArrayList<>();
            Object[] realAnnotations = method.getAvailableAnnotations();
            for (Object a : realAnnotations) {
                annotations.add(annotationFromObject(a));
            }
            List<ParameterInfo> params = new ArrayList<>();
            CtClass[] parameterTypes;
            try {
                parameterTypes = method.getParameterTypes();
            } catch (NotFoundException e) {
                e.printStackTrace();
                return null;
            }
            for (int i = 0; i < parameterTypes.length; i++) {
                CtClass param = parameterTypes[i];
                List<Lazy<AnnotationInfo>> paramAnnotations = new ArrayList<>();
                try {
                    for (Object o : method.getParameterAnnotations()[i]) {
                        paramAnnotations.add(annotationFromObject(o));
                    }
                } catch (ClassNotFoundException e) {
                    e.printStackTrace();
                    return null;
                }
                params.add(new ParameterInfo("param" + i, // Can't get this
                        0, // Can't get this
                        new ClassName(param.getName()),
                        paramAnnotations));
            }
            return new MethodInfo(name, modifiers, returns, annotations, params, accessor);
        });
    }

    public static Lazy<FieldInfo> buildField(CtField field, FieldAccessor accessor) { // TODO: Transformers?
        return new Lazy<>(() -> {
            String name = field.getName();
            int modifiers = field.getModifiers();
            ClassName type;
            try {
                type = new ClassName(field.getType().getName());
            } catch (NotFoundException e) {
                e.printStackTrace();
                return null;
            }
            List<Lazy<AnnotationInfo>> annotations = new ArrayList<>();
            Object[] realAnnotations = field.getAvailableAnnotations();
            for (Object a : realAnnotations) {
                annotations.add(annotationFromObject(a));
            }
            return new FieldInfo(name, modifiers, type, annotations, accessor);
        });
    }

    public static Lazy<ClassInfo> buildClass(CtClass clazz,
                                             Function<CtMethod, MethodInfo> methodBuilder,
                                             Function<CtField, FieldInfo> fieldBuilder) {
        return new Lazy<>(() -> {
            ClassName name = new ClassName(clazz.getName());
            int modifiers = clazz.getModifiers();
            Map<String, FieldInfo> fields = new HashMap<>();
            Arrays.stream(clazz.getDeclaredFields()).forEach(f -> {
                fields.put(f.getName(), fieldBuilder.apply(f));
            });
            LazyMap<String, FieldInfo> lazyFields = new LazyMap<>(fields);
            Map<String, MethodInfo> methods = new HashMap<>();
            Arrays.stream(clazz.getDeclaredMethods()).forEach( m-> {
                methods.put(m.getName(), methodBuilder.apply(m));
            });
            LazyMap<String, MethodInfo> lazyMethods = new LazyMap<>(methods);
            List<Lazy<AnnotationInfo>> annotations = new ArrayList<>();
            Object[] realAnnotations = clazz.getAvailableAnnotations();
            for (Object a : realAnnotations) {
                annotations.add(annotationFromObject(a));
            }
            List<ClassName> extendsList = new ArrayList<>();
            try {
                for (CtClass e : clazz.getInterfaces()) {
                    extendsList.add(new ClassName(e.getName()));
                }
            } catch (NotFoundException e) {
                e.printStackTrace();
                return null;
            }
            try {
                extendsList.add(new ClassName(clazz.getSuperclass().getName()));
            } catch (NotFoundException e) {
                e.printStackTrace();
                return null;
            }
            Lazy<Class> transformed = new Lazy<>(() -> {
                try {
                    return clazz.toClass();
                } catch (CannotCompileException e) {
                    e.printStackTrace();
                    return null;
                }
            });
            return new ClassInfo(name, modifiers, lazyFields, lazyMethods, annotations, extendsList, transformed);
        });
    }
}
