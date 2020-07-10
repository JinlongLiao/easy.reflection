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

package io.github.jinlongliao.easy.reflection.vfs;

import io.github.jinlongliao.easy.reflection.exception.ReflectionsException;
import io.github.jinlongliao.easy.reflection.util.LogUtil;
import io.github.jinlongliao.easy.reflection.vfs.impl.ZipDir;
import org.slf4j.Logger;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * UrlType to be used by Reflections library.
 * This class handles the vfszip and vfsfile protocol of JBOSS files.
 * <p>
 * <p>to use it, register it in Vfs via {@link Vfs#addDefaultURLTypes(Vfs.UrlType)} or {@link Vfs#setDefaultURLTypes(java.util.List)}.
 *
 * @author Sergio Pola
 */
public class UrlTypeVfs implements Vfs.UrlType {
    private static Logger log = LogUtil.findLogger(UrlTypeVfs.class);

    public final static String[] REPLACE_EXTENSION = new String[]{".ear/", ".jar/", ".war/", ".sar/", ".har/", ".par/"};
    private static final Pattern PATTERN = Pattern.compile("\\.[ejprw]ar/");
    private static final String VFS_ZIP = "vfszip";
    private static final String VFS_FILE = "vfsfile";

    @Override
    public boolean matches(URL url) {
        return VFS_ZIP.equals(url.getProtocol()) || VFS_FILE.equals(url.getProtocol());
    }

    @Override
    public Vfs.Dir createDir(final URL url) {
        try {
            URL adaptedUrl = adaptURL(url);
            return new ZipDir(new JarFile(adaptedUrl.getFile()));
        } catch (Exception e) {
            try {
                return new ZipDir(new JarFile(url.getFile()));
            } catch (IOException e1) {
                log.warn("Could not get URL", e);
                log.warn("Could not get URL", e1);
            }
        }
        return null;
    }

    public URL adaptURL(URL url) throws MalformedURLException {
        if (VFS_ZIP.equals(url.getProtocol())) {
            return replaceZipSeparators(url.getPath(), realFile);
        } else if (VFS_FILE.equals(url.getProtocol())) {
            return new URL(url.toString().replace(VFS_FILE, "file"));
        } else {
            return url;
        }
    }

    URL replaceZipSeparators(String path, Predicate<File> acceptFile)
            throws MalformedURLException {
        int pos = 0;
        while (pos != -1) {
            pos = findFirstMatchofDeployableExtention(path, pos);

            if (pos > 0) {
                File file = new File(path.substring(0, pos - 1));
                if (acceptFile.test(file)) {
                    return replaceZipSeparatorStartingFrom(path, pos);
                }
            }
        }

        throw new ReflectionsException("Unable to identify the real zip file in path '" + path + "'.");
    }

    int findFirstMatchofDeployableExtention(String path, int pos) {
        Matcher m = PATTERN.matcher(path);
        if (m.find(pos)) {
            return m.end();
        } else {
            return -1;
        }
    }

    Predicate<File> realFile = file -> file.exists() && file.isFile();

    URL replaceZipSeparatorStartingFrom(String path, int pos)
            throws MalformedURLException {
        String zipFile = path.substring(0, pos - 1);
        String zipPath = path.substring(pos);

        int numSubs = 1;
        for (String ext : REPLACE_EXTENSION) {
            while (zipPath.contains(ext)) {
                zipPath = zipPath.replace(ext, ext.substring(0, 4) + "!");
                numSubs++;
            }
        }

        String prefix = "";
        for (int i = 0; i < numSubs; i++) {
            prefix += "zip:";
        }

        if (zipPath.trim().length() == 0) {
            return new URL(prefix + "/" + zipFile);
        } else {
            return new URL(prefix + "/" + zipFile + "!" + zipPath);
        }
    }
}
