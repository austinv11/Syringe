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

package com.austinv11.syringe.asm.loaders;

import com.austinv11.syringe.asm.ByteCodeLoader;
import com.austinv11.syringe.classloading.ClassLoaderUtils;

import javax.annotation.Nullable;
import java.net.URL;
import java.net.URLClassLoader;

//TODO: Better in place support
public class NewClassByteCodeLoader implements ByteCodeLoader {

    private final ClassDefiningClassLoader loader;

    public NewClassByteCodeLoader() {
        //Bootstrap
        ClassLoader old = ClassLoaderUtils.getSystemClassLoader();
        loader = new ClassDefiningClassLoader(old);
        ClassLoaderUtils.overrideSystemClassLoader(loader, true);
    }

    @Override
    public Class<?> load(byte[] bytecode) {
        return loader.define(null, bytecode);
    }

    @Override
    public Class<?> load(byte[] bytecode, Class<?> currClass) {
        return loader.define(currClass.getName(), bytecode);
    }

    @Override
    public boolean inPlace() {
        return false;
    }

    public final static class ClassDefiningClassLoader extends URLClassLoader {

        private final ClassLoader parent;

        public ClassDefiningClassLoader(ClassLoader parent) {
            super(parent instanceof URLClassLoader ? ((URLClassLoader) parent).getURLs() : new URL[0], parent);
            this.parent = parent;
        }

        @Nullable
        private Class<?> define(@Nullable String name, byte[] bytecode) {
            if (name != null && name.startsWith("java."))
                return null;
            try {
                return super.defineClass(name, bytecode, 0, bytecode.length);
            } catch (NoClassDefFoundError e) {
                try {
                    return parent.loadClass(name);
                } catch (ClassNotFoundException e1) {
                    return null;
                }
            }
        }

        @Override
        public Class<?> loadClass(String name) throws ClassNotFoundException {
            Class<?> c = findLoadedClass(name);
            if (c == null || c.getClassLoader() != this) {
                if (c != null && c.getClassLoader() instanceof URLClassLoader) {
                    for (URL url : ((URLClassLoader) c.getClassLoader()).getURLs()) {
                        addURL(url);
                    }
                }
                try {
                    c = findClass(name);
                } catch (ClassNotFoundException e) {
                    if (c == null) {
                        if (name.contains("$"))
                            return loadClass(name.replace("$", "."));
                        throw e;
                    }
                }
            }
            return c;
        }

        @Override
        protected Class<?> loadClass(String name, boolean resolve) throws ClassNotFoundException {
            return super.loadClass(name, resolve);
        }
    }
}
