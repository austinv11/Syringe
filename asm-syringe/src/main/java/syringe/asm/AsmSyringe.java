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

package syringe.asm;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;
import syringe.Syringe;
import syringe.asm.util.OpenClassLoader;
import syringe.util.CallbackCollector;
import syringe.visitor.ClassVisitor;
import syringe.visitor.MethodVisitor;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashSet;
import java.util.Set;

public class AsmSyringe implements Syringe { //TODO

    private final Set<ClassVisitor> classVisitors = new HashSet<>();
    private final Set<MethodVisitor> methodVisitors = new HashSet<>();

    private final OpenClassLoader ocl;

    public AsmSyringe(OpenClassLoader ocl) {
        this.ocl = ocl;
    }

    @Override
    public void inject() {
        CallbackCollector cc = new CallbackCollector(classVisitors, methodVisitors);
        ClassReader reader = null;
        try {
            reader = new ClassReader("syringe.asm.test.Test2");
        } catch (IOException e) {
            e.printStackTrace();
        }
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        SyringeClassVisitor scv = new SyringeClassVisitor(new TraceClassVisitor(writer, new PrintWriter(System.out)), cc, () -> {
            return new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        });
        reader.accept(scv, ClassReader.SKIP_FRAMES);
        scv.getNeedsLoading().forEach((name, bytecode) -> ocl.define(name.replace("/", "."), bytecode, true));
        ocl.define("syringe.asm.test.Test2", writer.toByteArray(), false);
    }

    @Override
    public void addVisitor(ClassVisitor cv) {
        classVisitors.add(cv);
    }

    @Override
    public void addVisitor(MethodVisitor mv) {
        methodVisitors.add(mv);
    }
}
