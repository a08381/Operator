package cn.windmourn.operator;

import net.minecraft.launchwrapper.IClassTransformer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class MinecraftTransformer implements IClassTransformer {

    private Logger logger = LogManager.getLogger("operator");
    private ZipFile opZipFile = null;

    public MinecraftTransformer() {
        dbg("Operator ClassTransformer");
        try {
            URLClassLoader ucl = (URLClassLoader) getClass().getClassLoader();
            URL[] urls = ucl.getURLs();
            for (URL url : urls) {
                ZipFile zipFile = getOperatorZipFile(url);
                if (zipFile != null) {
                    this.opZipFile = zipFile;
                    dbg("Operator URL: " + url);
                    dbg("Operator ZIP file: " + zipFile);
                    break;
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (this.opZipFile == null) {
            dbg("*** Can not find the Operator JAR in the classpath ***");
            dbg("*** Operator will not be loaded! ***");
        }
    }

    private ZipFile getOperatorZipFile(URL url) {
        try {
            URI uri = url.toURI();
            File file = new File(uri);
            ZipFile zipFile = new ZipFile(file);
            if (zipFile.getEntry("cn/windmourn/operator/Transformer.class") == null) {
                zipFile.close();
                return null;
            }
            return zipFile;
        } catch (IOException | URISyntaxException ignored) {
        }
        return null;
    }

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        byte opBytes[] = getOperatorClass(name);
        if (opBytes != null) {
            dbg("Operator: Replacing " + name + " -> " + transformedName);
            return opBytes;
        }
        return basicClass;
    }

    private byte[] getOperatorClass(String name) {
        if (this.opZipFile == null) {
            return null;
        }
        String fullName = name.replace('.', '/') + ".class";
        ZipEntry ze = this.opZipFile.getEntry(fullName);
        if (ze == null) {
            return null;
        }
        try {
            InputStream in = this.opZipFile.getInputStream(ze);
            byte[] bytes = readAll(in);
            if (bytes.length != ze.getSize()) {
                dbg("Invalid size for " + fullName + ": " + bytes.length + ", should be: " + ze.getSize());
                return null;
            }
            return bytes;
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    private byte[] readAll(InputStream is)
            throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (; ; ) {
            int len = is.read(buf);
            if (len < 0) {
                break;
            }
            baos.write(buf, 0, len);
        }
        is.close();

        return baos.toByteArray();
    }

    private void dbg(String str) {
        logger.debug(str);
    }

}
