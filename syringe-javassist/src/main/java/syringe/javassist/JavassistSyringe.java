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
        try { //TODO
            return Collections.singleton(cp.get("test"));
        } catch (NotFoundException e) {
            return null;
        }
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
                        Map.of("method_name", mi.getName(),
                                "invocation_statement", invocation
                                        + mi.getName()
                                        + "("
                                        + String.join(",", argList)
                                        + ");"))).append("\n");
            } else {
                invocation += "this.";
                instanceMethodInit.append(TemplatingEngine.template(methodWrapper,
                        Map.of("method_name", mi.getName(),
                                "invocation_statement", invocation
                                        + mi.getName()
                                        + "("
                                        + String.join(",", argList)
                                        + ");"))).append("\n");
            }
        }

        String selfInfoHolder = RandomNameGenerator.generate() + "SelfInfo";
        InitializerRepository.put(selfInfoHolder, updatingClassInfoGenerator.apply(c));
        try {
            c.addField(new CtField(ctFromName(cp, new ClassName(ClassInfo.class)), selfInfoHolder, c),
                    CtField.Initializer.byCall(
                            cp.getCtClass(new ClassName(InitializerRepository.class).getAsInternalTypeName()),
                            "getAndRemove",
                            new String[]{selfInfoHolder}));
        } catch (CannotCompileException | NotFoundException e) {
            throw new RuntimeException(e);
        }

        String callbackHolder = RandomNameGenerator.generate() + "Holder";

        String stmt = TemplatingEngine.template(stackBuilder, Map.of("populate_static_field_map", staticFieldInit.toString(),
                "populate_instance_field_map", instanceFieldInit.toString(),
                "populate_static_method_map", staticMethodInit.toString(),
                "populate_instance_method_map", instanceMethodInit.toString(),
                "is_static_context", "true",
                "callbacks_field", callbackHolder,
                "self_info_field", selfInfoHolder));
        try {
            clinit.insertAfter(stmt, true);
        } catch (CannotCompileException e) {
            throw new RuntimeException(e);
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
    public void addVisitor(ClassVisitor cv) {
        classVisitors.add(cv);
    }

    @Override
    public void addVisitor(MethodVisitor mv) {
        methodVisitors.add(mv);
    }
}
