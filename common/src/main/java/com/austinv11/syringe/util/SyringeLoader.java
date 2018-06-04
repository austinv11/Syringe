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
package com.austinv11.syringe.util;

import com.austinv11.syringe.SyringeService;
import com.austinv11.syringe.visitor.InjectionVisitor;

import java.util.Iterator;
import java.util.ServiceLoader;

public final class SyringeLoader {

    private static final Lazy<ServiceLoader<SyringeService>> syringeLoader =
            new Lazy<>(() -> ServiceLoader.load(SyringeService.class));
    private static final Lazy<ServiceLoader<InjectionVisitor>> visitorLoader =
            new Lazy<>(() -> ServiceLoader.load(InjectionVisitor.class));

    private static <T> Iterator<T> loadServices(ServiceLoader<T> loader) {
        loader.reload();
        return loader.iterator();
    }

    public static Iterator<SyringeService> getServices() {
        return loadServices(syringeLoader.get());
    }

    public static Iterator<InjectionVisitor> getVisitors() {
        return loadServices(visitorLoader.get());
    }
}
