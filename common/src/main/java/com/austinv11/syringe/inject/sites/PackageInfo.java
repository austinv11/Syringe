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
import com.austinv11.syringe.util.Lazy;

public class PackageInfo {

    private final Lazy<AnnotationInfo>[] annotations;
    private final String name;

    public static Lazy<PackageInfo> fromPackage(Package p) {
        return new Lazy<>(() -> {
            Lazy<AnnotationInfo>[] annotations = AnnotationInfo.fromAnnotatedElement(p);
            return new PackageInfo(annotations, p.getName());
        });
    }

    public PackageInfo(Lazy<AnnotationInfo>[] annotations, String name) {
        this.annotations = annotations;
        this.name = name;
    }

    public Lazy<AnnotationInfo>[] getAnnotations() {
        return annotations;
    }

    public String getName() {
        return name;
    }
}
