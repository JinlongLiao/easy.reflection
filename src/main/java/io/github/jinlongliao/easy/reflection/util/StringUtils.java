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

import java.util.Collection;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * 字符串相关操作工具类
 *
 * @author liaojinlong
 * @date 2020/6/24 23:01
 */
public class StringUtils {
    /**
     * 字符串是否为NULL 或者空字符串
     *
     * @param str
     * @return / /
     */
    public static synchronized boolean isEmpty(String str) {
        return str == null || str.length() < 1;
    }

    /**
     * 字符串是否为NULL 或者空字符串或者空白字符串
     *
     * @param str
     * @return / /
     */
    public static synchronized boolean isBlank(String str) {
        return isEmpty(str) || str.trim().length() < 1;
    }

    /**
     * 字符串重复组合方法
     *
     * @param string 字符串
     * @param times  重复次数
     * @return /
     */
    public static synchronized String repeat(String string, int times) {
        return repeat(string, "", times);
    }

    /**
     * 字符串 以 特定字符串拼接组合N此
     *
     * @param string
     * @param join
     * @param times
     * @return /
     */
    public static synchronized String repeat(String string, String join, int times) {
        return IntStream.range(0, times).mapToObj(i -> string).collect(Collectors.joining(join));
    }

    /**
     * 将集合中的 数据 以 delimiter 组合
     *
     * @param elements
     * @param delimiter
     * @return /
     */
    public static synchronized String append(Collection<?> elements, String delimiter) {
        return elements.stream().map(Object::toString).collect(Collectors.joining(delimiter));
    }

    /**
     * 数据组合
     *
     * @param elements
     * @return /
     */
    public static synchronized String append(CharSequence... elements) {
        StringBuilder stringBuilder = new StringBuilder();
        for (CharSequence element : elements) {
            stringBuilder.append(element);
        }
        return stringBuilder.toString();
    }
}
