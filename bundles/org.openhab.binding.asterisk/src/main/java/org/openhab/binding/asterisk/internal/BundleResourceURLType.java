/**
 * Copyright (c) 2010-2021 Contributors to the openHAB project
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package org.openhab.binding.asterisk.internal;

import static org.osgi.framework.wiring.BundleWiring.FINDENTRIES_RECURSE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Collection;
import java.util.stream.Collectors;

import org.asteriskjava.manager.event.ManagerEvent;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.wiring.BundleWiring;
import org.reflections.vfs.Vfs.Dir;
import org.reflections.vfs.Vfs.File;
import org.reflections.vfs.Vfs.UrlType;

/**
 * Add capability to handle OSGI bundles to the Reflections Library.
 * Used by the asterisk library to register the events it uses.
 * 
 * @author Eric de Boer - Initial contribution
 */
public class BundleResourceURLType implements UrlType {

    @Override
    public boolean matches(URL url) {
        return "bundleresource".equals(url.getProtocol());
    }

    @Override
    public Dir createDir(URL url) {

        return new Dir() {

            @Override
            public String getPath() {
                return "/";
            }

            @Override
            public void close() {
            }

            @Override
            public Iterable<File> getFiles() {
                Bundle bundle = FrameworkUtil.getBundle(ManagerEvent.class);
                Collection<String> resourcePaths = bundle.adapt(BundleWiring.class)
                        .listResources("org/asteriskjava/manager/event", "*", FINDENTRIES_RECURSE);
                return resourcePaths.stream().map(this::toFile).collect(Collectors.toSet());
            }

            private File toFile(String resourcePath) {
                return new File() {
                    @Override
                    public String getName() {
                        return resourcePath.substring(resourcePath.lastIndexOf("/") + 1);
                    }

                    @Override
                    public String getRelativePath() {
                        return resourcePath;
                    }

                    @Override
                    public InputStream openInputStream() throws IOException {
                        Bundle bundle = FrameworkUtil.getBundle(ManagerEvent.class);
                        return bundle.getEntry(resourcePath).openStream();
                    }
                };
            }
        };
    }
}
