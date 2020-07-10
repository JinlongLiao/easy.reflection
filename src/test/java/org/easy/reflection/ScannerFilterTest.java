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

package org.easy.reflection;

import org.easy.reflection.util.ScannerFilter;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test filtering
 */
public class ScannerFilterTest {

    @Test
    public void test_include() {
        ScannerFilter filter = new ScannerFilter().include("org.easy.reflection.*");
        assertTrue(filter.test("org.easy.reflection.Reflections"));
        assertTrue(filter.test("org.easy.reflection.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
    }

    @Test
    public void test_includePackage() {
        ScannerFilter filter = new ScannerFilter().includePackage("org.easy.reflection");
        assertTrue(filter.test("org.easy.reflection.Reflections"));
        assertTrue(filter.test("org.easy.reflection.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
    }

    @Test
    public void test_includePackageMultiple() {
        ScannerFilter filter = new ScannerFilter().includePackage("org.easy.reflection", "org.foo");
        assertTrue(filter.test("org.easy.reflection.Reflections"));
        assertTrue(filter.test("org.easy.reflection.foo.Reflections"));
        assertTrue(filter.test("org.foo.Reflections"));
        assertTrue(filter.test("org.foo.bar.Reflections"));
        assertFalse(filter.test("org.bar.Reflections"));
    }

    @Test
    public void test_includePackagebyClass() {
        ScannerFilter filter = new ScannerFilter().includePackage(Reflections.class);
        assertTrue(filter.test("org.easy.reflection.Reflections"));
        assertTrue(filter.test("org.easy.reflection.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_exclude() {
        ScannerFilter filter = new ScannerFilter().exclude("org\\.easy\\.reflection.*");
        assertFalse(filter.test("org.easy.reflection.Reflections"));
        assertFalse(filter.test("org.easy.reflection.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
    }

    @Test
    public void test_excludePackage() {
        ScannerFilter filter = new ScannerFilter().excludePackage("org.easy.reflection");
        assertFalse(filter.test("org.easy.reflection.Reflections"));
        assertFalse(filter.test("org.easy.reflection.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
    }

    @Test
    public void test_excludePackageByClass() {
        ScannerFilter filter = new ScannerFilter().excludePackage(Reflections.class);
        assertFalse(filter.test("org.easy.reflection.Reflections"));
        assertFalse(filter.test("org.easy.reflection.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parse_include() {
        ScannerFilter filter = ScannerFilter.parse("+org.easy.reflection.*");
        assertTrue(filter.test("org.easy.reflection.Reflections"));
        assertTrue(filter.test("org.easy.reflection.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
    }

    @Test
    public void test_parse_include_notRegex() {
        ScannerFilter filter = ScannerFilter.parse("+org.easy.reflection");
        assertFalse(filter.test("org.easy.reflection.Reflections"));
        assertFalse(filter.test("org.easy.reflection.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
    }

    @Test
    public void test_parse_exclude() {
        ScannerFilter filter = ScannerFilter.parse("-org.easy.reflection.*");
        assertFalse(filter.test("org.easy.reflection.Reflections"));
        assertFalse(filter.test("org.easy.reflection.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
    }

    @Test
    public void test_parse_exclude_notRegex() {
        ScannerFilter filter = ScannerFilter.parse("-org.easy.reflection");
        assertTrue(filter.test("org.easy.reflection.Reflections"));
        assertTrue(filter.test("org.easy.reflection.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
    }

    @Test
    public void test_parse_include_exclude() {
        ScannerFilter filter = ScannerFilter.parse("+org.easy.reflection.*, -org.easy.reflection.foo.*");
        assertTrue(filter.test("org.easy.reflection.Reflections"));
        assertFalse(filter.test("org.easy.reflection.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
    }

    //-----------------------------------------------------------------------
    @Test
    public void test_parsePackages_include() {
        ScannerFilter filter = ScannerFilter.parsePackages("+org.easy.reflection");
        assertTrue(filter.test("org.easy.reflection.Reflections"));
        assertTrue(filter.test("org.easy.reflection.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
    }

    @Test
    public void test_parsePackages_include_trailingDot() {
        ScannerFilter filter = ScannerFilter.parsePackages("+org.easy.reflection.");
        assertTrue(filter.test("org.easy.reflection.Reflections"));
        assertTrue(filter.test("org.easy.reflection.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
    }

    @Test
    public void test_parsePackages_exclude() {
        ScannerFilter filter = ScannerFilter.parsePackages("-org.easy.reflection");
        assertFalse(filter.test("org.easy.reflection.Reflections"));
        assertFalse(filter.test("org.easy.reflection.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
    }

    @Test
    public void test_parsePackages_exclude_trailingDot() {
        ScannerFilter filter = ScannerFilter.parsePackages("-org.easy.reflection.");
        assertFalse(filter.test("org.easy.reflection.Reflections"));
        assertFalse(filter.test("org.easy.reflection.foo.Reflections"));
        assertTrue(filter.test("org.foobar.Reflections"));
    }

    @Test
    public void test_parsePackages_include_exclude() {
        ScannerFilter filter = ScannerFilter.parsePackages("+org.easy.reflection, -org.easy.reflection.foo");
        assertTrue(filter.test("org.easy.reflection.Reflections"));
        assertFalse(filter.test("org.easy.reflection.foo.Reflections"));
        assertFalse(filter.test("org.foobar.Reflections"));
    }

}
