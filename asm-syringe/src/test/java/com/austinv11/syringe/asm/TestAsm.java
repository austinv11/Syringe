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

package com.austinv11.syringe.asm;

import com.austinv11.syringe.asm.loaders.InstrumentationByteCodeLoader;
import org.junit.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

public class TestAsm {

    public static void main(String[] args) throws NoSuchMethodException, IllegalAccessException,
            InvocationTargetException {
        InstrumentationByteCodeLoader.main(args);
        new TestAsm().test();
    }

    @Test
    public void test() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        TestClass tc = new TestClass(); //Force class loading
        Method m = tc.getClass().getMethod("generated", String.class);
        m.setAccessible(true);
        System.out.println(m.invoke(null, "Ayy"));
    }

}
