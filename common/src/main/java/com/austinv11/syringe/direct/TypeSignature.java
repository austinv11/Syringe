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

import java.util.Objects;

public class TypeSignature {

    public static final TypeSignature VOID = new TypeSignature("V");
    public static final TypeSignature BOOLEAN = new TypeSignature("Z");
    public static final TypeSignature CHAR = new TypeSignature("C");
    public static final TypeSignature BYTE = new TypeSignature("B");
    public static final TypeSignature SHORT = new TypeSignature("S");
    public static final TypeSignature INT = new TypeSignature("I");
    public static final TypeSignature FLOAT = new TypeSignature("F");
    public static final TypeSignature LONG = new TypeSignature("J");
    public static final TypeSignature DOUBLE = new TypeSignature("D");

    private final String stringRep;

    private static String format(String packageName, String className, boolean isArray) {
        return String.format("%sL%s/%s;", isArray ? "[" : "",
                String.join("/", packageName.split(".")),
                className);
    }

    public TypeSignature(String packageName, String className, boolean isArray) {
        this(format(packageName, className, isArray));
    }

    public TypeSignature(String stringRep) {
        this.stringRep = stringRep;
    }

    public TypeSignature toArray() {
        return new TypeSignature("[" + stringRep);
    }

    @Override
    public String toString() {
        return stringRep;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof TypeSignature)) {
            return false;
        }
        TypeSignature that = (TypeSignature) o;
        return Objects.equals(stringRep, that.stringRep);
    }

    @Override
    public int hashCode() {

        return Objects.hash(stringRep);
    }
}
