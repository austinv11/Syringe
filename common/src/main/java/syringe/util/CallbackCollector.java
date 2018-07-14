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

package syringe.util;

import syringe.callbacks.clazz.*;
import syringe.callbacks.method.ExceptionThrownCallback;
import syringe.callbacks.method.MethodAnnotationCallback;
import syringe.callbacks.method.MethodInvocationCallback;
import syringe.callbacks.method.MethodReturnCallback;
import syringe.visitor.ClassVisitor;
import syringe.visitor.MethodVisitor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * Simple class which combines multiple {@link syringe.visitor.ClassVisitor}s and
 * {@link syringe.visitor.MethodVisitor}s. It is recommended to recreate this instance if there's been a chance of
 * visitor state change.
 */
public class CallbackCollector {

    //ClassVisitor
    private final Set<ClassAnnotationCallback> classAnnotationCallbacks = new HashSet<>();
    private final Set<ClassDefinitionCallback> classDefinitionCallbacks = new HashSet<>();
    private final Set<ClassInitializerCallback> classInitializerCallbacks = new HashSet<>();
    private final Set<FieldDefinitionCallback> fieldDefinitionCallbacks = new HashSet<>();
    private final Set<MethodDefinitionCallback> methodDefinitionCallbacks = new HashSet<>();

    //MethodVisitor
    private final Set<ExceptionThrownCallback> exceptionThrownCallbacks = new HashSet<>();
    private final Set<MethodAnnotationCallback> methodAnnotationCallbacks = new HashSet<>();
    private final Set<MethodInvocationCallback> methodInvocationCallbacks = new HashSet<>();
    private final Set<MethodReturnCallback> methodReturnCallbacks = new HashSet<>();

    public CallbackCollector(Collection<ClassVisitor> cvs, Collection<MethodVisitor> mvs) {
        for (ClassVisitor cv : cvs) {
            cv.defineAnnotations().ifPresent(classAnnotationCallbacks::add);
            cv.classConstruct().ifPresent(classDefinitionCallbacks::add);
            cv.classInit().ifPresent(classInitializerCallbacks::add);
            cv.defineFields().ifPresent(fieldDefinitionCallbacks::add);
            cv.defineMethods().ifPresent(methodDefinitionCallbacks::add);
        }

        for (MethodVisitor mv : mvs) {
            mv.throwException().ifPresent(exceptionThrownCallbacks::add);
            mv.defineAnnotations().ifPresent(methodAnnotationCallbacks::add);
            mv.invokeMethod().ifPresent(methodInvocationCallbacks::add);
            mv.methodReturn().ifPresent(methodReturnCallbacks::add);
        }
    }

    //ClassVisitor

    public Set<ClassAnnotationCallback> getClassAnnotationCallbacks() {
        return classAnnotationCallbacks;
    }

    public Set<ClassDefinitionCallback> getClassDefinitionCallbacks() {
        return classDefinitionCallbacks;
    }

    public Set<ClassInitializerCallback> getClassInitializerCallbacks() {
        return classInitializerCallbacks;
    }

    public Set<FieldDefinitionCallback> getFieldDefinitionCallbacks() {
        return fieldDefinitionCallbacks;
    }

    public Set<MethodDefinitionCallback> getMethodDefinitionCallbacks() {
        return methodDefinitionCallbacks;
    }


    //MethodVisitor

    public Set<ExceptionThrownCallback> getExceptionThrownCallbacks() {
        return exceptionThrownCallbacks;
    }

    public Set<MethodAnnotationCallback> getMethodAnnotationCallbacks() {
        return methodAnnotationCallbacks;
    }

    public Set<MethodInvocationCallback> getMethodInvocationCallbacks() {
        return methodInvocationCallbacks;
    }

    public Set<MethodReturnCallback> getMethodReturnCallbacks() {
        return methodReturnCallbacks;
    }
}
