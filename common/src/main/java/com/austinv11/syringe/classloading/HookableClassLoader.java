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
package com.austinv11.syringe.classloading;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLClassLoader;
import java.net.URLStreamHandlerFactory;
import java.security.CodeSource;
import java.security.PermissionCollection;
import java.util.Enumeration;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.jar.Manifest;

/**
 * This is an extension of {@link java.net.URLClassLoader} with some exposed hooks (callbacks and imperative).
 */
public class HookableClassLoader extends URLClassLoader {

    private final Set<Runnable> closeHooks = new CopyOnWriteArraySet<>();
    private volatile boolean isClosed = false;

    //TODO package hooks?

    private final Set<Predicate<String>> shouldLoadHooks = new CopyOnWriteArraySet<>();
    private final Set<Function<String, Optional<Class<?>>>> preLoadHooks = new CopyOnWriteArraySet<>();
    private final Set<Function<Class<?>, Optional<Class<?>>>> postLoadHooks = new CopyOnWriteArraySet<>();

    public HookableClassLoader(URL[] urls, ClassLoader parent) {
        super(urls, parent);
    }

    public HookableClassLoader(URL[] urls) {
        super(urls);
    }

    public HookableClassLoader(URL[] urls, ClassLoader parent, URLStreamHandlerFactory factory) {
        super(urls, parent, factory);
    }

    public HookableClassLoader(ClassLoader parent) {
        this(new URL[0], parent);
    }

    public HookableClassLoader() {
        this(new URL[0]);
    }

    @Override
    public InputStream getResourceAsStream(String name) {
        return super.getResourceAsStream(name);
    }

    /**
     * Adds a hook ran when {@link #close()} is called. This hook should run as quickly as possible!
     *
     * @param r The close hook to add.
     *
     * @see #removeCloseHook(Runnable)
     */
    public void addCloseHook(Runnable r) {
        closeHooks.add(r);
    }

    /**
     * Removes a close hook.
     *
     * @param r The close hook to remove.
     *
     * @see #addCloseHook(Runnable)
     */
    public void removeCloseHook(Runnable r) {
        closeHooks.remove(r);
    }

    @Override
    public void close() throws IOException {
        if (!isClosed) {
            closeHooks.forEach(Runnable::run);
            super.close();
            isClosed = true;
        }
    }

    /**
     * Checks if this class loader has been closed.
     *
     * @return True if closed, false if otherwise.
     */
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public void addURL(URL url) {
        super.addURL(url);
    }

    @Override
    public URL[] getURLs() {
        return super.getURLs();
    }

    @Override
    public Class<?> findClass(String name) throws ClassNotFoundException {
        return super.findClass(name);
    }

    @Override
    public Package definePackage(String name, Manifest man, URL url) throws IllegalArgumentException {
        return super.definePackage(name, man, url);
    }

    @Override
    public URL findResource(String name) {
        return super.findResource(name);
    }

    @Override
    public Enumeration<URL> findResources(String name) throws IOException {
        return super.findResources(name);
    }

    @Override
    public PermissionCollection getPermissions(CodeSource codesource) {
        return super.getPermissions(codesource);
    }

    /**
     * Adds a hook to determine whether a passed class definition should be allowed to load or not.
     *
     * @param hook The hook.
     *
     * @see #removeShouldLoadHook(java.util.function.Predicate)
     */
    public void addShouldLoadHook(Predicate<String> hook) {
        shouldLoadHooks.add(hook);
    }

    /**
     * Removes a "should load" hook.
     *
     * @param hook The hook.
     *
     * @see #addShouldLoadHook(java.util.function.Predicate)
     */
    public void removeShouldLoadHook(Predicate<String> hook) {
        shouldLoadHooks.remove(hook);
    }

    /**
     * Adds a hook to attempt to class load before the standard jdk mechanism.
     *
     * @param preHook The hook.
     *
     * @see #removePreLoadHook(java.util.function.Function)
     */
    public void addPreLoadHook(Function<String, Optional<Class<?>>> preHook) {
        preLoadHooks.add(preHook);
    }

    /**
     * Removes a pre-load hook.
     *
     * @param preHook The hook.
     *
     * @see #addPreLoadHook(java.util.function.Function)
     */
    public void removePreLoadHook(Function<String, Optional<Class<?>>> preHook) {
        preLoadHooks.remove(preHook);
    }

    /**
     * Adds a hook to attempt to class load after the standard jdk mechanism (useful for decorating classes).
     *
     * @param postHook The hook.
     *
     * @see #removePostLoadHook(java.util.function.Function)
     */
    public void addPostLoadHook(Function<Class<?>, Optional<Class<?>>> postHook) {
        postLoadHooks.add(postHook);
    }

    /**
     * Removes a post-load hook.
     *
     * @param postHook The hook.
     *
     * @see #addPostLoadHook(java.util.function.Function)
     */
    public void removePostLoadHook(Function<Class<?>, Optional<Class<?>>> postHook) {
        postLoadHooks.remove(postHook);
    }

    @Override
    public Class<?> loadClass(String name) throws ClassNotFoundException {
        return super.loadClass(name);
    }

    @Override
    public Class<?> loadClass(String name, /*Whether to load classes referenced in the given class*/ boolean resolve) throws ClassNotFoundException {
        if (!shouldLoadHooks.isEmpty()) {
            if (shouldLoadHooks.stream().anyMatch(p -> !p.test(name)))
                return null;
        }

        Class<?> clazz = null;

        if (!preLoadHooks.isEmpty()) {
            clazz = preLoadHooks
                    .stream()
                    .map(f -> f.apply(name))
                    .filter(Optional::isPresent)
                    .map(o -> o.get())
                    .findFirst()
                    .orElse(null);
        }

        if (clazz == null)
            clazz = super.loadClass(name, resolve);

        Class<?> finalClazz = clazz;

        Class<?> newClass = null;
        if (!postLoadHooks.isEmpty()) {
            newClass = postLoadHooks
                    .stream()
                    .map(f -> f.apply(finalClazz))
                    .filter(Optional::isPresent)
                    .map(o -> o.get())
                    .findFirst()
                    .orElse(null);
        }

        return newClass == null ? finalClazz : newClass;
    }

    @Override
    protected Object getClassLoadingLock(String className) {
        return super.getClassLoadingLock(className);
    }

    @Override
    public URL getResource(String name) {
        return super.getResource(name);
    }

    @Override
    public Enumeration<URL> getResources(String name) throws IOException {
        return super.getResources(name);
    }

    @Override
    protected Package definePackage(String name, String specTitle, String specVersion, String specVendor, String
            implTitle, String implVersion, String implVendor, URL sealBase) throws IllegalArgumentException {
        return super.definePackage(name, specTitle, specVersion, specVendor, implTitle, implVersion, implVendor,
                sealBase);
    }

    @Override
    protected Package getPackage(String name) {
        return super.getPackage(name);
    }

    @Override
    protected Package[] getPackages() {
        return super.getPackages();
    }

    @Override
    protected String findLibrary(String libname) {
        return super.findLibrary(libname);
    }

    @Override
    public void setDefaultAssertionStatus(boolean enabled) {
        super.setDefaultAssertionStatus(enabled);
    }

    @Override
    public void setPackageAssertionStatus(String packageName, boolean enabled) {
        super.setPackageAssertionStatus(packageName, enabled);
    }

    @Override
    public void setClassAssertionStatus(String className, boolean enabled) {
        super.setClassAssertionStatus(className, enabled);
    }

    @Override
    public void clearAssertionStatus() {
        super.clearAssertionStatus();
    }
}
