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

package com.austinv11.syringe.callbacks.clazz;

import com.austinv11.syringe.access.FieldAccessor;
import com.austinv11.syringe.access.LazyMap;
import com.austinv11.syringe.access.MethodAccessor;
import com.austinv11.syringe.info.ClassInfo;

@FunctionalInterface
public interface ClassInitializerCallback {

    void classInit(ClassInfo clazz, LazyMap<String, FieldAccessor> staticFields, LazyMap<String, MethodAccessor> staticMethods);
}