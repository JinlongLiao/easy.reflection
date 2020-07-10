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

package io.github.jinlongliao.easy.reflection.vfs.impl;

import io.github.jinlongliao.easy.reflection.vfs.Vfs;
import io.github.jinlongliao.easy.reflection.util.LogUtil;
import org.slf4j.Logger;

import java.io.*;
import java.util.Objects;
import java.util.jar.JarFile;

/**
 * an implementation of {@link Vfs.Dir} for {@link java.util.zip.ZipFile}
 *
 * @author ronmamo
 */
public class ZipDir implements Vfs.Dir {
    public final java.util.zip.ZipFile jarFile;
    private InputStream inputStream = null;
    private static Logger log = LogUtil.findLogger(ZipDir.class);

    public ZipDir(JarFile jarFile) {
        this.jarFile = jarFile;
    }

    /**
     * 获取文件名称
     *
     * @return /
     */
    @Override
    public String getName() {
        return jarFile.getName();
    }

    @Override
    public String getRelativePath() {
        return jarFile.getName();
    }

    /**
     * 获取文件输入流
     *
     * @return /
     * @throws IOException
     */
    @Override
    public InputStream openInputStream() throws IOException {
        return inputStream = new FileInputStream(jarFile.getName());

    }

    @Override
    public Iterable<Vfs.File> getFiles() {
        return () -> jarFile.stream()
                .filter(entry -> !entry.isDirectory())
                .map(entry -> (Vfs.File) new ZipFile(ZipDir.this, entry))
                .iterator();
    }

    @Override
    public void close() throws IOException {
        if (Objects.nonNull(inputStream)) {
            inputStream.close();
            inputStream = null;
        }
        try {
            jarFile.close();
        } catch (IOException e) {
            log.warn("Could not close JarFile", e);
            throw e;
        }
    }

    @Override
    public String toString() {
        return getName();
    }
}
