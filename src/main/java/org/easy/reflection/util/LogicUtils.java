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

package org.easy.reflection.util;


import org.slf4j.Logger;

import java.util.Arrays;
import java.util.Collection;

import java.util.Set;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * 逻辑相关 工具类
 *
 * @author liaojinlong
 * @date 2020/6/25 23:52
 */
public class LogicUtils {
    private static Logger log = LogUtil.findLogger(LogicUtils.class);

    /**
     * 逻辑 与
     *
     * @param predicates
     * @param <T>
     * @return /
     */
    public static <T> Predicate<T> and(Predicate... predicates) {
        return Arrays.stream(predicates).reduce(t -> true, Predicate::and);
    }

    /**
     * @param result
     * @param predicates
     * @param <T>
     * @return /
     */
    public static <T> Set<T> filter(Collection<T> result, Predicate<? super T>... predicates) {
        return result.stream().filter(and(predicates)).collect(Collectors.toSet());
    }

    /**
     * @param result
     * @param predicate
     * @param <T>
     * @return /
     */
    public static <T> Set<T> filter(Collection<T> result, Predicate<? super T> predicate) {
        return result.stream().filter(predicate).collect(Collectors.toSet());
    }

    /**
     * @param result
     * @param predicates
     * @param <T>
     * @return /
     */
    public static <T> Set<T> filter(T[] result, Predicate<? super T>... predicates) {
        return Arrays.stream(result).filter(and(predicates)).collect(Collectors.toSet());
    }
}
