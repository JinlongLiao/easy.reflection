/*
 *  Copyright 2019-2020 author
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package org.easy.reflection.vfs.impl;

import org.easy.reflection.exception.ReflectionsException;
import org.easy.reflection.vfs.Vfs;
import org.easy.reflection.util.FileUtils;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;
import java.util.jar.JarInputStream;
import java.util.zip.ZipEntry;

/**
 * @author romamo
 */
public class JarInputDir implements Vfs.Dir {
    private final URL url;
    JarInputStream jarInputStream;
    long cursor = 0;
    long nextCursor = 0;

    public JarInputDir(URL url) {
        this.url = url;
    }

    /**
     * 获取文件名称
     *
     * @return /
     */
    @Override
    public String getName() {
        return this.url.getFile();
    }

    @Override
    public String getRelativePath() {
        return url.getPath();
    }

    /**
     * 获取文件输入流
     *
     * @return /
     * @throws IOException
     */
    @Override
    public InputStream openInputStream() throws IOException {
        try {
            jarInputStream = new JarInputStream(url.openConnection().getInputStream());
        } catch (Exception e) {
            throw new ReflectionsException("Could not open url connection", e);
        }
        return jarInputStream;
    }

    @Override
    public Iterable<Vfs.File> getFiles() {
        return () -> new Iterator<Vfs.File>() {
            Vfs.File entry = null;

            @Override
            public boolean hasNext() {
                return entry != null || (entry = computeNext()) != null;
            }

            @Override
            public Vfs.File next() {
                Vfs.File next = entry;
                entry = null;
                return next;
            }

            private Vfs.File computeNext() {
                try {
                    openInputStream();
                    while (true) {
                        ZipEntry entry = jarInputStream.getNextJarEntry();
                        if (entry == null) {
                            return null;
                        }

                        long size = entry.getSize();
                        if (size < 0) {
                            size = 0xffffffffL + size; //JDK-6916399
                        }
                        nextCursor += size;
                        if (!entry.isDirectory()) {
                            return new JarInputFile(entry, JarInputDir.this, cursor, nextCursor);
                        }
                    }
                } catch (IOException e) {
                    throw new ReflectionsException("could not get next zip entry", e);
                }
            }
        };
    }

    @Override
    public void close() {
        FileUtils.close(jarInputStream);
    }
}
