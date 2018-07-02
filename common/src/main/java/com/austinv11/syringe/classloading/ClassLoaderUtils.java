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
package com.austinv11.syringe.classloading;

import com.austinv11.syringe.util.Lazy;

import java.lang.reflect.Field;
import java.util.Vector;

/**
 * A set of utility methods related to classloading.
 */
public class ClassLoaderUtils {

    private static final Lazy<Field> scl = new Lazy<>(() -> {
        Field scl;
        try {
            scl = ClassLoader.class.getDeclaredField("scl");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        scl.setAccessible(true);
        return scl;
    });

    private static final Lazy<Field> classes = new Lazy<>(() -> {
        Field classes;
        try {
            classes = ClassLoader.class.getDeclaredField("classes");
        } catch (NoSuchFieldException e) {
            throw new RuntimeException(e);
        }
        classes.setAccessible(true);
       return classes;
    });

    /**
     * Overrides the system class loader at runtime. Note: This is only likely to work on oracle/openjdk jvm
     * implementations. So it is preferred to use the {@code -Djava.system.class.loader=myClassLoader} switch
     * when starting java instead for ensured compatibility.
     *
     * @param newLoader The new loader to override with.
     * @return The original system classloader instance.
     */
    public static final ClassLoader overrideSystemClassLoader(ClassLoader newLoader) {
        return overrideSystemClassLoader(newLoader, false);
    }

    /**
     * Overrides the system class loader at runtime. Note: This is only likely to work on oracle/openjdk jvm
     * implementations. So it is preferred to use the {@code -Djava.system.class.loader=myClassLoader} switch
     * when starting java instead for ensured compatibility.
     *
     * @param newLoader The new loader to override with.
     * @param reloadClasses Whether to attempt to reload already loaded classes or not (default=false).
     * @return The original system classloader instance.
     */
    public static final ClassLoader overrideSystemClassLoader(ClassLoader newLoader, boolean reloadClasses) {
        ClassLoader old = ClassLoader.getSystemClassLoader();

        try {
            scl.get().set(null, newLoader);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }

        if (reloadClasses) {
            try {
                for (Class<?> clazz : (Vector<Class<?>>) classes.get().get(old)) {
                    try {
                        newLoader.loadClass(clazz.getName());
                    } catch (ClassNotFoundException e) {}
                }
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }
        }

        return old;
    }

    /**
     * Reflects to retrieve the current system class loader.
     *
     * @return The current system {@link java.lang.ClassLoader} instance.
     */
    public static final ClassLoader getSystemClassLoader() {
        try {
            return (ClassLoader) scl.get().get(null);
        } catch (IllegalAccessException e) {
            throw new RuntimeException(e);
        }
    }
}
