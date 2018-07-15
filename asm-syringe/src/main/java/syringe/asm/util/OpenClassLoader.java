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

package syringe.asm.util;

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public final class OpenClassLoader extends ClassLoader {

    private final List<BiFunction<String, byte[], byte[]>> callbacks = new ArrayList<>();

    public OpenClassLoader(ClassLoader parent) {
        super(parent);
    }

    public OpenClassLoader() {
        super();
    }

    @Nullable
    public Class<?> define(@Nullable String name, byte[] bytecode, boolean skipInstrumentation) {
        if (name != null && name.startsWith("java."))
            return null;
        byte[] curr = bytecode;
        if (!skipInstrumentation) {
            for (BiFunction<String, byte[], byte[]> callback : callbacks) {
                curr = callback.apply(name, curr);
            }
        }
        return super.defineClass(name, curr, 0, bytecode.length);
    }

    public void registerCallback(BiFunction<String, byte[], byte[]> callback) {
        callbacks.add(callback);
    }
}
