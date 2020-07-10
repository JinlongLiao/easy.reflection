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

import java.io.IOException;
import java.net.ServerSocket;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 操作系统工具类
 *
 * @author liaojinlong
 */
public class OsUtil {
    private static Logger log = LogUtil.findLogger(OsUtil.class);
    private static final String OS = System.getProperty("os.name").toLowerCase();
    private static final String ARCH = System.getProperty("sun.arch.data.model");
    public static final AtomicBoolean NOT_WIN;

    /**
     * 是否为 LINUX ，BSD,UNIX 等非 WINDOWS 系统
     */
    public static final boolean UNIX_LIKE;

    static {
        UNIX_LIKE = !isWindows();
        NOT_WIN = new AtomicBoolean(UNIX_LIKE);
    }

    /**
     * 查看指定的端口号是否空闲，若空闲则返回否则返回一个随机的空闲端口号
     *
     * @return /
     */
    public static int getFreePort(int defaultPort) throws IOException {
        try (
                ServerSocket serverSocket = new ServerSocket(defaultPort)
        ) {
            return serverSocket.getLocalPort();
        } catch (IOException e) {
            return getFreePort();
        }
    }

    /**
     * 获取空闲端口号
     *
     * @return 空闲端口号
     */
    public static int getFreePort() throws IOException {
        try (
                ServerSocket serverSocket = new ServerSocket(0)
        ) {
            return serverSocket.getLocalPort();
        }
    }

    /**
     * 检查端口号是否被占用
     *
     * @param port
     * @return 端口是否有效
     */
    public static boolean isBusyPort(int port) {
        boolean ret = true;
        ServerSocket serverSocket = null;
        try {
            serverSocket = new ServerSocket(port);
            ret = false;
        } catch (Exception e) {
        } finally {
            if (serverSocket != null) {
                try {
                    serverSocket.close();
                } catch (IOException e) {
                    log.error(e.getMessage(), e);
                }
            }
        }
        return ret;
    }


    /**
     * @return 是否 windows
     */
    public static boolean isWindows() {
        return OS.indexOf("win") >= 0;
    }

    /**
     * @return 是否 winXP
     */
    public static boolean isWindowsXP() {
        return OS.indexOf("win") >= 0 && OS.indexOf("xp") >= 0;
    }

    /**
     * @return 是否 MACX OS
     */
    public static boolean isMac() {
        return OS.indexOf("mac") >= 0;
    }

    /**
     * @return 是否UNIX
     */
    public static boolean isUnix() {
        return OS.indexOf("nix") >= 0 || OS.indexOf("nux") >= 0 || OS.indexOf("aix") >= 0;
    }

    /**
     * @return 是否 Solaris
     */
    public static boolean isSolaris() {
        return (OS.indexOf("sunos") >= 0);
    }


    /**
     * @return OS 是否 64位
     */
    public static boolean is64() {
        return "64".equals(ARCH);
    }

    /**
     * @return OS 是否 32位
     */
    public static boolean is32() {
        return "32".equals(ARCH);
    }
}