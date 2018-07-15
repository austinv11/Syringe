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

package syringe.asm.test;

import syringe.access.FieldAccessor;
import syringe.util.LazyMap;
import syringe.access.MethodAccessor;
import syringe.asm.AsmSyringe;
import syringe.asm.util.OpenClassLoader;
import syringe.callbacks.clazz.ClassDefinitionCallback;
import syringe.callbacks.clazz.ClassInitializerCallback;
import syringe.info.ClassInfo;
import syringe.visitor.ClassVisitor;

import java.util.Optional;

public class Test {

    @org.junit.Test
    public void test() throws ClassNotFoundException, IllegalAccessException, InstantiationException {
        OpenClassLoader ocl = new OpenClassLoader(Thread.currentThread().getContextClassLoader());
        Thread.currentThread().setContextClassLoader(ocl);
        AsmSyringe syringe = new AsmSyringe(ocl);
        syringe.addVisitor(new ClassVisitor() {
            @Override
            public Optional<ClassInitializerCallback> classInit() {
                return Optional.of(new ClassInitializerCallback() {
                    @Override
                    public void classInit(ClassInfo clazz, boolean staticContext, LazyMap<String, FieldAccessor> fields, LazyMap<String, MethodAccessor> methods) {
                        System.out.println("Init!");
                    }
                });
            }

            @Override
            public Optional<ClassDefinitionCallback> classConstruct() {
                return Optional.of(new ClassDefinitionCallback() {
                    @Override
                    public void construction(ClassInfo clazz) {
                        System.out.println("Construction!");
                    }
                });
            }
        });
        syringe.inject();
        Class<?> clazz = ocl.loadClass("syringe.asm.test.Test2");
        Test2 t2 = (Test2) clazz.newInstance();
        System.out.println(); //TODO fix
    }
}
