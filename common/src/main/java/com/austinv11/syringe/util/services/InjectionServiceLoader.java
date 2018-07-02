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

package com.austinv11.syringe.util.services;

import com.austinv11.syringe.visitor.InjectionVisitor;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public final class InjectionServiceLoader {

    public static Collection<InjectionService> services() {
        ServiceLoader<InjectionService> loader = ServiceLoader.load(InjectionService.class);
        List<InjectionService> services = new LinkedList<>();
        loader.iterator().forEachRemaining(services::add);
        return services;
    }

    public static Collection<InjectionVisitor> visitors() {
        return services().stream().map(InjectionService::visitors).flatMap(Collection::stream).collect(Collectors.toList());
    }
}
