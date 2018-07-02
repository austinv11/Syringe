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

import com.austinv11.syringe.Syringe;
import com.austinv11.syringe.SyringeService;
import com.austinv11.syringe.asm.loaders.NewClassByteCodeLoader;
import com.austinv11.syringe.util.IncompatibleConfigurationException;
import com.austinv11.syringe.util.services.InjectionServiceLoader;
import com.austinv11.syringe.visitor.InjectionVisitor;
import com.google.auto.service.AutoService;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.util.TraceClassVisitor;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Consumer;

/**
 * This is a {@link com.austinv11.syringe.SyringeService} implementation backed by
 * <a href="https://asm.ow2.io/">ASM</a>. This is the preferred runtime-oriented syringe implementation.
 */
@AutoService(SyringeService.class)
public class AsmSyringe implements SyringeService {

    public static final Syringe SYRINGE = new Syringe("ASM-Syringe", "1.0", "ASM Instrumentation");

    private final ByteCodeLoader loader;
    protected final Set<InjectionVisitor> visitors = new LinkedHashSet<>();

    public AsmSyringe() {
        this(new NewClassByteCodeLoader());
    }

    public AsmSyringe(ByteCodeLoader loader) {
        this.loader = loader;
        InjectionServiceLoader.visitors().forEach(this::addVisitor);
    }

    @Override
    public Syringe getSyringe() {
        return SYRINGE;
    }

    @Override
    public void addVisitor(InjectionVisitor visitor) {
        visitors.add(visitor);
    }

    @Override
    public <T> Class visit(Class<T> clazz) throws IncompatibleConfigurationException {
        try {
            return process(clazz);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public <T> T visit(T obj) throws IncompatibleConfigurationException {
        if (!loader.inPlace())
            throw new IncompatibleConfigurationException("The current loader does not support in-place reloading!");
        visit(obj.getClass());
        return obj;
    }

    public byte[] visit(byte[] bytecode, Class<?> clazz) {
        ClassReader reader = new ClassReader(bytecode);
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        AsmVisitor asmVisitor = new AsmVisitor(writer, this, clazz, visitors);
        reader.accept(asmVisitor, ClassReader.SKIP_FRAMES);
        return writer.toByteArray();
    }

    public Class<?> process(Class<?> clazz) throws IOException {
        ClassReader reader = new ClassReader(clazz.getName());
        ClassWriter writer = new ClassWriter(reader, ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        AsmVisitor asmVisitor = new AsmVisitor(writer, this, clazz, visitors);
        reader.accept(asmVisitor, ClassReader.SKIP_FRAMES);
        return loader.load(writer.toByteArray(), clazz);
    }

    public void defineClass(Consumer<ClassVisitor> callback) {
        ClassWriter writer = new ClassWriter(ClassWriter.COMPUTE_FRAMES | ClassWriter.COMPUTE_MAXS);
        if ("true".equals(System.getenv("trace")))
            callback.accept(new TraceClassVisitor(writer, new PrintWriter(System.out)));
        else
            callback.accept(writer);
        loader.load(writer.toByteArray());
    }
}
