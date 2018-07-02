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

import com.austinv11.syringe.asm.AsmSyringe;
import com.austinv11.syringe.asm.ByteCodeLoader;
import net.bytebuddy.agent.ByteBuddyAgent;

import javax.annotation.Nullable;
import java.lang.instrument.*;
import java.security.ProtectionDomain;
import java.util.concurrent.atomic.AtomicReference;

public class InstrumentationByteCodeLoader implements ByteCodeLoader, ClassFileTransformer {

    private static final AtomicReference<InstrumentationByteCodeLoader> loader = new AtomicReference<>();

    public static void premain(String agentArgs, Instrumentation inst) {
        loader.set(new InstrumentationByteCodeLoader(inst));
    }

    public static void agentmain(String agentArgs, Instrumentation inst) {
        loader.set(new InstrumentationByteCodeLoader(inst));
    }

    public static void main(String[] args) {
        agentmain("", ByteBuddyAgent.install());
    }

    private final Instrumentation inst;
    private final LiteClassLoader cl = new LiteClassLoader();
    private final AsmSyringe syringe;

    public InstrumentationByteCodeLoader(Instrumentation inst) {
        this.inst = inst;
        inst.addTransformer(this, true);
        this.syringe = new AsmSyringe(this);
    }

    @Override
    public Class<?> load(byte[] bytecode) {
        return cl.define(null, bytecode);
    }

    @Override
    public Class<?> load(byte[] bytecode, Class<?> currClass) {
        try {
            inst.redefineClasses(new ClassDefinition(currClass, bytecode));
        } catch (ClassNotFoundException | UnmodifiableClassException e) {
            throw new RuntimeException(e);
        }
        return currClass;
    }

    @Override
    public boolean inPlace() {
        return true;
    }

    @Override
    public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain, byte[] classfileBuffer) {
        if (classBeingRedefined == null) {
            try {
                if (className.startsWith("java/"))
                    return classfileBuffer;
                classBeingRedefined = cl.define(className+"$TEMP", classfileBuffer);
            } catch (Throwable t) {
                t.printStackTrace();
                throw t;
            }
        }
        return syringe.visit(classfileBuffer, classBeingRedefined);
    }

    private static final class LiteClassLoader extends ClassLoader {

        @Nullable
        private Class<?> define(@Nullable String name, byte[] bytecode) {
            if (name != null && name.startsWith("java."))
                return null;
            return super.defineClass(name, bytecode, 0, bytecode.length);
        }
    }
}
