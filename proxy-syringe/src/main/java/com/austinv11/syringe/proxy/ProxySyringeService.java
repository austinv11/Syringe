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
package com.austinv11.syringe.proxy;

import com.austinv11.syringe.Syringe;
import com.austinv11.syringe.SyringeService;
import com.austinv11.syringe.util.IncompatibleConfigurationException;
import com.austinv11.syringe.visitor.InjectionVisitor;
import com.google.auto.service.AutoService;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@AutoService(SyringeService.class)
public class ProxySyringeService implements SyringeService {

    private static final Syringe syringe = new Syringe("Proxy-Syringe", "1.0", "JDK Proxies");

    private final List<InjectionVisitor> visitors = new CopyOnWriteArrayList<>();

    @Override
    public Syringe getSyringe() {
        return syringe;
    }

    @Override
    public void addVisitor(InjectionVisitor visitor) {
        visitors.add(visitor);
    }

    @Override
    public <T> Class<T> visit(Class<T> clazz) throws IncompatibleConfigurationException {

    }

    @Override
    public <T> T visit(T obj) throws IncompatibleConfigurationException {
        return null;
    }
}
