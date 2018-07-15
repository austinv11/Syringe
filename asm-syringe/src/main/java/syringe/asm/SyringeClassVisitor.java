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

import org.objectweb.asm.*;
import syringe.access.FieldAccessor;
import syringe.util.LazyMap;
import syringe.access.MethodAccessor;
import syringe.callbacks.clazz.ClassAnnotationCallback;
import syringe.callbacks.clazz.ClassDefinitionCallback;
import syringe.callbacks.clazz.ClassInitializerCallback;
import syringe.info.*;
import syringe.util.CallbackCollector;
import syringe.util.ClassName;
import syringe.util.Lazy;
import syringe.util.RandomNameGenerator;

import javax.annotation.Nullable;
import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

//TODO: wrap primitives with their wrappers (i.e. Integer.valueOf) when passed to hooks
//TODO: wire up: ClassInitializerCallback, FieldDefinitionCallback, MethodDefinitionCallback,
//TODO: ExceptionThrownCallback, MethodAnnotationCallback, MethodInvocationCallback, MethodReturnCallback
public final class SyringeClassVisitor extends ClassVisitor implements Opcodes {

    private final CallbackCollector cc;
    private final Map<String, String> queuedInitializers = new HashMap<>();
    private final List<Consumer<MethodVisitor>> queuedInstanceInitializers = new ArrayList<>();

    private final Supplier<ClassWriter> newClassGenerator;

    private boolean clinitCalled;
    private String selfInfoAddress;
    private final List<String> classInitializerCallbacks = new ArrayList<>();

    private ClassName selfName;
    private Lazy<Class> classTransformer;
    private int modifiers;
    private final Map<String, Lazy<FieldInfo>> fields = new HashMap<>();
    private final Map<String, Lazy<MethodInfo>> methods = new HashMap<>();
    private final List<AnnotationInfo> annotations = new ArrayList<>();
    private final List<ClassName> extendsList = new ArrayList<>();

    private final Map<String, byte[]> needsLoading = new HashMap<>();
    private final Map<String, String> field2AccessorType = new HashMap<>();
    private final Map<String, String> method2AccessorType = new HashMap<>();

    public SyringeClassVisitor(ClassVisitor cv, CallbackCollector cc, Supplier<ClassWriter> newClassGenerator) {
        super(ASM6, cv);
        this.cc = cc;
        this.newClassGenerator = newClassGenerator;
    }

    public Map<String, byte[]> getNeedsLoading() {
        return needsLoading;
    }

    public String addStaticField(@Nullable Object instance) {
        return this.addStaticField(instance == null ? Object.class : instance.getClass(), instance);
    }

    /**
     * Adds a static field to the current class holding the specified object.
     *
     * @return The (arbitrary) name of the generated static field.
     */
    public String addStaticField(Class<?> fieldType, @Nullable Object instance) {
        String name = RandomNameGenerator.generate(fieldType) + "_" + Objects.hashCode(instance);
        GlobalRegistry.register(name, instance);
        boolean primitive = fieldType.isPrimitive() || fieldType.equals(String.class);
        String type = new ClassName(fieldType).getAsInternalTypeName();
        FieldVisitor fv = visitField(ACC_PRIVATE | ACC_STATIC | ACC_FINAL | ACC_SYNTHETIC, name,
                type,
                null,
                primitive ? instance : null);
        if (!primitive) { //Need to initialize by other means if not primitive or String
            GlobalRegistry.register(name, instance);
            queuedInitializers.put(name, type); //@see visitMethod -> <clinit>
        }
        fv.visitEnd();
        return name;
    }

    public void addStaticField(Class<?> fieldType, String location) {
        queuedInitializers.put(location, new ClassName(fieldType).getAsInternalTypeName());
    }

    public String newFieldAccessorClass(FieldInfo target) {
        String name = "ASMSyringe$$InnerClass$$" + RandomNameGenerator.generate();
        String innerName = selfName.getAsSlashNotation() + "$" + name;

        ClassWriter cv = newClassGenerator.get();
        boolean isStatic = Modifier.isStatic(target.getModifiers());
        visitInnerClass(innerName, selfName.getAsSlashNotation(), name, ACC_PUBLIC | (isStatic ? ACC_STATIC : 0));

        String fieldName = target.getName() + "$Accessor";

        FieldVisitor fv = visitField((isStatic ? ACC_STATIC : 0) | ACC_SYNTHETIC, fieldName, new ClassName(innerName).getAsInternalTypeName(), null, null);
        fv.visitEnd();

        if (isStatic) {
            queuedInitializers.put(fieldName, new ClassName(innerName).getAsInternalTypeName());
        } else {
            queuedInstanceInitializers.add(mv -> {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitTypeInsn(NEW, innerName);
                mv.visitInsn(DUP);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESPECIAL, innerName, "<init>", "(" + selfName.getAsInternalTypeName() + ")V", false);
                mv.visitFieldInsn(PUTFIELD, selfName.getAsSlashNotation(), fieldName, new ClassName(innerName).getAsInternalTypeName());
            });
        }

        cv.visit(V1_8, ACC_PUBLIC | ACC_SUPER, innerName, null, "java/lang/Object", new String[]{new ClassName(FieldAccessor.class).getAsSlashNotation()});
        cv.visitSource("DUMMY.java", null);
        cv.visitInnerClass(innerName, selfName.getAsSlashNotation(), name, ACC_PUBLIC | (isStatic ? ACC_STATIC : 0));

        if (!isStatic) {
            fv = cv.visitField(ACC_FINAL | ACC_SYNTHETIC, "this$0", selfName.getAsInternalTypeName(), null, null);
            fv.visitEnd();
        }

        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        if (!isStatic) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(PUTFIELD, innerName, "this$0", selfName.getAsInternalTypeName());
        }
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLocalVariable("this", new ClassName(innerName).getAsInternalTypeName(), null, l0, l1, 0);
        if (!isStatic)
            mv.visitLocalVariable("this$0", "Ltrash/Test;", null, l0, l1, 1);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = cv.visitMethod(ACC_PUBLIC, "get", "()Ljava/lang/Object;", null, null);
        mv.visitCode();
        l0 = new Label();
        mv.visitLabel(l0);
        if (!isStatic) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, innerName, "this$0", selfName.getAsInternalTypeName());
            mv.visitFieldInsn(GETFIELD, selfName.getAsSlashNotation(), target.getName(), target.getType().getAsInternalTypeName());
        } else {
            mv.visitFieldInsn(GETSTATIC, selfName.getAsSlashNotation(), target.getName(), target.getType().getAsInternalTypeName());
        }
        mv.visitInsn(Opcodes.ARETURN);
        l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLocalVariable("this", new ClassName(innerName).getAsInternalTypeName(), null, l0, l1, 0);
        mv.visitMaxs(1, 1);
        mv.visitEnd();

        cv.visitEnd();

        needsLoading.put(innerName, cv.toByteArray());
        field2AccessorType.put(target.getName(), "L" + innerName + ";");
        return fieldName;
    }

    /**
     * Adds a non-static field to the current class holding the specified object.
     *
     * @return The (arbitrary) name of the generated field.
     */
    public String addInstanceField(Class<?> fieldType, BiConsumer<String, MethodVisitor> callback) {
        String name = RandomNameGenerator.generate(fieldType) + "_instance_" + Objects.hashCode(callback);
        String type = new ClassName(fieldType).getAsInternalTypeName();
        FieldVisitor fv = visitField(ACC_PRIVATE | ACC_FINAL | ACC_SYNTHETIC, name, type, null, null);
        fv.visitEnd();
        queuedInstanceInitializers.add(mv -> callback.accept(name, mv));
        return name;
    }

    public String newMethodAccessorClass(MethodInfo target) {
        //TODO: this is copy pasted from field
        String name = "ASMSyringe$$InnerClass$$" + RandomNameGenerator.generate();
        String innerName = selfName.getAsSlashNotation() + "$" + name;

        ClassWriter cv = newClassGenerator.get();
        boolean isStatic = Modifier.isStatic(target.getModifiers());
        visitInnerClass(innerName, selfName.getAsSlashNotation(), name, ACC_PUBLIC | (isStatic ? ACC_STATIC : 0));

        String methodName = target.getName() + target.getInternalTypeSignature().replace(";", "").replace("(", "").replace(")", "").replace("/", "") + "$Accessor";

        FieldVisitor fv = visitField((isStatic ? ACC_STATIC : 0) | ACC_SYNTHETIC, methodName, new ClassName(innerName).getAsInternalTypeName(), null, null);
        fv.visitEnd();

        if (isStatic) {
            queuedInitializers.put(methodName, new ClassName(innerName).getAsInternalTypeName());
        } else {
            queuedInstanceInitializers.add(mv -> {
                mv.visitVarInsn(ALOAD, 0);
                mv.visitTypeInsn(NEW, innerName);
                mv.visitInsn(DUP);
                mv.visitVarInsn(ALOAD, 0);
                mv.visitMethodInsn(INVOKESPECIAL, innerName, "<init>", "(" + selfName.getAsInternalTypeName() + ")V", false);
                mv.visitFieldInsn(PUTFIELD, selfName.getAsSlashNotation(), methodName, new ClassName(innerName).getAsInternalTypeName());
            });
        }

        cv.visit(V1_8, ACC_PUBLIC | ACC_SUPER, innerName, null, "java/lang/Object", new String[]{new ClassName(FieldAccessor.class).getAsSlashNotation()});
        cv.visitSource("DUMMY.java", null);
        cv.visitInnerClass(innerName, selfName.getAsSlashNotation(), name, ACC_PUBLIC | (isStatic ? ACC_STATIC : 0));

        if (!isStatic) {
            fv = cv.visitField(ACC_FINAL | ACC_SYNTHETIC, "this$0", selfName.getAsInternalTypeName(), null, null);
            fv.visitEnd();
        }

        MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        Label l0 = new Label();
        mv.visitLabel(l0);
        if (!isStatic) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitFieldInsn(PUTFIELD, innerName, "this$0", selfName.getAsInternalTypeName());
        }
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Object", "<init>", "()V", false);
        mv.visitInsn(RETURN);
        Label l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLocalVariable("this", new ClassName(innerName).getAsInternalTypeName(), null, l0, l1, 0);
        if (!isStatic)
            mv.visitLocalVariable("this$0", "Ltrash/Test;", null, l0, l1, 1);
        mv.visitMaxs(2, 2);
        mv.visitEnd();

        mv = cv.visitMethod(ACC_PUBLIC | ACC_VARARGS, "invoke", "([Ljava/lang/Object;)Ljava/lang/Object;", null, new String[]{"java/lang/Throwable"});
        mv.visitCode();
        l0 = new Label();
        mv.visitLabel(l0);
        if (!isStatic) {
            mv.visitVarInsn(ALOAD, 0);
            mv.visitFieldInsn(GETFIELD, innerName, "this$0", selfName.getAsInternalTypeName());
        }
        for (int i = 0; i < target.getParams().size(); i++) {
            mv.visitVarInsn(ALOAD, 1);
            mv.visitLdcInsn(i);
            mv.visitInsn(AALOAD);
        }
        mv.visitMethodInsn(isStatic ? INVOKESTATIC : INVOKEVIRTUAL, selfName.getAsSlashNotation(), target.getName(), target.getInternalTypeSignature(), false);
        if (target.getReturnType().getAsInternalTypeName().equals("V"))
            mv.visitInsn(ACONST_NULL);
        mv.visitInsn(Opcodes.ARETURN);
        l1 = new Label();
        mv.visitLabel(l1);
        mv.visitLocalVariable("this", new ClassName(innerName).getAsInternalTypeName(), null, l0, l1, 0);
        mv.visitLocalVariable("args", "[Ljava/lang/Object;", null, l0, l1, 1);
        mv.visitMaxs(3, 2);
        mv.visitEnd();

        cv.visitEnd();

        needsLoading.put(innerName, cv.toByteArray());
        method2AccessorType.put(target.getName() + target.getInternalTypeSignature(), "L" + innerName + ";");
        return methodName;
    }

    public void generateFieldAndMethodMaps(boolean fromStaticContext, MethodVisitor mv, int stackOffset) {
        ClassName lazyMap = new ClassName(LazyMap.class);

        mv.visitLabel(new Label());
        mv.visitTypeInsn(Opcodes.NEW, "java/util/HashMap");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
        int baseIndex = (fromStaticContext ? 0 : 1) + stackOffset;
        mv.visitVarInsn(ASTORE, baseIndex);
        fields.forEach((n, f) -> {
            boolean isStatic = Modifier.isStatic(f.get().getModifiers());
            if (!(isStatic && !fromStaticContext)) {
                mv.visitVarInsn(ALOAD, baseIndex);
                mv.visitLdcInsn(n);
                String type = field2AccessorType.get(f.get().getName());
                mv.visitFieldInsn(isStatic ? GETSTATIC : GETFIELD, selfName.getAsSlashNotation(), n + "$Accessor", type);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/HashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
                mv.visitInsn(POP);
            }
        });
        mv.visitTypeInsn(NEW, lazyMap.getAsSlashNotation());
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(ALOAD, baseIndex);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, lazyMap.getAsSlashNotation(), "<init>", "(Ljava/util/Map;)V", false);
        mv.visitVarInsn(ASTORE, baseIndex); // We don't need the old map

        mv.visitLabel(new Label());
        mv.visitTypeInsn(Opcodes.NEW, "java/util/HashMap");
        mv.visitInsn(Opcodes.DUP);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, "java/util/HashMap", "<init>", "()V", false);
        mv.visitVarInsn(ASTORE, baseIndex+1);
        methods.forEach((n, m) -> {
            boolean isStatic = Modifier.isStatic(m.get().getModifiers());
            if (!(isStatic && !fromStaticContext)) {
                mv.visitVarInsn(ALOAD, baseIndex+1);
                mv.visitLdcInsn(n);
                String methodName = m.get().getName() + m.get().getInternalTypeSignature().replace(";", "").replace("(", "").replace(")", "").replace("/", "") + "$Accessor";
                String type = method2AccessorType.get(methodName + m.get().getInternalTypeSignature());
                mv.visitFieldInsn(isStatic ? GETSTATIC : GETFIELD, selfName.getAsSlashNotation(), methodName, type);
                mv.visitMethodInsn(INVOKEVIRTUAL, "java/util/HashMap", "put", "(Ljava/lang/Object;Ljava/lang/Object;)Ljava/lang/Object;", false);
                mv.visitInsn(POP);
            }
        });
        mv.visitTypeInsn(NEW, lazyMap.getAsSlashNotation());
        mv.visitInsn(Opcodes.DUP);
        mv.visitVarInsn(ALOAD, baseIndex+1);
        mv.visitMethodInsn(Opcodes.INVOKESPECIAL, lazyMap.getAsSlashNotation(), "<init>", "(Ljava/util/Map;)V", false);
        mv.visitVarInsn(ASTORE, baseIndex+1); // We don't need the old map

    }

    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.selfName = new ClassName(name);
        this.classTransformer = new Lazy<>(() -> {
            try {
                return Class.forName(selfName.getFullyQualifiedName());
            } catch (ClassNotFoundException e) {
                return null;
            }
        });
        this.modifiers = access;
        if (superName != null) {
            extendsList.add(new ClassName(superName));
        }
        if (interfaces != null) {
            for (String i : interfaces) {
                extendsList.add(new ClassName(i));
            }
        }
        super.visit(version, access, name, signature, superName, interfaces);

        selfInfoAddress = RandomNameGenerator.generate(ClassInfo.class);
        addStaticField(ClassInfo.class, selfInfoAddress); //@see -> visitEnd

        cc.getClassInitializerCallbacks().forEach(cic -> {
            String address = addStaticField(ClassInitializerCallback.class, cic);
            classInitializerCallbacks.add(address);
        });
    }

    @Override
    public void visitSource(String source, String debug) {
        super.visitSource(source, debug);
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
    public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
        AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
        ClassName name = new ClassName(descriptor);
        return new AnnotationInfoVisitor(av, classTransformer, name, annotations);
    }

    @Override
    public AnnotationVisitor visitTypeAnnotation(int typeRef, TypePath typePath, String descriptor, boolean visible) {
        return super.visitTypeAnnotation(typeRef, typePath, descriptor, visible);
    }

    @Override
    public void visitAttribute(Attribute attribute) {
        super.visitAttribute(attribute);
    }

    @Override
    public void visitInnerClass(String name, String outerName, String innerName, int access) {
        super.visitInnerClass(name, outerName, innerName, access);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String descriptor, String signature, Object value) {
        FieldVisitor fv = super.visitField(access, name, descriptor, signature, value);
        FieldVisitor observer = new FieldVisitor(ASM6, fv) {
            final List<AnnotationInfo> annotations = new ArrayList<>();
            final Lazy<Field> transformer = new Lazy<>(() -> {
                Class clazz = classTransformer.get();
                if (clazz != null) {
                    try {
                        return clazz.getField(name);
                    } catch (NoSuchFieldException ignored) {}
                }
                return null;
            });

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
                return new AnnotationInfoVisitor(av, transformer, new ClassName(descriptor), annotations);
            }

            @Override
            public void visitEnd() {
                super.visitEnd();
                if ((ACC_SYNTHETIC & access) == 0 && !name.contains("$") && !name.equals("this")) {
                    FieldInfo fi = new FieldInfo(name,
                            access,
                            new ClassName(descriptor),
                            annotations.stream().map(Lazy::new).collect(Collectors.toList()),
                            new FieldAccessor() {
                                @Nullable
                                @Override
                                public Object get() {
                                    throw new RuntimeException("Cannot access field before it is defined!");
                                }
                            }, transformer);
                    fields.put(name, new Lazy<>(fi));
                    newFieldAccessorClass(fi);
                }
            }
        };
        return observer;
    }

    @Override //TODO: Delay <init> and <clint> until end of class declaration
    public MethodVisitor visitMethod(int access, String name, String descriptor, String signature, String[]
            exceptions) {
        MethodVisitor mv = super.visitMethod(access, name, descriptor, signature, exceptions);

        MethodVisitor observer = new MethodVisitor(ASM6, mv) {
            final Lazy<Method> transformer = new Lazy<>(() -> {
                Class clazz = classTransformer.get();
                if (clazz != null) {
                    for (Method m : clazz.getMethods()) {
                        if (m.getName().equals(name)) {
                            if (Type.getMethodDescriptor(m).equals(descriptor)) {
                                return m;
                            }
                        }
                    }
                }
                return null;
            });

            final List<AnnotationInfo> annotations = new ArrayList<>();
            final List<ParameterInfo> params = new ArrayList<>(); //TODO

            @Override
            public void visitParameter(String name, int access) {
                super.visitParameter(name, access);
            }

            @Override
            public AnnotationVisitor visitParameterAnnotation(int parameter, String descriptor, boolean visible) {
                return super.visitParameterAnnotation(parameter, descriptor, visible);
            }

            @Override
            public AnnotationVisitor visitAnnotation(String descriptor, boolean visible) {
                AnnotationVisitor av = super.visitAnnotation(descriptor, visible);
                return new AnnotationInfoVisitor(av, transformer, new ClassName(descriptor), annotations);
            }

            @Override
            public void visitEnd() {
                super.visitEnd();
                if ((ACC_SYNTHETIC & access) == 0 && !name.contains("$") && !name.contains("<")) {
                    MethodInfo mi = new MethodInfo(name,
                            modifiers,
                            new ClassName(descriptor.substring(descriptor.length() - 1)),
                            annotations.stream().map(Lazy::new).collect(Collectors.toList()),
                            params,
                            new MethodAccessor() {
                                @Nullable
                                @Override
                                public Object invoke(@Nullable Object... args) throws Throwable {
                                    throw new RuntimeException("Cannot access method before it is defined!");
                                }
                            });
                    methods.put(name, new Lazy<>(mi));
                    newMethodAccessorClass(mi);
                }
            }
        };

        if (name.equals("<clinit>")) {
            clinitCalled = true;
            return new MethodVisitor(ASM6, observer) {  //Wires non-primitive static fields created by #addStaticInstance
                @Override
                public void visitCode() {
                    super.visitCode();
                    queuedInitializers.forEach((k, v) -> {
                        visitLabel(new Label());
                        visitLdcInsn(k);
                        visitMethodInsn(INVOKESTATIC,
                                new ClassName(GlobalRegistry.class).getAsSlashNotation(),
                                "get",
                                "(Ljava/lang/Object;)Ljava/lang/Object;",
                                false);
                        visitTypeInsn(CHECKCAST, v.substring(1, v.length()-1)); //Strip L and ;
                        visitFieldInsn(PUTSTATIC, selfName.getAsSlashNotation(), k, v);
                    });
                }
            };
        }
        if (name.equals("<init>")) {
            return new MethodVisitor(ASM6, observer) {
                boolean calledThis = false; //Track if constructor called this()
                @Override
                public void visitMethodInsn(int opcode, String owner, String name, String descriptor, boolean
                        isInterface) {
                    if (opcode == INVOKESPECIAL && owner.equals(selfName.getAsSlashNotation()) && name.equals("<init>")) {
                        calledThis = true;
                    }
                    super.visitMethodInsn(opcode, owner, name, descriptor, isInterface);
                }

                @Override
                public void visitInsn(int opcode) {
                    if (opcode == RETURN) {
                        if (!calledThis) { //Only init if not already created
                            for (Consumer<MethodVisitor> callback : queuedInstanceInitializers) {
                                visitLabel(new Label());
                                callback.accept(this); //That callback better clean up
                            }
                            String cicSlash = new ClassName(ClassInitializerCallback.class).getAsSlashNotation();
                            String cicName = new ClassName(ClassInitializerCallback.class).getAsInternalTypeName();
                            String ciName = new ClassName(ClassInfo.class).getAsInternalTypeName();
                            String classInitDescriptor = "(Lsyringe/info/ClassInfo;ZLsyringe/access/LazyMap;Lsyringe/access/LazyMap;)V";
                            //                        String classInitSignature = "(Lsyringe/info/ClassInfo;ZLsyringe/access/LazyMap<Ljava/lang/String;Lsyringe/access/FieldAccessor;>;Lsyringe/access/LazyMap<Ljava/lang/String;Lsyringe/access/MethodAccessor;>;)V";
                            for (String cic : classInitializerCallbacks) {
                                visitLabel(new Label());
                                visitFieldInsn(GETSTATIC, selfName.getAsSlashNotation(), cic, cicName);
                                visitFieldInsn(GETSTATIC, selfName.getAsSlashNotation(), selfInfoAddress, ciName);
                                generateFieldAndMethodMaps(false, this, 0);
                                visitVarInsn(ALOAD, 1);
                                visitVarInsn(ALOAD, 2);
                                visitMethodInsn(INVOKEINTERFACE, cicSlash, "classInit", classInitDescriptor, true);
                            }
                        }
                    }

                    super.visitInsn(opcode);
                }
            };
        }
        return observer;
    }

    @Override
    public void visitEnd() {
        if (!clinitCalled && queuedInitializers.size() > 0) {
            MethodVisitor mv = visitMethod(0x8, "<clinit>", "()V", null, null);
            mv.visitCode();
            mv.visitEnd();
        } else if (clinitCalled && queuedInitializers.size() > 0) {
            throw new RuntimeException("Uh oh, required fields are not initialized!");
        }

        Supplier<ClassInfo> selfInfo = () -> {
            return new ClassInfo(selfName,
                    modifiers,
                    LazyMap.convert(fields),
                    LazyMap.convert(methods),
                    annotations.stream().map(Lazy::new).collect(Collectors.toList()),
                    extendsList,
                    classTransformer);
        };

        for (ClassAnnotationCallback cac : cc.getClassAnnotationCallbacks()) {
            cac.annotations(selfInfo.get(), annotations);
        }

        for (ClassDefinitionCallback cdc : cc.getClassDefinitionCallbacks()) {
            cdc.construction(selfInfo.get());
        }

        super.visitEnd();

        GlobalRegistry.register(selfInfoAddress, selfInfo.get());
    }

    private final class AnnotationInfoVisitor extends AnnotationVisitor {

        final Lazy<? extends AnnotatedElement> holder;
        final ClassName name;
        final List<AnnotationInfo> collector;
        final Map<String, Object> values = new HashMap<>();
        final List<AnnotationInfo> nested = new ArrayList<>();

        public AnnotationInfoVisitor(AnnotationVisitor annotationVisitor, Lazy<? extends AnnotatedElement> holder, ClassName name, List<AnnotationInfo> collector) {
            super(ASM6, annotationVisitor);
            this.holder = holder;
            this.name = name;
            this.collector = collector;
        }

        @Override
        public void visit(String name, Object value) {
            super.visit(name, value);
            values.put(name, value);
        }

        @Override
        public void visitEnum(String name, String descriptor, String value) {
            super.visitEnum(name, descriptor, value);
            values.put(name, value);
        }

        @Override
        public AnnotationVisitor visitArray(String name) {
            AnnotationVisitor av = super.visitArray(name);
            return new AnnotationVisitor(ASM6, av) {
                final List<Object> arrayCollection = new ArrayList<>();
                @Override
                public void visit(String name, Object value) {
                    super.visit(name, value);
                    arrayCollection.add(value);
                }

                @Override
                public void visitEnum(String name, String descriptor, String value) {
                    super.visitEnum(name, descriptor, value);
                    arrayCollection.add(value);
                }

                @Override
                public AnnotationVisitor visitArray(String name) {
                    throw new RuntimeException("Nested annotation array? Report this stacktrace!");
                }

                @Override
                public void visitEnd() {
                    super.visitEnd();
                    values.put(name, arrayCollection.toArray());
                }
            };
        }

        @Override
        public AnnotationVisitor visitAnnotation(String name, String descriptor) {
            AnnotationVisitor av = super.visitAnnotation(name, descriptor);
            return new AnnotationInfoVisitor(av, new Lazy<>(() -> {
                try {
                    return Class.forName(this.name.getFullyQualifiedName());
                } catch (ClassNotFoundException e) {
                    return null;
                }
            }), new ClassName(descriptor), nested);
        }

        @Override
        public void visitEnd() {
            super.visitEnd();
            LazyMap<String, FieldAccessor> accessors = new LazyMap<>(new HashMap<>(), values.keySet(), (k) -> {
                return () -> values.get(k);
            });
            collector.add(new AnnotationInfo(name, accessors, nested, new Lazy<>((Supplier<Annotation>) () -> {
                AnnotatedElement ae = holder.get();
                if (ae == null)
                    return null;
                for (Annotation a : ae.getAnnotations()) {
                    if (new ClassName(a.getClass()).equals(name)) {
                        return a;
                    }
                }
                return null;
            })));
        }
    }
}
