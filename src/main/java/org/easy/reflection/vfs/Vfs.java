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

package org.easy.reflection.vfs;

import org.easy.reflection.exception.ReflectionsException;
import org.easy.reflection.util.ClasspathHelper;
import org.easy.reflection.vfs.impl.SystemDir;
import org.easy.reflection.util.LogUtil;
import org.easy.reflection.util.StringUtils;
import org.easy.reflection.vfs.impl.JarInputDir;
import org.easy.reflection.vfs.impl.ZipDir;
import org.slf4j.Logger;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.function.Predicate;
import java.util.jar.JarFile;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * a simple virtual file system bridge
 * <p>use the {@link Vfs#fromURL(java.net.URL)} to get a {@link Vfs.Dir},
 * then use {@link Vfs.Dir#getFiles()} to iterate over the {@link Vfs.File}
 * <p>for example:
 * <pre>
 *      Vfs.Dir dir = Vfs.fromURL(url);
 *      Iterable<Vfs.File> files = dir.getFiles();
 *      for (Vfs.File file : files) {
 *          InputStream is = file.openInputStream();
 *      }
 * </pre>
 * <p>{@link Vfs#fromURL(java.net.URL)} uses static {@link Vfs.DefaultUrlTypes} to resolve URLs.
 * It contains VfsTypes for handling for common resources such as local jar file, local directory, jar url, jar input stream and more.
 * <p>It can be plugged in with other {@link Vfs.UrlType} using {@link Vfs#addDefaultURLTypes(Vfs.UrlType)} or {@link Vfs#setDefaultURLTypes(java.util.List)}.
 * <p>for example:
 * <pre>
 *      Vfs.addDefaultURLTypes(new Vfs.UrlType() {
 *          public boolean matches(URL url)         {
 *              return url.getProtocol().equals("http");
 *          }
 *          public Vfs.Dir createDir(final URL url) {
 *              return new HttpDir(url); //implement this type... (check out a naive implementation on VfsTest)
 *          }
 *      });
 *
 *      Vfs.Dir dir = Vfs.fromURL(new URL("http://mirrors.ibiblio.org/pub/mirrors/maven2/org/slf4j/slf4j-api/1.5.6/slf4j-api-1.5.6.jar"));
 * </pre>
 * <p>use {@link Vfs#findFiles(java.util.Collection, java.util.function.Predicate)} to get an
 * iteration of files matching given name predicate over given list of urls
 */
public abstract class Vfs {
    private static List<UrlType> defaultUrlTypes = new ArrayList<>(Arrays.asList(DefaultUrlTypes.values()));
    private static final Logger log = LogUtil.findLogger(Vfs.class);
    protected static final String JAR = "jar";
    protected static final String JAR_FILE = ".jar";
    protected static final String ZIP = "zip";
    protected static final String FILE = "file";
    protected static final String WS_JAR = "wsjar";
    protected static final String VSF = "vfs";
    protected static final String VSF_FILE = "vfs_file";
    protected static final String VSF_ZIP = "vfs_zip";
    protected static final String BUNDLE = "bundle";
    protected static final String FILE_LOCATOR_CLASS = "org.eclipse.core.runtime.FileLocator";

    /**
     * an abstract vfs dir
     */
    public interface Dir extends File, Closeable {
        /**
         * 获取子文件夹的全部文件 ，包涵文件与文件夹
         *
         * @return /
         */
        Iterable<File> getFiles();
    }

    /**
     * an abstract vfs file
     */
    public interface File extends Closeable {
        /**
         * 获取文件名称
         *
         * @return /
         */
        String getName();

        /**
         * 获取相对路径
         *
         * @return /
         */
        String getRelativePath();

        /**
         * 获取文件输入流
         *
         * @return /
         * @throws IOException
         */
        InputStream openInputStream() throws IOException;
    }

    /**
     * 依据URL 匹配
     * a matcher and factory for a url
     */
    public interface UrlType {
        /**
         * 匹配是否为 相同的资源类型
         *
         * @param url
         * @return /
         * @throws Exception
         */
        boolean matches(URL url) throws Exception;

        /**
         * 新建资源
         *
         * @param url
         * @return /
         * @throws Exception
         */
        Dir createDir(URL url) throws Exception;
    }

    /**
     * the default url types that will be used when issuing {@link Vfs#fromURL(java.net.URL)}
     */
    public static List<UrlType> getDefaultUrlTypes() {
        return defaultUrlTypes;
    }

    /**
     * sets the static default url types. can be used to statically plug in urlTypes
     */
    public static void setDefaultURLTypes(final List<UrlType> urlTypes) {
        defaultUrlTypes = urlTypes;
    }

    /**
     * add a static default url types to the beginning of the default url types list. can be used to statically plug in urlTypes
     */
    public static void addDefaultURLTypes(UrlType urlType) {
        defaultUrlTypes.add(0, urlType);
    }

    /**
     * tries to create a Dir from the given url, using the defaultUrlTypes
     */
    public static Dir fromURL(final URL url) {
        return fromURL(url, defaultUrlTypes);
    }

    /**
     * tries to create a Dir from the given url, using the given urlTypes
     */
    public static Dir fromURL(final URL url, final List<UrlType> urlTypes) {
        for (UrlType urlType : urlTypes) {
            try {
                if (urlType.matches(url)) {
                    Dir dir = urlType.createDir(url);
                    if (dir != null) {
                        return dir;
                    }
                }
            } catch (Throwable e) {
                log.warn("could not create Dir using " + urlType + " from url " + url.toExternalForm() + ". skipping.", e);
            }
        }
        throw new ReflectionsException("could not create Vfs.Dir from url, no matching UrlType was found [" + url.toExternalForm() + "]\n" +
                "either use fromURL(final URL url, final List<UrlType> urlTypes) or " +
                "use the static setDefaultURLTypes(final List<UrlType> urlTypes) or addDefaultURLTypes(UrlType urlType) " +
                "with your specialized UrlType.");
    }

    /**
     * tries to create a Dir from the given url, using the given urlTypes
     */
    public static Dir fromURL(final URL url, final UrlType... urlTypes) {
        return fromURL(url, Arrays.asList(urlTypes));
    }

    /**
     * return an iterable of all {@link Vfs.File} in given urls, starting with given packagePrefix and matching nameFilter
     */
    public static Iterable<File> findFiles(final Collection<URL> inUrls, final String packagePrefix, final Predicate<String> nameFilter) {
        Predicate<File> fileNamePredicate = file -> {
            String path = file.getRelativePath();
            if (path.startsWith(packagePrefix)) {
                String filename = path.substring(path.indexOf(packagePrefix) + packagePrefix.length());
                return !StringUtils.isBlank(filename) && nameFilter.test(filename.substring(1));
            } else {
                return false;
            }
        };
        return findFiles(inUrls, fileNamePredicate);
    }

    /**
     * return an iterable of all {@link Vfs.File} in given urls, matching filePredicate
     */
    public static Iterable<File> findFiles(final Collection<URL> urls, final Predicate<File> filePredicate) {
        return () -> urls.stream()
                .flatMap(url -> {
                    try {
                        return StreamSupport.stream(fromURL(url).getFiles().spliterator(), false);
                    } catch (Throwable e) {

                        log.error("could not findFiles for url. continuing. [" + url + "]", e);

                        return Stream.of();
                    }
                })
                .filter(filePredicate).iterator();
    }

    /**
     * try to get {@link java.io.File} from url
     */
    public static java.io.File getFile(URL url) {
        java.io.File file;
        String path;

        try {
            path = url.toURI().getSchemeSpecificPart();
            if ((file = new java.io.File(path)).exists()) {
                return file;
            }
        } catch (URISyntaxException ignored) {
        }

        try {
            path = URLDecoder.decode(url.getPath(), "UTF-8");
            if (path.contains(".jar!")) {
                path = path.substring(0, path.lastIndexOf(".jar!") + ".jar".length());
            }
            if ((file = new java.io.File(path)).exists()) {
                return file;
            }

        } catch (UnsupportedEncodingException ignored) {
            log.debug(ignored.getMessage(), ignored);
        }

        try {
            path = url.toExternalForm();
            if (path.startsWith("jar:")) {
                path = path.substring("jar:".length());
            }
            if (path.startsWith("wsjar:")) {
                path = path.substring("wsjar:".length());
            }
            if (path.startsWith("file:")) {
                path = path.substring("file:".length());
            }
            if (path.contains(".jar!")) {
                path = path.substring(0, path.indexOf(".jar!") + ".jar".length());
            }
            if (path.contains(".war!")) {
                path = path.substring(0, path.indexOf(".war!") + ".war".length());
            }
            if ((file = new java.io.File(path)).exists()) {
                return file;
            }

            path = path.replace("%20", " ");
            if ((file = new java.io.File(path)).exists()) {
                return file;
            }

        } catch (Exception ignored) {
            log.debug(ignored.getMessage(), ignored);

        }

        return null;
    }

    private static final Pattern jarPattern = Pattern.compile(".*\\.jar(\\!.*|$)");

    /**
     * 正则匹配是否为Jar
     *
     * @param url
     * @return 是否为 jar
     */
    private static boolean hasJarFileInPath(URL url) {
        return jarPattern.matcher(url.toExternalForm()).matches();
    }

    /**
     * default url types used by {@link Vfs#fromURL(java.net.URL)}
     * <p>
     * <p>jarFile - creates a {@link ZipDir} over jar file
     * <p>jarUrl - creates a {@link ZipDir} over a jar url (contains ".jar!/" in it's name), using Java's {@link JarURLConnection}
     * <p>directory - creates a {@link SystemDir} over a file system directory
     * <p>jboss vfs - for protocols vfs, using jboss vfs (should be provided in classpath)
     * <p>jboss vfsfile - creates a {@link UrlTypeVfs} for protocols vfszip and vfsfile.
     * <p>bundle - for bundle protocol, using eclipse FileLocator (should be provided in classpath)
     * <p>jarInputStream - creates a {@link JarInputDir} over jar files, using Java's JarInputStream
     */
    public enum DefaultUrlTypes implements UrlType {
        /**
         * Jar文件
         */
        jarFile {
            @Override
            public boolean matches(URL url) {
                return url.getProtocol().equals(FILE) && hasJarFileInPath(url);
            }

            @Override
            public Dir createDir(final URL url) throws Exception {
                return new ZipDir(new JarFile(getFile(url)));
            }
        },
        /**
         * 远程加载
         */
        jarUrl {
            @Override
            public boolean matches(URL url) {
                return JAR.equals(url.getProtocol()) || ZIP.equals(url.getProtocol()) || WS_JAR.equals(url.getProtocol());
            }

            @Override
            public Dir createDir(URL url) throws Exception {
                try {
                    URLConnection urlConnection = url.openConnection();
                    if (urlConnection instanceof JarURLConnection) {
                        urlConnection.setUseCaches(false);
                        return new ZipDir(((JarURLConnection) urlConnection).getJarFile());
                    }
                } catch (Throwable e) { /*fallback*/ }
                java.io.File file = getFile(url);
                if (file != null) {
                    return new ZipDir(new JarFile(file));
                }
                return null;
            }
        },

        directory {
            @Override
            public boolean matches(URL url) {
                if (url.getProtocol().equals(FILE) && !hasJarFileInPath(url)) {
                    java.io.File file = getFile(url);
                    return file != null && file.isDirectory();
                } else {
                    return false;
                }
            }

            @Override
            public Dir createDir(final URL url) throws Exception {
                return new SystemDir(getFile(url));
            }
        },
        jboss_vfs {
            @Override
            public boolean matches(URL url) {
                return url.getProtocol().equals(VSF);
            }

            @Override
            public Vfs.Dir createDir(URL url) throws Exception {
                Object content = url.openConnection().getContent();
                Class<?> virtualFile = ClasspathHelper.contextClassLoader().loadClass("org.jboss.vfs.VirtualFile");
                java.io.File physicalFile = (java.io.File) virtualFile.getMethod("getPhysicalFile").invoke(content);
                String name = (String) virtualFile.getMethod("getName").invoke(content);
                java.io.File file = new java.io.File(physicalFile.getParentFile(), name);
                if (!file.exists() || !file.canRead()) {
                    file = physicalFile;
                }
                return file.isDirectory() ? new SystemDir(file) : new ZipDir(new JarFile(file));
            }
        },

        jboss_vfsfile {
            @Override
            public boolean matches(URL url) throws Exception {
                return VSF_FILE.equals(url.getProtocol()) || VSF_ZIP.equals(url.getProtocol());
            }

            @Override
            public Dir createDir(URL url) throws Exception {
                return new UrlTypeVfs().createDir(url);
            }
        },

        bundle {
            @Override
            public boolean matches(URL url) throws Exception {
                return url.getProtocol().startsWith(BUNDLE);
            }

            @Override
            public Dir createDir(URL url) throws Exception {
                return fromURL((URL) ClasspathHelper.contextClassLoader().
                        loadClass(FILE_LOCATOR_CLASS)
                        .getMethod("resolve", URL.class)
                        .invoke(null, url));
            }
        },

        jarInputStream {
            @Override
            public boolean matches(URL url) throws Exception {
                return url.toExternalForm().contains(JAR_FILE);
            }

            @Override
            public Dir createDir(final URL url) throws Exception {
                return new JarInputDir(url);
            }
        }
    }
}
