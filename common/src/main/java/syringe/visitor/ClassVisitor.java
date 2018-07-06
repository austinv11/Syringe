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

package syringe.visitor;

import syringe.callbacks.clazz.*;

import java.util.Optional;

public interface ClassVisitor {

    default Optional<ClassInitializerCallback> classInit() {
        return Optional.empty();
    }

    default Optional<ClassConstructionCallback> classConstruct() {
        return Optional.empty();
    }

    default Optional<MethodDefinitionCallback> defineMethods() { //Handles addition + removal + signature
        return Optional.empty();
    }

    default Optional<FieldDefinitionCallback> defineFields() { //Handles addition + removal + signature
        return Optional.empty();
    }

    default Optional<ClassInheritanceCallback> defineInheritence() {
        return Optional.empty();
    }

    default Optional<ClassAnnotationCallback> defineAnnotations() {
        return Optional.empty();
    }
}