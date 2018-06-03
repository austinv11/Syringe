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
package com.austinv11.syringe.inject.sites;

import com.austinv11.syringe.inject.AnnotationInfo;
import com.austinv11.syringe.inject.InjectionSite;
import com.austinv11.syringe.inject.InjectionTarget;

public class ClassSite extends InjectionSite {

    private final String packageName;

    private final FieldSite[] fields;

    private final MethodSite[] methods;

    public ClassSite(InjectionTarget target, AnnotationInfo[] annotationInfo, String name, int modifiers, String
            packageName, FieldSite[] fields, MethodSite[] methods) {
        super(target, annotationInfo, name, modifiers);
        this.packageName = packageName;
        this.fields = fields;
        this.methods = methods;
    }

    public String getPackageName() {
        return packageName;
    }

    public FieldSite[] getFields() {
        return fields;
    }

    public MethodSite[] getMethods() {
        return methods;
    }
}
