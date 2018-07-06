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

package syringe.callbacks.method;

import syringe.info.ClassInfo;
import syringe.info.MethodInfo;

import javax.annotation.Nullable;
import java.util.List;

@FunctionalInterface
public interface MethodInvocationCallback { //TODO: Info about caller maybe?

    @Nullable
    Object call(ClassInfo clazz, MethodInfo method, @Nullable Object instance, List<Object> params) throws Throwable;
}
