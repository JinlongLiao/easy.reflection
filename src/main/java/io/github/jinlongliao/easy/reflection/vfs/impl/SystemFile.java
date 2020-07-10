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

import io.github.jinlongliao.easy.reflection.util.FileUtils;
import io.github.jinlongliao.easy.reflection.vfs.Vfs;

import java.io.IOException;
import java.io.InputStream;
import java.io.FileInputStream;
import java.util.Objects;

/**
 * an implementation of {@link Vfs.File} for a directory {@link java.io.File}
 *
 * @author ronmao
 * @author liaojinlong
 * @date 2013/4/17
 */
public class SystemFile implements Vfs.File {
    private final SystemDir root;
    private final java.io.File file;
    private InputStream inputStream = null;

    public SystemFile(final SystemDir root, java.io.File file) {
        this.root = root;
        this.file = file;
    }

    @Override
    public String getName() {
        return file.getName();
    }

    @Override
    public String getRelativePath() {
        String filepath = FileUtils.getUnixPath(file.getPath());
        if (filepath.startsWith(root.getRelativePath())) {
            return filepath.substring(root.getRelativePath().length() + 1);
        }
//should not get here
        return null;
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return inputStream = new FileInputStream(file);
    }

    @Override
    public String toString() {
        return file.toString();
    }

    @Override
    public void close() throws IOException {
        if (Objects.nonNull(inputStream)) {
            inputStream.close();
            inputStream = null;
        }
    }
}
