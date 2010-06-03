package org.sadiframework.utils;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Utility class that performs the unzip operation
 * 
 * @author Eddie Kawas
 * 
 */
public class ZipUtilities {

    /**
     * 
     * @param filename
     *            the absolute file path of the file to unzip
     * @param baseDir
     *            the directory to unzip the contents into
     * @throws IOException
     */
    public static void unzip(String filename, String baseDir) throws IOException {
        if (filename == null)
            return;
        InputStream in = new BufferedInputStream(new FileInputStream(filename));
        ZipInputStream zin = new ZipInputStream(in);
        ZipEntry e;

        while ((e = zin.getNextEntry()) != null) {
            if (e.isDirectory()) {
                File f = new File(baseDir, e.getName());
                f.setWritable(true);
                if (!f.exists())
                    if (!f.mkdirs())
                        throw new IOException(String.format("Could not make directory: %s in %s", e
                                .getName(), baseDir));
            } else {
                unzip(zin, e.getName(), baseDir);
            }
        }
        zin.close();
    }

    /**
     * 
     * @param inStream
     *            the input stream to unzip
     * @param baseDir
     *            the directory to unzip into
     * @throws IOException
     */
    public static void unzip(InputStream inStream, String baseDir) throws IOException {
        if (inStream == null)
            return;
        ZipInputStream zin = new ZipInputStream(new BufferedInputStream(inStream));
        // ZipInputStream zin = new ZipInputStream(inStream);
        ZipEntry e;
        while ((e = zin.getNextEntry()) != null) {
            if (e.isDirectory()) {
                File f = new File(baseDir, e.getName());
                f.setWritable(true);
                if (!f.exists())
                    if (!f.mkdirs())
                        throw new IOException(String.format("Could not make directory: %s in %s", e
                                .getName(), baseDir));
            } else {
                unzip(zin, e.getName(), baseDir);
            }
        }
        zin.close();
    }

    private static void unzip(ZipInputStream zin, String s, String baseDir) throws IOException {
        // System.out.println(String.format("unzip: %s%s", baseDir, s));
        int BUFFER = 2048;
        File f = new File(baseDir, s);
        f.setWritable(true);
        // do not overwrite existing files
        if (f.exists()) {
            return;
        }
        FileOutputStream fos = new FileOutputStream(f);
        BufferedOutputStream dest = new BufferedOutputStream(fos, BUFFER);
        int count = -1;
        byte[] data = new byte[BUFFER];
        while ((count = zin.read(data, 0, BUFFER)) != -1) {
            dest.write(data, 0, count);
        }
        dest.flush();
        dest.close();
    }
}
