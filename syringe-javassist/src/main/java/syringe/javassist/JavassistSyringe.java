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

package syringe.javassist;

import javassist.*;
import javassist.bytecode.AccessFlag;
import javassist.bytecode.Descriptor;
import syringe.Syringe;
import syringe.callbacks.clazz.ClassInitializerCallback;
import syringe.callbacks.clazz.FieldDefinitionCallback;
import syringe.callbacks.clazz.MethodDefinitionCallback;
import syringe.callbacks.method.ExceptionThrownCallback;
import syringe.callbacks.method.MethodInvocationCallback;
import syringe.callbacks.method.MethodReturnCallback;
import syringe.info.*;
import syringe.javassist.util.InitializerRepository;
import syringe.javassist.util.SyringeHelper;
import syringe.javassist.util.TemplatingEngine;
import syringe.javassist.util.ThrowingFunction;
import syringe.util.CallbackCollector;
import syringe.util.ClassName;
import syringe.util.Lazy;
import syringe.util.RandomNameGenerator;
import syringe.visitor.ClassVisitor;
import syringe.visitor.MethodVisitor;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

public class JavassistSyringe implements Syringe {

    private static final String stackBuilder = readResource("StackBuilder.java");
    private static final String fieldWrapper = readResource("FieldWrapper.java");
    private static final String methodWrapper = readResource("MethodWrapper.java");
    private static final String methodCallAndReturn = readResource("MethodCallAndReturn.java");
    private static final String methodCall = readResource("MethodCall.java");
    private static final String methodBeforeInsertion = readResource("MethodBeforeInsertion.java");
    private static final String methodAfterInsertion = readResource("MethodAfterInsertion.java");
    private static final String catchHandler = readResource("CatchHandler.java");

    private final Set<ClassVisitor> classVisitors = new HashSet<>();
    private final Set<MethodVisitor> methodVisitors = new HashSet<>();

    private static final String readResource(String address) {
        InputStream stream = JavassistSyringe.class.getResourceAsStream("templates/" + address);
        Scanner s = new Scanner(stream).useDelimiter("\\A");
        return s.hasNext() ? s.next() : "";
    }

    private static final CtClass ctFromName(ClassPool cp, ClassName name) {
        if (name.isPrimitive() && !name.isArray()) {
            switch (name.getRawName()) {
                case "boolean":
                    return CtClass.booleanType;
                case "char":
                    return CtClass.charType;
                case "byte":
                    return CtClass.byteType;
                case "short":
                    return CtClass.shortType;
                case "long":
                    return CtClass.longType;
                case "float":
                    return CtClass.floatType;
                case "double":
                    return CtClass.doubleType;
                case "void":
                    return CtClass.voidType;
            }
        }
        try {
            return Descriptor.toCtClass(name.getAsInternalTypeName(), cp);
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    private Iterable<CtClass> scanClasses(ClassPool cp) {
        throw new RuntimeException("Runtime scanning not currently implemented!"); //TODO
    }

    private static String uniqueSignature(MethodInfo mi) {
        StringBuilder sb = new StringBuilder(mi.getName()).append('(');
        for (ParameterInfo pi : mi.getParams()) {
            sb.append(pi.getType().getAsInternalTypeName());
        }
        sb.append(')');
        return sb.toString();
    }

    private static String uniqueSignature(CtMethod cm) {
        StringBuilder sb = new StringBuilder(cm.getName()).append('(');
        try {
            for (CtClass pt : cm.getParameterTypes()) {
                sb.append(new ClassName(pt.getName()).getAsInternalTypeName());
            }
        } catch (NotFoundException e) {
            throw new RuntimeException(e);
        }
        sb.append(')');
        return sb.toString();
    }

    private void hook(CallbackCollector cc, ClassPool cp, CtClass c) {
        Function<CtClass, ClassInfo> updatingClassInfoGenerator = (ctClass) -> {
            return SyringeHelper.buildClass(ctClass, (cm) -> SyringeHelper.buildMethod(cm, (args) -> {
                throw new RuntimeException("Method is not accessible form this context!");
            }).get(), (cf) -> SyringeHelper.buildField(cf, () -> {
                throw new RuntimeException("Field is not accessible from this context!");
            }).get()).get();
        };

        //FieldDefinitionCallback
        List<FieldInfo> fields = new ArrayList<>();
        for (CtField f : c.getFields()) {
            if ((f.getModifiers() & AccessFlag.SYNTHETIC) != 0 && !f.getName().contains("$")) {
                continue;
            }
            fields.add(SyringeHelper.buildField(f, () -> {
                throw new RuntimeException("Field is not accessible from this context!");
            }).get());
        }
        for (FieldDefinitionCallback fdc : cc.getFieldDefinitionCallbacks()) {
            List<FieldInfo> newFields = fdc.fields(updatingClassInfoGenerator.apply(c), fields);
            for (FieldInfo f : newFields) {
                try {
                    CtField field = new CtField(ctFromName(cp, f.getType()), f.getName(), c);
                    field.setModifiers(f.getModifiers());
                    //TODO: annotations?
                    String loc = RandomNameGenerator.generate() + field.getName();
                    c.addField(field, CtField.Initializer.byCall(
                            cp.getCtClass(new ClassName(InitializerRepository.class).getAsInternalTypeName()),
                            "getAndRemove",
                            new String[]{loc}));
                    try {
                        InitializerRepository.put(loc, f.getAccessor().get());
                    } catch (Throwable t) {
                        InitializerRepository.put(loc, null); //TODO: handle primitive defaults
                    }
                } catch (CannotCompileException | NotFoundException e) {
                    throw new RuntimeException(e);
                }
                fields.add(f);
            }
        }

        //MethodDefinitionCallback
        List<MethodInfo> methods = new ArrayList<>();
        for (CtMethod m : c.getMethods()) {
            if ((m.getModifiers() & AccessFlag.SYNTHETIC) != 0
                    && !m.getName().contains("<")
                    && !m.getName().contains("$")) {
                continue;
            }
            methods.add(SyringeHelper.buildMethod(m, (args) -> {
                throw new RuntimeException("Method is not accessible from this context!");
            }).get());
        }
        for (MethodDefinitionCallback mdc : cc.getMethodDefinitionCallbacks()) {
            List<MethodInfo> newMethods = mdc.methods(updatingClassInfoGenerator.apply(c), methods);
            for (MethodInfo mi : newMethods) {
                String name = RandomNameGenerator.generate(Function.class) + "bridge";
                CtField bridge;
                try {
                    bridge = new CtField(cp.getCtClass(new ClassName(ThrowingFunction.class).getAsInternalTypeName()), name, c);
                    bridge.setModifiers(AccessFlag.SYNTHETIC | AccessFlag.PROTECTED | AccessFlag.STATIC);
                    c.addField(bridge, CtField.Initializer.byCall(cp.getCtClass(new ClassName(InitializerRepository.class).getAsInternalTypeName()), "getAndRemove", new String[]{name}));
                    InitializerRepository.put(name, (ThrowingFunction<Object[], Object>) objects -> mi.getAccessor().invoke(objects));
                } catch (CannotCompileException | NotFoundException e) {
                    throw new RuntimeException(e);
                } //TODO: more extensive new method support?
                CtClass[] params = new CtClass[mi.getParams().size()];
                for (int i = 0; i < params.length; i++) {
                    params[i] = ctFromName(cp, mi.getParams().get(i).getType());
                }
                CtClass[] throwTypes = new CtClass[] {ctFromName(cp, new ClassName(Throwable.class))};
                try {
                    CtMethod ctMethod = CtNewMethod.make(mi.getModifiers(),
                            ctFromName(cp, mi.getReturnType()),
                            mi.getName(),
                            params,
                            throwTypes,
                            mi.getReturnType().equals(new ClassName("void")) ? "{return;}" : "return null;", //FIXME: better default value support
                            c);
                    c.addMethod(ctMethod);
                } catch (CannotCompileException e) {
                    throw new RuntimeException(e);
                }
                methods.add(mi);
            }
        }

        //ClassDefinitionCallback
        cc.getClassDefinitionCallbacks().forEach(cdc -> cdc.construction(updatingClassInfoGenerator.apply(c)));

        //ClassAnnotationCallback
        cc.getClassAnnotationCallbacks().forEach(cac -> {
            ClassInfo clazz = updatingClassInfoGenerator.apply(c);
            cac.annotations(clazz, clazz.getAnnotations().stream().map(Lazy::get).collect(Collectors.toList()));
        });

        //TODO: Add constructor/initializer if it doesn't exist
        //ClassInitializerCallback
        CtConstructor clinit = c.getClassInitializer();
        if (clinit == null) {
            try {
                clinit = c.makeClassInitializer();
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        }
        StringBuilder staticFieldInit = new StringBuilder();
        StringBuilder instanceFieldInit = new StringBuilder();
        for (FieldInfo fi : fields) {
            if (Modifier.isStatic(fi.getModifiers())) {
                staticFieldInit.append(TemplatingEngine.template(fieldWrapper, Map.of("field_name", fi.getName()))).append("\n");
            } else {
                instanceFieldInit.append(TemplatingEngine.template(fieldWrapper, Map.of("field_name", fi.getName()))).append("\n");
            }
        }
        StringBuilder staticMethodInit = new StringBuilder();
        StringBuilder instanceMethodInit = new StringBuilder();
        for (MethodInfo mi : methods) {
            List<String> argList = new ArrayList<>();
            List<ParameterInfo> params = mi.getParams();
            for (int i = 0; i < params.size(); i++) {
                argList.add("args[" + i + "]");
            }
            String invocation = c.getSimpleName() + ".";
            if (Modifier.isStatic(mi.getModifiers())) {
                staticMethodInit.append(TemplatingEngine.template(methodWrapper,
                        Map.of("method_name", uniqueSignature(mi),
                                "invocation_statement", invocation
                                        + mi.getName()
                                        + "("
                                        + String.join(",", argList)
                                        + ");"))).append("\n");
            } else {
                invocation += "this.";
                instanceMethodInit.append(TemplatingEngine.template(methodWrapper,
                        Map.of("method_name", uniqueSignature(mi),
                                "invocation_statement", invocation
                                        + mi.getName()
                                        + "("
                                        + String.join(",", argList)
                                        + ");"))).append("\n");
            }
        }

        String selfInfoHolder = RandomNameGenerator.generate() + "SelfInfo";
        try {
            CtField f = new CtField(ctFromName(cp, new ClassName(ClassInfo.class)), selfInfoHolder, c);
            f.setModifiers(Modifier.STATIC);
            c.addField(f);
        } catch (CannotCompileException e) {
            throw new RuntimeException(e);
        }

        String callbackHolder = RandomNameGenerator.generate() + "Holder";
        InitializerRepository.put(callbackHolder, cc.getClassInitializerCallbacks().toArray(new ClassInitializerCallback[0]));
        try {
            CtField f = new CtField(ctFromName(cp, new ClassName(ClassInitializerCallback[].class)), callbackHolder, c);
            f.setModifiers(Modifier.STATIC);
            c.addField(f, CtField.Initializer.byCall(cp.getCtClass(new ClassName(InitializerRepository.class).getAsInternalTypeName()), "getAndRemove", new String[]{callbackHolder}));
        } catch (CannotCompileException | NotFoundException e) {
            throw new RuntimeException(e);
        }

        String stmt = TemplatingEngine.template(stackBuilder, Map.of("populate_static_field_map", staticFieldInit.toString(),
                "populate_instance_field_map", instanceFieldInit.toString(),
                "populate_static_method_map", staticMethodInit.toString(),
                "populate_instance_method_map", instanceMethodInit.toString(),
                "is_static_context", "%s",
                "callbacks_field", callbackHolder,
                "self_info_field", selfInfoHolder + "_%s",
                "class_name", c.getSimpleName(),
                "class_modifiers", Integer.toString(c.getModifiers())));
        try {
            clinit.insertAfter(String.format(stmt, "true", "static"), true);
        } catch (CannotCompileException e) {
            throw new RuntimeException(e);
        }
        for (CtConstructor init : c.getDeclaredConstructors()) {
            try {
                if (init.callsSuper()) {
                    init.insertAfter(String.format(stmt, "false", "instance"), true);
                }
            } catch (CannotCompileException e) {
                throw new RuntimeException(e);
            }
        }

        //Method callbacks
        String exceptionThrownHolder = RandomNameGenerator.generate() + "ExceptionThrownHolder";
        InitializerRepository.put(exceptionThrownHolder, cc.getExceptionThrownCallbacks().toArray(new ExceptionThrownCallback[0]));
        try {
            CtField f = new CtField(ctFromName(cp, new ClassName(ExceptionThrownCallback[].class)), exceptionThrownHolder, c);
            f.setModifiers(Modifier.STATIC);
            c.addField(f, CtField.Initializer.byCall(cp.getCtClass(new ClassName(InitializerRepository.class).getAsInternalTypeName()), "getAndRemove", new String[]{exceptionThrownHolder}));
        } catch (CannotCompileException | NotFoundException e) {
            throw new RuntimeException(e);
        }

        String methodInvocationHolder = RandomNameGenerator.generate() + "MethodInvocationHolder";
        InitializerRepository.put(methodInvocationHolder, cc.getMethodInvocationCallbacks().toArray(new MethodInvocationCallback[0]));
        try {
            CtField f = new CtField(ctFromName(cp, new ClassName(MethodInvocationCallback[].class)), methodInvocationHolder, c);
            f.setModifiers(Modifier.STATIC);
            c.addField(f, CtField.Initializer.byCall(cp.getCtClass(new ClassName(InitializerRepository.class).getAsInternalTypeName()), "getAndRemove", new String[]{methodInvocationHolder}));
        } catch (CannotCompileException | NotFoundException e) {
            throw new RuntimeException(e);
        }

        String methodReturnHolder = RandomNameGenerator.generate() + "MethodReturnHolder";
        InitializerRepository.put(methodReturnHolder, cc.getMethodReturnCallbacks().toArray(new MethodReturnCallback[0]));
        try {
            CtField f = new CtField(ctFromName(cp, new ClassName(MethodReturnCallback[].class)), methodReturnHolder, c);
            f.setModifiers(Modifier.STATIC);
            c.addField(f, CtField.Initializer.byCall(cp.getCtClass(new ClassName(InitializerRepository.class).getAsInternalTypeName()), "getAndRemove", new String[]{methodReturnHolder}));
        } catch (CannotCompileException | NotFoundException e) {
            throw new RuntimeException(e);
        }

        ClassInfo currSelf = updatingClassInfoGenerator.apply(c);
        currSelf.getMethods().keys().forEach(mk -> {
            MethodInfo mi = currSelf.getMethods().get(mk);
            List<AnnotationInfo> annotations = mi.getAnnotations().stream().map(Lazy::get).collect(Collectors.toList());
            cc.getMethodAnnotationCallbacks().forEach(mac -> {
                mac.annotations(currSelf, mi, annotations);
            });
        });

        for (CtMethod m : c.getDeclaredMethods()) {
            if ((((AccessFlag.SYNTHETIC | AccessFlag.BRIDGE) & m.getModifiers()) != 0) || m.getName().contains("<"))
                continue;

            StringBuilder before = new StringBuilder();
            for (int i = 0; i < cc.getMethodInvocationCallbacks().size(); i++) {
                before.append(TemplatingEngine.template(methodBeforeInsertion,
                        Map.of("self_info_field", selfInfoHolder + "_" + (Modifier.isStatic(m.getModifiers()) ?
                                        "static" : "instance"),
                                "callback_field", methodInvocationHolder + "[" + i + "]",
                                "static_context", Modifier.isStatic(m.getModifiers()) ? "true" : "false",
                                "method_key", uniqueSignature(m))));
            }
            if (before.length() > 0) {
                try {
                    m.insertBefore(before.toString());
                } catch (CannotCompileException e) {
                    throw new RuntimeException(e);
                }
            }

            StringBuilder after = new StringBuilder();
            for (int i = 0; i < cc.getMethodReturnCallbacks().size(); i++) {
                after.append(TemplatingEngine.template(methodAfterInsertion,
                        Map.of("self_info_field", selfInfoHolder + "_" + (Modifier.isStatic(m.getModifiers()) ?
                                        "static" : "instance"),
                                "callback_field", methodReturnHolder + "[" + i + "]",
                                "static_context", Modifier.isStatic(m.getModifiers()) ? "true" : "false",
                                "method_key", uniqueSignature(m))));
            }
            if (after.length() > 0) {
                after.append("\n{return ($r) $_;}");
                try {
                    m.insertAfter(after.toString());
                } catch (CannotCompileException e) {
                    throw new RuntimeException(e);
                }
            }

            StringBuilder exception = new StringBuilder();
            for (int i = 0; i < cc.getMethodInvocationCallbacks().size(); i++) {
                exception.append(TemplatingEngine.template(catchHandler,
                        Map.of("self_info_field", selfInfoHolder + "_" + (Modifier.isStatic(m.getModifiers()) ?
                                        "static" : "instance"),
                                "callback_field", methodReturnHolder + "[" + i + "]",
                                "method_key", uniqueSignature(m))));
            }
            if (exception.length() > 0) {
                exception.append("\n{return ($r) $_;}");
                try {
                    m.addCatch(exception.toString(), ctFromName(cp, new ClassName(Throwable.class)));
                } catch (CannotCompileException e) {
                    throw new RuntimeException(e);
                }
            }
        }
    }

    @Override
    public void inject() {
        CallbackCollector cc = new CallbackCollector(classVisitors, methodVisitors);
        ClassPool cp = ClassPool.getDefault();
        for (CtClass clazz : scanClasses(cp)) {
            try {
                hook(cc, cp, clazz);
                clazz.writeFile();
                clazz.detach();
            } catch (NotFoundException | IOException | CannotCompileException e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public <T> Class<? extends T> inject(Class<? extends T> clazz) {
        ClassPool cp = ClassPool.getDefault();
        try {
            CtClass cc = cp.get(clazz.getName());
            hook(new CallbackCollector(classVisitors, methodVisitors), cp, cc);
            cc.writeFile();
            cc.detach();
            return (Class<? extends T>) cc.toClass();
        } catch (NotFoundException | CannotCompileException | IOException e) {
            throw new RuntimeException(e);
        }
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
