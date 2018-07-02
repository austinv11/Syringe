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

import com.austinv11.syringe.direct.FieldIdentifier;
import com.austinv11.syringe.direct.MethodIdentifier;
import com.austinv11.syringe.direct.TypeSignature;
import com.austinv11.syringe.inject.InjectionTarget;
import com.austinv11.syringe.inject.clazz.AddFieldInjection;
import com.austinv11.syringe.inject.clazz.AddMethodInjection;
import com.austinv11.syringe.inject.sites.ClassSite;
import com.austinv11.syringe.util.Lazy;
import com.austinv11.syringe.visitor.ClassInjectionVisitor;
import com.austinv11.syringe.visitor.FieldInjectionVisitor;
import com.austinv11.syringe.visitor.InjectionVisitor;
import com.austinv11.syringe.visitor.MethodInjectionVisitor;
import org.objectweb.asm.*;
import org.objectweb.asm.util.CheckClassAdapter;
import org.objectweb.asm.util.TraceClassVisitor;

import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.util.*;

public class AsmVisitor extends ClassVisitor implements Opcodes {

    private final AsmSyringe syringe;
    private final Lazy<ClassSite> clazz;
    private final TypeSignature currType;
    private final Map<InjectionTarget, List<InjectionVisitor>> injectionVisitors = new HashMap<>();
    private final List<AddFieldInjection.FieldDeclaration> fieldsToInject = new LinkedList<>();
    private final List<AddMethodInjection.MethodDefinition> methodsToInject = new LinkedList<>();
    private final List<Runnable> visitCallbacks = new LinkedList<>();

    public AsmVisitor(ClassVisitor cv, AsmSyringe syringe, Class<?> clazz, Set<InjectionVisitor> injectionVisitors) {
        super(ASM6, "true".equals(System.getenv("trace")) ? new TraceClassVisitor(/*new CheckClassAdapter(*/cv/*)*/, new PrintWriter(System.out)) : cv);
        this.syringe = syringe;
        this.clazz = ClassSite.fromClass(clazz);
        this.currType = new TypeSignature(clazz);
        for (InjectionVisitor visitor : injectionVisitors) {
            if (visitor instanceof ClassInjectionVisitor) {
                if (!this.injectionVisitors.containsKey(InjectionTarget.CLASS))
                    this.injectionVisitors.put(InjectionTarget.CLASS, new LinkedList<>());
                this.injectionVisitors.get(InjectionTarget.CLASS).add(visitor);
            }
            if (visitor instanceof MethodInjectionVisitor) {
                if (!this.injectionVisitors.containsKey(InjectionTarget.METHOD))
                    this.injectionVisitors.put(InjectionTarget.METHOD, new LinkedList<>());
                this.injectionVisitors.get(InjectionTarget.METHOD).add(visitor);
            }
            if (visitor instanceof FieldInjectionVisitor) {
                if (!this.injectionVisitors.containsKey(InjectionTarget.FIELD))
                    this.injectionVisitors.put(InjectionTarget.FIELD, new LinkedList<>());
                this.injectionVisitors.get(InjectionTarget.FIELD).add(visitor);
            }
        }

        this.injectionVisitors.getOrDefault(InjectionTarget.CLASS, new LinkedList<>())
                              .stream()
                              .flatMap(v -> Arrays.stream(v.visit(this.clazz)))
                              .filter(i -> i instanceof AddFieldInjection)
                              .map(i -> (AddFieldInjection) i)
                              .map(i -> i.defineField(this.clazz))
                              .filter(Optional::isPresent)
                              .map(Optional::get)
                              .forEach(dec -> {
                                  //TODO: Support generic types
                                  if (Modifier.isStatic(dec.modifiers())) {
                                      Object initial = dec.callback(null);
                                      this.visitField(dec.modifiers(), dec.name(), dec.type().toString(), null, initial).visitEnd();
                                  } else {
                                      fieldsToInject.add(dec);
                                      this.visitField(Modifier.STATIC | Modifier.PRIVATE | Modifier.FINAL,
                                              String.format("f_callback_%d", dec.hashCode()),
                                              new TypeSignature(dec.getClass()).toString(),
                                              null,
                                              dec).visitEnd();
                                      this.visitField(dec.modifiers(), dec.name(), dec.type().toString(), null, null).visitEnd(); // Initial vals are ignored for instance fields
                                  }
                              });
        this.injectionVisitors.getOrDefault(InjectionTarget.CLASS, new LinkedList<>())
                              .stream()
                              .flatMap(v -> Arrays.stream(v.visit(this.clazz)))
                              .filter(i -> i instanceof AddMethodInjection)
                              .map(i -> (AddMethodInjection) i)
                              .map(i -> i.defineMethod(this.clazz))
                              .filter(Optional::isPresent)
                              .map(Optional::get)
                              .forEach(i -> {
                                  methodsToInject.add(i);
                                  for (MethodIdentifier method : i.preloadedMethods()) {
                                      syringe.defineClass(cw -> {
                                          cw.visit(V1_8,
                                                  ACC_SUPER | ACC_SYNTHETIC | ACC_PUBLIC,
                                                  Type.getInternalName(clazz) + "$Invokes_" + method.getName(),
                                                  "Ljava/lang/Object;Ljava/util/function/Function<[Ljava/lang/Object;Ljava/lang/Object;>;",
                                                  "java/lang/Object",
                                                  new String[]{"java/util/function/Function"});
                                          cw.visitSource("DUMMY.java", null);

                                          FieldVisitor fv = cw.visitField(ACC_PRIVATE + ACC_FINAL, "instance", "Ljava/lang/Object;", null, null);
                                          fv.visitEnd();

                                          MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "(Ljava/lang/Object;)V", null, null);
                                          mv.visitCode();
                                          mv.visitVarInsn(ALOAD, 0);
                                          mv.visitVarInsn(ALOAD, 1);
                                          mv.visitFieldInsn(PUTFIELD, Type.getInternalName(clazz) + "$Invokes_" + method.getName(), "instance", "Ljava/lang/Object;");
                                          mv.visitVarInsn(ALOAD, 0);
                                          mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
                                          mv.visitInsn(RETURN);
                                          mv.visitMaxs(2,2);
                                          mv.visitEnd();

                                          mv = cw.visitMethod(ACC_PUBLIC, "apply", "([Ljava/lang/Object;)" +
                                                  "Ljava/lang/Object;", null, null);
                                          mv.visitCode();
                                          int startStack = method.isStatic() ? 0 : 1;
                                          int currStack = startStack + 1;
                                          if (!method.isStatic()) {
                                              mv.visitFieldInsn(GETFIELD, Type.getInternalName(clazz) + "$Invokes_" + method.getName(), "instance", null);
                                              mv.visitVarInsn(ASTORE, currStack);
                                              mv.visitVarInsn(ALOAD, currStack);
                                          }
                                          int arrStack = 1;
                                          for (int j = 0; j < i.parameterTypes().length; j++) {
                                              mv.visitVarInsn(ALOAD, arrStack);
                                              mv.visitLdcInsn(currStack++);
                                              mv.visitInsn(AALOAD);
                                          }
                                          String sig = "(";
                                          for (TypeSignature param : method.getParams()) {
                                              sig += param.toString();
                                          }
                                          sig += ")" + method.getReturnType().toString();
                                          mv.visitMethodInsn(Modifier.isStatic(i.modifiers()) ? INVOKESTATIC :
                                                  INVOKEVIRTUAL, method.getOwner().toInternalString(), method.getName(), sig, false);
                                          mv.visitInsn(ARETURN);
                                          mv.visitMaxs(currStack, currStack);
                                          mv.visitEnd();
                                          cw.visitEnd();
                                      });

                                      visitCallbacks.add(() -> {
                                          this.visitInnerClass(Type.getInternalName(clazz) + "$Invokes_" + method.getName(), null, null, 0);
                                      });
                                  }
                              });
    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        visitCallbacks.forEach(Runnable::run);
    }

    @Override
    public ModuleVisitor visitModule(String name, int access, String version) {
        return super.visitModule(name, access, version);
    }

    @Override
    public void visitOuterClass(String owner, String name, String descriptor) {
        super.visitOuterClass(owner, name, descriptor);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        FV fv = new FV(super.visitField(access, name, descriptor, signature, value), access, name, descriptor, signature, value);
        return fv;
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[]
            exceptions) {
        MV mv = new MV(super.visitMethod(access, name, descriptor, signature, exceptions), access, name, descriptor, signature, exceptions);
        return mv;
    }

    @Override
    public void visitEnd() {
        methodsToInject.forEach(def -> {
            //TODO: Support generic types
            String[] exceptions = new String[def.throwTypes().length];
            for (int i = 0 ; i < exceptions.length; i++)
                exceptions[i] = def.throwTypes()[i].toString();
            String key = AsmConstantHolder.add(def);
            this.visitField(Modifier.STATIC | Modifier.PRIVATE | Modifier.FINAL,
                    String.format("m_callback_%d", def.hashCode()),
                    "Ljava/lang/String;",
                    null,
                    key).visitEnd();
            MethodVisitor mv = this.visitMethod(def.modifiers() | ACC_SYNTHETIC, def.name(), def.descriptor(), null, exceptions);
            mv.visitCode();
            int startStore = Modifier.isStatic(def.modifiers()) ? 0 : 1;
            int currStore = startStore + def.parameterTypes().length;
            mv.visitTypeInsn(NEW, "java/util/HashMap");
            int fieldMap = currStore++;
            mv.visitVarInsn(ASTORE, fieldMap);
            mv.visitVarInsn(ALOAD, fieldMap);
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
            for (FieldIdentifier f : def.preloadedFields()) {
                mv.visitTypeInsn(NEW, "com/austinv11/syringe/direct/DirectFieldAccessor");
                mv.visitVarInsn(ASTORE, currStore);
                mv.visitVarInsn(ALOAD, currStore);
                if (f.isStatic()) {
                    mv.visitFieldInsn(GETSTATIC, f.getOwner().toInternalString(), f.getName(), f.getType().toString());
                } else {
                    mv.visitVarInsn(ALOAD, 0);
                    mv.visitFieldInsn(GETFIELD, f.getOwner().toInternalString(), f.getName(), f.getType().toString());
                }
                mv.visitMethodInsn(INVOKESPECIAL, "com/austinv11/syringe/direct/DirectFieldAccessor", "<init>", "(Ljava/lang/Object;)V", false);
                mv.visitVarInsn(ALOAD, fieldMap);
                mv.visitLdcInsn(f.getName());
                mv.visitVarInsn(ALOAD, currStore);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
                mv.visitInsn(POP);
            }
            mv.visitTypeInsn(NEW, "java/util/HashMap");
            int methodMap = currStore++;
            mv.visitVarInsn(ASTORE, methodMap);
            mv.visitVarInsn(ALOAD, methodMap);
            mv.visitMethodInsn(INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
            for (MethodIdentifier m : def.preloadedMethods()) {
                mv.visitTypeInsn(NEW, m.getOwner().toInternalString() + "$Invokes_" + m.getName());
                mv.visitVarInsn(ASTORE, currStore);
                mv.visitVarInsn(ALOAD, currStore++);
                if (m.isStatic()) {
                    mv.visitInsn(ACONST_NULL);
                } else {
                    mv.visitVarInsn(ALOAD, 0);
                }
                mv.visitMethodInsn(INVOKESPECIAL, m.getOwner().toInternalString() + "$Invokes_" + m.getName(), "<init>", "(Ljava/lang/Object;)V", false);
                mv.visitTypeInsn(NEW, "com/austinv11/syringe/direct/DirectMethodAccessor");
                mv.visitVarInsn(ASTORE, currStore);
                mv.visitVarInsn(ALOAD, currStore);
                mv.visitVarInsn(ALOAD, --currStore);
                mv.visitMethodInsn(INVOKESPECIAL, "com/austinv11/syringe/direct/DirectMethodAccessor", "<init>", "(Ljava/util/function/Function;)V", false);
                mv.visitVarInsn(ALOAD, methodMap);
                mv.visitLdcInsn(m.getName());
                mv.visitVarInsn(ALOAD, currStore);
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", true);
                mv.visitInsn(POP);
            }

            int arrStore = currStore++;
            mv.visitLdcInsn(def.parameterTypes().length);
            mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
            mv.visitVarInsn(ASTORE, arrStore);
            for (int i = 0; i < def.parameterTypes().length; i++) {
                mv.visitVarInsn(ALOAD, arrStore);
                mv.visitLdcInsn(i);
                mv.visitVarInsn(ALOAD, i);
                mv.visitInsn(AASTORE);
            }

            mv.visitFieldInsn(GETSTATIC, currType.toInternalString(),
                    String.format("m_callback_%d", def.hashCode()),
                    "Ljava/lang/String;");

            mv.visitMethodInsn(INVOKESTATIC, Type.getInternalName(AsmConstantHolder.class), "get", "(Ljava/lang/String;)Ljava/lang/Object;", false);
            mv.visitTypeInsn(CHECKCAST, Type.getInternalName(def.getClass()));

            if (Modifier.isStatic(def.modifiers())) {
                mv.visitInsn(ACONST_NULL);
            } else {
                mv.visitVarInsn(ALOAD, 0);
            }
            mv.visitVarInsn(ALOAD, arrStore);
            mv.visitVarInsn(ALOAD, fieldMap);
            mv.visitVarInsn(ALOAD, methodMap);
            mv.visitMethodInsn(INVOKEVIRTUAL, Type.getInternalName(def.getClass()), "callback",
                    "(Ljava/lang/Object;[Ljava/lang/Object;Ljava/util/Map;Ljava/util/Map;)Ljava/lang/Object;",
                    false);
            int retStore = currStore++;
            mv.visitVarInsn(ASTORE, retStore);

            for (FieldIdentifier f : def.preloadedFields()) {
                mv.visitVarInsn(ALOAD, fieldMap);
                mv.visitLdcInsn(f.getName());
                mv.visitMethodInsn(INVOKEINTERFACE, "java/util/Map", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", true);
                mv.visitTypeInsn(CHECKCAST, f.getType().toInternalString());
                mv.visitFieldInsn(f.isStatic() ? PUTSTATIC : PUTFIELD, currType.toInternalString(), f.getName(), f.getType().toString());
            }
            mv.visitVarInsn(ALOAD, retStore);
            mv.visitTypeInsn(CHECKCAST, def.returnType().toInternalString());
            mv.visitInsn(ARETURN);
            mv.visitMaxs(currStore, currStore);
            mv.visitEnd();
        });
        super.visitEnd();
    }

    private class MV extends MethodVisitor {

        private final int access;
        private final String name;
        private final String descriptor;
        private final String signature;
        private final String[] exceptions;

        private int loadDepth;
        private boolean didCallThis = false;

        public MV(MethodVisitor mv, int access, String name, String descriptor, String signature, String[] exceptions) {
            super(ASM6, mv);
            this.access = access;
            this.name = name;
            this.descriptor = descriptor;
            this.signature = signature;
            this.exceptions = exceptions;
        }

        @Override
        public void visitVarInsn(int opcode, int var) {
            if (opcode == ALOAD) { //Track the free var
                loadDepth = Math.max(loadDepth, var);
            }
            super.visitVarInsn(opcode, var);
        }

        @Override
        public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean isInterface) {
            //Check for this(); calls
            if (opcode == INVOKESPECIAL && owner.equals(currType.toString()) && name.equals("<init>")) {
                didCallThis = true;
            }
            super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
        }

        @Override
        public void visitEnd() {
            if (name.equals("<init>") && !didCallThis) {
                for (AddFieldInjection.FieldDeclaration dec : fieldsToInject) {
                    //<var> = f_callback_<hashcode>.callback(this);
                    this.visitVarInsn(ALOAD, 0);
                    this.visitFieldInsn(GETSTATIC, currType.toString(),
                            String.format("f_callback_%d", dec.hashCode()),
                            new TypeSignature(dec.getClass()).toString());
                    this.visitFieldInsn(PUTFIELD, currType.toString(), dec.name(), dec.type().toString());
                }
            }
            super.visitEnd();
        }
    }

    private class FV extends FieldVisitor {

        private final int access;
        private final String name;
        private final String descriptor;
        private final String signature;
        private final Object value;

        public FV(FieldVisitor fv, int access, String name, String descriptor, String signature, Object value) {
            super(ASM6, fv);
            this.access = access;
            this.name = name;
            this.descriptor = descriptor;
            this.signature = signature;
            this.value = value;
        }
    }
}
