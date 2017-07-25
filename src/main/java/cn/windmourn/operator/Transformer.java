package cn.windmourn.operator;

import net.minecraft.launchwrapper.IClassTransformer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class Transformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        InputStream is = getClass().getResourceAsStream("/" + transformedName.replace('.', '_') + ".patch");
        if (is != null) {
            dbg("Operator: Replacing " + name + " -> " + transformedName);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] temp = new byte[4096];
            int len;
            try {
                while ((len = is.read(temp)) != -1) {
                    baos.write(temp, 0, len);
                }
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return baos.toByteArray();
        }
        return basicClass;
    }

    private static void dbg(String str) {
        System.out.println(str);
    }

}
