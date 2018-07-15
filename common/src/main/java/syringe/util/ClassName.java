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

import javax.annotation.Nullable;
import java.util.*;

/**
 * This parses a variety of different possible string representations of classes in order to break it down into its
 * components to allow for a clean abstraction which provides conversions to various other representations.
 * <br>
 * Examples of possible inputs:
 * <ul>
 *     <li>Ljava/lang/Object;</li>
 *     <li>java.lang.Object[]</li>
 *     <li>boolean</li>
 *     <li>Z</li>
 * </ul>
 */
public final class ClassName {

    private final static Map<String, String> primitives;
    private final static Map<String, String> primitivesReversed;

    static {
        Map<String, String> p = new HashMap<>();
        Map<String, String> pr = new HashMap<>();
        p.put("void", "V");
        p.put("boolean", "Z");
        p.put("char", "C");
        p.put("byte", "B");
        p.put("short", "S");
        p.put("int", "I");
        p.put("float", "F");
        p.put("long", "J");
        p.put("double", "D");
        primitives = Collections.unmodifiableMap(p);
        p.forEach((key, value) -> pr.put(value, key));
        primitivesReversed = Collections.unmodifiableMap(pr);
    }

    @Nullable
    private final String[] packageComponents;
    private final String className;
    private final int nestedArrayCount;
    private final boolean isPrimitive;

    public ClassName(Class<?> clazz) {
        this(clazz.getName());
    }

    public ClassName(String name) {
        if (name.startsWith("L") && name.endsWith(";"))
            name = name.substring(1);
        if (name.endsWith(";"))
            name = name.substring(0, name.length() - 1);
        int arrayCount = 0;
        while (name.startsWith("[")) {
            arrayCount++;
            name = name.substring(1);
        }
        while (name.endsWith("[]")) {
            arrayCount++;
            name = name.substring(0, name.length() - 2);
        }
        this.nestedArrayCount = arrayCount;
        if (primitives.containsKey(name)) {
            this.className = name;
            this.packageComponents = null;
            this.isPrimitive = true;
        } else if (primitivesReversed.containsKey(name)) {
            this.className = primitivesReversed.get(name);
            this.packageComponents = null;
            this.isPrimitive = true;
        } else {
            name = name.replace('/', '.');
            String[] split = name.split("\\.");
            this.packageComponents = new String[split.length - 1];
            System.arraycopy(split, 0, this.packageComponents, 0, split.length - 1);
            this.className = split[split.length - 1];
            this.isPrimitive = false;
        }
    }

    public String getRawName() {
        return className;
    }

    public String getTypeName() {
        return isPrimitive ? primitives.get(className) : className;
    }

    public String getPackageName() {
        if (packageComponents == null)
            return "";

        return String.join(".", packageComponents);
    }

    public String getFullyQualifiedName() {
        StringBuilder base;
        if (isPrimitive)
            base = new StringBuilder(getTypeName());
        else
            base = new StringBuilder(getPackageName() + "." + getTypeName());
        for (int i = 0; i < nestedArrayCount; i++) {
            base.append("[]");
        }
        return base.toString();
    }

    public String getAsSlashNotation() {
        String base;
        if (packageComponents != null) {
            base = String.join("/", packageComponents) + "/";
        } else {
            base = "";
        }
        return base + getTypeName();
    }

    public String getAsInternalTypeName() {
        StringBuilder str = new StringBuilder(isPrimitive ? getTypeName() : "L" + getAsSlashNotation() + ";");
        for (int i = 0; i < nestedArrayCount; i++) {
            str.insert(0, "[");
        }
        return str.toString();
    }

    public boolean isPrimitive() {
        return isPrimitive;
    }

    public boolean isArray() {
        return nestedArrayCount > 0;
    }

    public int getArrayDimension() {
        return nestedArrayCount;
    }

    @Override
    public String toString() {
        return getFullyQualifiedName();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof ClassName) {
            return getAsInternalTypeName().equals(((ClassName) obj).getAsInternalTypeName());
        }
        return false;
    }

    @Override
    public int hashCode() {
        int result = Objects.hash(className, nestedArrayCount, isPrimitive());
        result = 31 * result + Arrays.hashCode(packageComponents);
        return result;
    }
}
