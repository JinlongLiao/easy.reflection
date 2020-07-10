/*
 *  Copyright 2019-2020
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
package org.easy.reflection.util;

import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;


/**
 * File 相关操作Utils
 *
 * @author liaojinlong
 * @date 2020/6/25 23:25
 */
public class FileUtils {
    private static Logger log = LogUtil.findLogger(FileUtils.class);

    /**
     * 转换路径位 Unix 形式
     *
     * @param path
     * @return UNIX 路径形式
     */
    public static String getUnixPath(String path) {
        if (!OsUtil.NOT_WIN.get()) {
            path = path.replace("\\", "/");
        }
        return path;
    }

    /**
     * java 包路径转系统文件路径
     *
     * @param classPath
     * @return 系统文件路径
     */
    public static synchronized String classPath2SystemPath(String classPath) {
        return classPath.replace('.', '/');
    }

    /**
     * 系统文件路径转Java 包路径
     *
     * @param systemPath
     * @return java 全路径
     */
    public static synchronized String systemPath2ClassPath(String systemPath) {
        return getUnixPath(systemPath).replace('/', '.');
    }

    /**
     * 创建文件
     *
     * @param filename
     * @return / {@link java.io.File}
     */
    public static File prepareFile(String filename) {
        File file = new File(filename);
        File dir = file.getAbsoluteFile().getParentFile();
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return file;
    }

    /**
     * 输入流关闭
     *
     * @param inputStream
     */
    public static void close(InputStream inputStream) {
        try {
            if (inputStream != null) {
                inputStream.close();
            }
        } catch (IOException e) {
            log.error("Could not close InputStream", e);
        }
    }

}
