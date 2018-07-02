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
package com.austinv11.syringe.cglib;

import com.austinv11.syringe.Syringe;
import com.austinv11.syringe.SyringeService;
import com.austinv11.syringe.util.IncompatibleConfigurationException;
import com.austinv11.syringe.util.services.InjectionServiceLoader;
import com.austinv11.syringe.visitor.InjectionVisitor;
import com.austinv11.syringe.visitor.MethodInjectionVisitor;
import com.google.auto.service.AutoService;
import net.sf.cglib.proxy.Enhancer;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * This is a {@link com.austinv11.syringe.SyringeService} implementation backed by
 * <a href="https://github.com/cglib/cglib">cglib</a>. Due to the nature of the library, it only supports modifying
 * instance methods and does not support in-place modifications (it creates a new proxy class, it does not modify
 * the original class) since this simply creates proxies like the jdk {@link java.lang.reflect.Proxy}.
 *
 * Recommended use-cases for this: cglib is in your classpath already and/or you wish to retain the original classes
 * in your classpath.
 */
@SuppressWarnings("unchecked")
@AutoService(SyringeService.class)
public class CglibSyringe implements SyringeService {

    public static final Syringe SYRINGE = new Syringe("CGLib-Syringe", "1.0", "CGLib proxies");

    protected final Set<MethodInjectionVisitor> visitors = new LinkedHashSet<>();

    public CglibSyringe() {
        InjectionServiceLoader.visitors().stream().filter(v -> v instanceof MethodInjectionVisitor).forEach(this::addVisitor);
    }

    @Override
    public Syringe getSyringe() {
        return SYRINGE;
    }

    @Override
    public void addVisitor(InjectionVisitor visitor) {
        if (!(visitor instanceof MethodInjectionVisitor))
            throw new IllegalArgumentException("This syringe only supports method injections");

        visitors.add((MethodInjectionVisitor) visitor);
    }

    @Override
    public <T> Class<T> visit(Class<T> clazz) throws IncompatibleConfigurationException {
        Enhancer enhancer = new Enhancer();
        if (clazz.isInterface()) {
            enhancer.setInterfaces(new Class[]{clazz, Syringe.SyringeVisited.class});
        } else {
            enhancer.setSuperclass(clazz);
            enhancer.setInterfaces(new Class[]{Syringe.SyringeVisited.class});
        }
        enhancer.setCallback(new SyringeCallback(this));
        return enhancer.createClass();
    }

    @Override
    public <T> T visit(T obj) throws IncompatibleConfigurationException {
        Enhancer enhancer = new Enhancer();
        if (obj.getClass().isInterface()) {
            enhancer.setInterfaces(new Class[]{obj.getClass(), Syringe.SyringeVisited.class});
        } else {
            enhancer.setSuperclass(obj.getClass());
            enhancer.setInterfaces(new Class[]{Syringe.SyringeVisited.class});
        }
        enhancer.setCallback(new SyringeCallback(this));
        return (T) enhancer.create();
    }
}
