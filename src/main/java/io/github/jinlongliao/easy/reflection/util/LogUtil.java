
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
package io.github.jinlongliao.easy.reflection.util;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.Marker;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 日志适配类，使用 slf4j 作为代理类
 *
 * @author liaojinlong
 * @date 2020/6/24 23:23
 */
public class LogUtil {
    private static AtomicBoolean class_loader_flag = new AtomicBoolean(false);
    private static AtomicBoolean class_loader_error = new AtomicBoolean(true);
    private static Map<Class, Logger> LOGGER_CACHE = new ConcurrentHashMap<>(32);

    /**
     * 获取当前时间 字符串
     *
     * @return /
     */
    private static String getCurrDateTime() {
        return DateTimeFormatter.ISO_DATE_TIME.format(LocalDateTime.now());
    }

    /**
     * current_threadId : time：级别：msg
     */
    public static final String WARN = "thread:%s-%s time: %s warn : %s";
    public static final String ERROR = "thread:%s-%s time: %s error : %s";

    /**
     * Log 框架适配，为避免此方法被滥用
     * <code>
     * Class.forName("org.slf4j.impl.StaticLoggerBinder");
     * </code>
     * 仅第一次加载
     *
     * @param aClass
     * @return / {@link org.slf4j.Logger}
     */
    private static Logger findLogger0(Class<?> aClass) {
        if (class_loader_error.get()) {
            try {
                if (!class_loader_flag.get()) {
                    Class.forName("org.slf4j.impl.StaticLoggerBinder");
                    class_loader_flag.set(true);
                }
                // This is to check whether an optional SLF4J binding is available. While SLF4J recommends that libraries
                // "should not declare a dependency on any SLF4J binding but only depend on slf4j-api", doing so forces
                // users of the library to either add a binding to the classpath (even if just slf4j-nop) or to set the
                // "slf4j.suppressInitError" system property in order to avoid the warning, which both is inconvenient.
                return LoggerFactory.getLogger(aClass);
            } catch (Throwable e) {
                new InnerProxyLogger(null, LogUtil.class).error(e.getMessage(), e);
                class_loader_error.set(false);
                return null;
            }
        } else {
            return null;
        }
    }

    public static final Lock lock = new ReentrantLock();

    /**
     * 统一对外暴露 Logger
     *
     * @param aClass
     * @return / {@link LogUtil.InnerProxyLogger}
     */
    public static Logger findLogger(Class<?> aClass) {
        Logger logger;
        if (LOGGER_CACHE.containsKey(aClass)) {
            logger = LOGGER_CACHE.get(aClass);
        } else {
            lock.lock();
            logger = new InnerProxyLogger(findLogger0(aClass), aClass);
            LOGGER_CACHE.put(aClass, logger);
            lock.unlock();
        }
        return logger;
    }

    /**
     * 内部代理Logger，在 slf4j 失效时 使用 System.out /System.err  等输出
     *
     * @author liaojinlong
     * @date 2020/6/24 23:28
     */
    static class InnerProxyLogger implements Logger {
        private final Class aClass;
        private Logger logger;
        private final boolean CAN_USE_LOGGER;

        public InnerProxyLogger(Logger logger, Class aClass) {
            this.logger = logger;
            this.aClass = aClass;
            if (Objects.nonNull(logger)) {
                CAN_USE_LOGGER = true;
            } else {
                CAN_USE_LOGGER = false;
            }

        }

        @Override
        public String getName() {
            if (CAN_USE_LOGGER) {
                return logger.getName();
            }
            return "InnerProxyLogger" + this.aClass.getName();
        }

        @Override
        public boolean isTraceEnabled() {
            if (CAN_USE_LOGGER) {
                return logger.isTraceEnabled();
            }
            return false;
        }

        @Override
        public void trace(String msg) {
            if (isTraceEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.trace(msg);
                }
            }
        }

        @Override
        public void trace(String format, Object arg) {
            if (isTraceEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.trace(format, arg);
                }
            }
        }

        @Override
        public void trace(String format, Object arg1, Object arg2) {
            if (isTraceEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.trace(format, arg1, arg2);
                }
            }
        }

        @Override
        public void trace(String format, Object... arguments) {
            if (isTraceEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.trace(format, arguments);
                }
            }
        }

        @Override
        public void trace(String msg, Throwable t) {
            if (isTraceEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.trace(msg, t);
                }
            }
        }

        @Override
        public boolean isTraceEnabled(Marker marker) {
            if (CAN_USE_LOGGER) {
                return logger.isTraceEnabled(marker);
            }
            return false;
        }

        @Override
        public void trace(Marker marker, String msg) {
            if (isTraceEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.trace(marker, msg);
                }
            }
        }

        @Override
        public void trace(Marker marker, String format, Object arg) {
            if (isTraceEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.trace(marker, format, arg);
                }
            }
        }

        @Override
        public void trace(Marker marker, String format, Object arg1, Object arg2) {
            if (isTraceEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.trace(marker, format, arg1, arg2);
                }
            }
        }

        @Override
        public void trace(Marker marker, String format, Object... argArray) {
            if (isTraceEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.trace(marker, format, argArray);
                }
            }
        }

        @Override
        public void trace(Marker marker, String msg, Throwable t) {
            if (isTraceEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.trace(marker, msg, t);
                }
            }
        }

        @Override
        public boolean isDebugEnabled() {
            if (CAN_USE_LOGGER) {
                return logger.isDebugEnabled();
            }
            return false;
        }

        @Override
        public void debug(String msg) {
            if (isDebugEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.debug(msg);
                }
            }
        }

        @Override
        public void debug(String format, Object arg) {
            if (isDebugEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.debug(format, arg);
                }
            }
        }

        @Override
        public void debug(String format, Object arg1, Object arg2) {
            if (isDebugEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.debug(format, arg1, arg2);
                }
            }
        }

        @Override
        public void debug(String format, Object... arguments) {
            if (isDebugEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.debug(format, arguments);
                }
            }
        }

        @Override
        public void debug(String msg, Throwable t) {
            if (isDebugEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.debug(msg, t);
                }
            }
        }

        @Override
        public boolean isDebugEnabled(Marker marker) {
            if (CAN_USE_LOGGER) {
                return logger.isDebugEnabled(marker);
            }
            return false;
        }

        @Override
        public void debug(Marker marker, String msg) {
            if (isDebugEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.debug(marker, msg);
                }
            }
        }

        @Override
        public void debug(Marker marker, String format, Object arg) {
            if (isDebugEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.debug(marker, format, arg);
                }
            }
        }

        @Override
        public void debug(Marker marker, String format, Object arg1, Object arg2) {
            if (isDebugEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.debug(marker, format, arg1, arg2);
                }
            }
        }

        @Override
        public void debug(Marker marker, String format, Object... arguments) {
            if (isDebugEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.debug(marker, format, arguments);
                }
            }
        }

        @Override
        public void debug(Marker marker, String msg, Throwable t) {
            if (isDebugEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.debug(marker, msg, t);
                }
            }
        }

        @Override
        public boolean isInfoEnabled() {
            if (CAN_USE_LOGGER) {
                return logger.isInfoEnabled();
            }
            return false;
        }

        @Override
        public void info(String msg) {
            if (isInfoEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.info(msg);
                }
            }
        }

        @Override
        public void info(String format, Object arg) {
            if (isInfoEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.info(format, arg);
                }
            }
        }

        @Override
        public void info(String format, Object arg1, Object arg2) {
            if (isInfoEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.info(format, arg1, arg2);
                }
            }
        }

        @Override
        public void info(String format, Object... arguments) {
            if (isInfoEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.info(format, arguments);
                }
            }
        }

        @Override
        public void info(String msg, Throwable t) {
            if (isInfoEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.info(msg, t);
                }
            }
        }

        @Override
        public boolean isInfoEnabled(Marker marker) {
            if (CAN_USE_LOGGER) {
                return logger.isInfoEnabled(marker);
            }
            return false;
        }

        @Override
        public void info(Marker marker, String msg) {
            if (isInfoEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.info(marker, msg);
                }
            }
        }

        @Override
        public void info(Marker marker, String format, Object arg) {
            if (isInfoEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.info(marker, format, arg);
                }
            }
        }

        @Override
        public void info(Marker marker, String format, Object arg1, Object arg2) {
            if (isInfoEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.info(marker, format, arg1, arg2);
                }
            }
        }

        @Override
        public void info(Marker marker, String format, Object... arguments) {
            if (isInfoEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.info(marker, format, arguments);
                }
            }
        }

        @Override
        public void info(Marker marker, String msg, Throwable t) {
            if (isInfoEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.info(marker, msg, t);
                }
            }
        }

        @Override
        public boolean isWarnEnabled() {
            if (CAN_USE_LOGGER) {
                return logger.isWarnEnabled();
            }
            return true;
        }

        @Override
        public void warn(String msg) {
            if (isWarnEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.info(msg);
                } else {
                    innerLog(LogType.WARN, msg);
                }
            }
        }

        @Override
        public void warn(String format, Object arg) {
            if (isWarnEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.warn(format);
                } else {
                    innerLog(LogType.WARN, format, arg);
                }
            }
        }

        @Override
        public void warn(String format, Object... arguments) {
            if (isWarnEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.warn(format);
                } else {
                    innerLog(LogType.WARN, format, arguments);
                }
            }
        }

        @Override
        public void warn(String format, Object arg1, Object arg2) {
            if (isWarnEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.warn(format);
                } else {
                    innerLog(LogType.WARN, format, arg1, arg2);
                }
            }
        }

        @Override
        public void warn(String msg, Throwable t) {
            if (isWarnEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.warn(msg, t);
                } else {
                    innerLog(LogType.WARN, msg, t);
                }
            }
        }

        @Override
        public boolean isWarnEnabled(Marker marker) {
            if (CAN_USE_LOGGER) {
                return logger.isWarnEnabled(marker);
            }
            return true;
        }

        @Override
        public void warn(Marker marker, String msg) {
            if (isWarnEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.warn(msg);
                } else {
                    innerLog(LogType.WARN, msg);
                }
            }
        }

        @Override
        public void warn(Marker marker, String format, Object arg) {
            if (isWarnEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.warn(format, arg);
                } else {
                    innerLog(LogType.WARN, format, arg);
                }
            }
        }

        @Override
        public void warn(Marker marker, String format, Object arg1, Object arg2) {
            if (isWarnEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.warn(format, arg1, arg2);
                } else {
                    innerLog(LogType.WARN, format, arg1, arg2);
                }
            }
        }

        @Override
        public void warn(Marker marker, String format, Object... arguments) {
            if (isWarnEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.warn(format, arguments);
                } else {
                    innerLog(LogType.WARN, format, arguments);
                }
            }
        }

        @Override
        public void warn(Marker marker, String msg, Throwable t) {
            if (isWarnEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.warn(marker, msg, t);
                } else {
                    innerLog(LogType.WARN, msg, t);
                }
            }
        }

        @Override
        public boolean isErrorEnabled() {
            if (CAN_USE_LOGGER) {
                return logger.isErrorEnabled();
            }
            return true;
        }

        @Override
        public void error(String msg) {
            if (isErrorEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.error(msg);
                } else {
                    innerLog(LogType.ERROR, msg);
                }
            }
        }

        @Override
        public void error(String format, Object arg) {
            if (isErrorEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.error(format, arg);
                } else {
                    innerLog(LogType.ERROR, format, arg);
                }
            }
        }

        @Override
        public void error(String format, Object arg1, Object arg2) {
            if (isErrorEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.error(format, arg1, arg2);
                } else {
                    innerLog(LogType.ERROR, format, arg1, arg2);
                }
            }
        }

        @Override
        public void error(String format, Object... arguments) {
            if (isErrorEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.error(format, arguments);
                } else {
                    innerLog(LogType.ERROR, format, arguments);
                }
            }
        }

        @Override
        public void error(String msg, Throwable t) {
            if (isErrorEnabled()) {
                if (CAN_USE_LOGGER) {
                    logger.error(msg, t);
                } else {
                    innerLog(LogType.ERROR, msg, t);
                }
            }
        }

        @Override
        public boolean isErrorEnabled(Marker marker) {
            if (CAN_USE_LOGGER) {
                return logger.isErrorEnabled(marker);
            }
            return true;
        }

        @Override
        public void error(Marker marker, String msg) {
            if (isErrorEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.error(msg);
                } else {
                    innerLog(LogType.ERROR, msg);
                }
            }
        }

        @Override
        public void error(Marker marker, String format, Object arg) {
            if (isErrorEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.error(marker, format, arg);
                } else {
                    innerLog(LogType.ERROR, format, arg);
                }
            }
        }

        @Override
        public void error(Marker marker, String format, Object arg1, Object arg2) {
            if (isErrorEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.error(marker, format, arg1, arg2);
                } else {
                    innerLog(LogType.ERROR, format, arg1, arg2);
                }
            }
        }

        @Override
        public void error(Marker marker, String format, Object... arguments) {
            if (isErrorEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.error(marker, format, arguments);
                } else {
                    innerLog(LogType.ERROR, format, arguments);
                }
            }
        }

        @Override
        public void error(Marker marker, String msg, Throwable t) {
            if (isErrorEnabled(marker)) {
                if (CAN_USE_LOGGER) {
                    logger.error(marker, msg, t);
                } else {
                    innerLog(LogType.ERROR, msg, t);
                }
            }
        }

        /**
         * 输出 日志信息
         *
         * @param logType
         * @param msg
         * @param argument
         */
        private void innerLog(LogType logType, String msg, Object... argument) {
            String format;
            Thread thread;
            switch (logType) {
                case WARN:
                    thread = Thread.currentThread();
                    format = String.format(WARN, thread.getName(), thread.getId(), getCurrDateTime(), msg);
                    if (argument != null || argument.length > 0) {
                        format = format.replaceAll("\\{\\}", "%s");
                        format = String.format(format, argument);
                    }
                    System.out.printf(format);
                    break;
                case ERROR:
                    thread = Thread.currentThread();
                    format = String.format(ERROR, thread.getName(), thread.getId(), getCurrDateTime(), msg);
                    if (argument != null || argument.length > 0) {
                        format = format.replaceAll("\\{\\}", "%s");
                        format = String.format(format, argument);
                    }
                    System.err.printf(format);
                    break;
                case TRACE:
                case DEBUG:
                case INFO:
                default:
                    break;
            }
        }

        /**
         * 输出错误日志信息
         *
         * @param logType
         * @param msg
         * @param throwable
         */
        private void innerLog(LogType logType, String msg, Throwable throwable) {
            String format;
            Thread thread;
            switch (logType) {
                case WARN:
                    thread = Thread.currentThread();
                    format = String.format(WARN, thread.getName(), thread.getId(), getCurrDateTime(), msg);
                    System.out.println(format);
                    throwable.printStackTrace();
                    break;
                case ERROR:
                    thread = Thread.currentThread();
                    format = String.format(ERROR, thread.getName(), thread.getId(), getCurrDateTime(), msg);
                    System.err.println(format);
                    throwable.printStackTrace();
                    break;
                case TRACE:
                case DEBUG:
                case INFO:
                default:
                    break;
            }
        }
    }

    /**
     * 日志类型:仅供本类适配 输出, TRACE, DEBUG, INFO 未实现
     *
     * @author liaojinlong
     * @date 2020/6/25 00:21
     * @see {@link InnerProxyLogger#innerLog(LogUtil.LogType, java.lang.String, java.lang.Object...)}
     */
    private enum LogType {
        TRACE, DEBUG, INFO, WARN, ERROR
    }
}
