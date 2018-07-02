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
package com.austinv11.syringe.direct;

public class MethodIdentifier {

    private final TypeSignature owner;
    private final boolean isStatic;
    private final TypeSignature returnType;
    private final String name;
    private final TypeSignature[] params;

    public MethodIdentifier(TypeSignature owner, boolean isStatic, TypeSignature returnType, String name, TypeSignature[] params) {
        this.owner = owner;
        this.isStatic = isStatic;
        this.returnType = returnType;
        this.name = name;
        this.params = params;
    }

    public TypeSignature getOwner() {
        return owner;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public TypeSignature getReturnType() {
        return returnType;
    }

    public String getName() {
        return name;
    }

    public TypeSignature[] getParams() {
        return params;
    }
}
