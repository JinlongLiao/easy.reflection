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

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.zip.ZipEntry;

/**
 * an implementation of {@link Vfs.File} for {@link java.util.zip.ZipEntry}
 */
public class ZipFile implements Vfs.File {
    private final ZipDir root;
    private final ZipEntry entry;
    private InputStream inputStream = null;

    public ZipFile(final ZipDir root, ZipEntry entry) {
        this.root = root;
        this.entry = entry;
    }

    @Override
    public String getName() {
        String name = entry.getName();
        return name.substring(name.lastIndexOf("/") + 1);
    }

    @Override
    public String getRelativePath() {
        return entry.getName();
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return inputStream = root.jarFile.getInputStream(entry);
    }

    @Override
    public String toString() {
        return root.getRelativePath() + "!" + java.io.File.separatorChar + entry.getName();
    }

    @Override
    public void close() throws IOException {
        if (Objects.nonNull(inputStream)) {
            inputStream.close();
            inputStream = null;
        }
    }
}
