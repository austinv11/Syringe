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
import com.austinv11.syringe.util.Property;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class AnnotationInfo {

    private final ClassSite type;
    private final Property[] properties;

    public static Lazy<AnnotationInfo[]> fromAnnotatedElement(AnnotatedElement element) {
        return new Lazy<>(() -> {
            AnnotationInfo[] array = new AnnotationInfo[element.getAnnotations().length];
            Annotation[] as = element.getAnnotations();
            for (int i = 0; i < as.length; i++) {
                Annotation a = as[i];
                Property[] properties = new Property[a.getClass().getDeclaredMethods().length];
                Method[] declaredMethods = a.getClass().getDeclaredMethods();
                for (int j = 0; j < declaredMethods.length; j++) {
                    Method m = declaredMethods[j];
                    m.setAccessible(true);
                    try {
                        properties[j] = new Property(ClassSite.fromClass(m.getReturnType()).get(), true, m.invoke(a));
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        e.printStackTrace();
                        return null;
                    }
                    array[i] = new AnnotationInfo(ClassSite.fromClass(a.getClass()).get(), properties);
                }
            }
            return array;
        });
    }

    public AnnotationInfo(ClassSite type, Property[] properties) {
        this.type = type;
        this.properties = properties;
    }

    public ClassSite getType() {
        return type;
    }

    public Property[] getProperties() {
        return properties;
    }
}
