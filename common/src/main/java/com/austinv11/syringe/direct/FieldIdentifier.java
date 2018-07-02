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

public class FieldIdentifier {

    private final TypeSignature owner;
    private final boolean isStatic;
    private final TypeSignature type;
    private final String name;

    public FieldIdentifier(TypeSignature owner, boolean isStatic, TypeSignature type, String name) {
        this.owner = owner;
        this.isStatic = isStatic;
        this.type = type;
        this.name = name;
    }

    public TypeSignature getOwner() {
        return owner;
    }

    public boolean isStatic() {
        return isStatic;
    }

    public TypeSignature getType() {
        return type;
    }

    public String getName() {
        return name;
    }
}
