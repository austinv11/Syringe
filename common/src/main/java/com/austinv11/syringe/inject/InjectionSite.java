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
package com.austinv11.syringe.inject;

import javax.annotation.Nullable;
import java.util.Optional;

/**
 * A simple POJO containing some information about the injection site.
 *
 * @see com.austinv11.syringe.inject.InjectionSiteFilter
 */
public class InjectionSite {

    private final InjectionTarget target;

    private final String packageName;
    private final String className;

    private final @Nullable String fieldName; //TODO signatures

    private final @Nullable String methodName; //TODO signatures

    public InjectionSite(InjectionTarget target, String packageName, String className, @Nullable String fieldName,
                         @Nullable String methodName) {
        this.target = target;
        this.packageName = packageName;
        this.className = className;
        this.fieldName = fieldName;
        this.methodName = methodName;
    }

    public InjectionTarget getTarget() {
        return target;
    }

    public String getPackageName() {
        return packageName;
    }

    public String getClassName() {
        return className;
    }

    public Optional<String> getFieldName() {
        return Optional.ofNullable(fieldName);
    }

    public Optional<String> getMethodName() {
        return Optional.ofNullable(methodName);
    }
}
