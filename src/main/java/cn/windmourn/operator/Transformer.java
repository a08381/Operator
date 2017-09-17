package cn.windmourn.operator;

import net.minecraft.launchwrapper.IClassTransformer;
import org.objectweb.asm.*;

public class Transformer implements IClassTransformer {

    @Override
    public byte[] transform(String name, String transformedName, byte[] basicClass) {
        if (name.equals("net.minecraftforge.common.ReflectionAPI")) {
            return transform001(basicClass);
        } else {
            return basicClass;
        }
    }

    private byte[] transform001(byte[] bytes) {
        ClassReader classReader = new ClassReader(bytes);
        ClassWriter classWriter = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        classReader.accept(new a(classWriter), ClassReader.EXPAND_FRAMES);
        return classWriter.toByteArray();
    }

    private class a extends ClassVisitor {

        public a(ClassVisitor cv) {
            super(Opcodes.ASM5, cv);
        }

        @Override
        public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
            if (name.equals("checkPermission")) {
                MethodVisitor mv = super.visitMethod(access, name, desc, signature, exceptions);
                mv.visitCode();
                mv.visitInsn(Opcodes.ICONST_1);
                mv.visitInsn(Opcodes.IRETURN);
                mv.visitMaxs(1, 0);
                mv.visitEnd();
                return new b();
            }
            return super.visitMethod(access, name, desc, signature, exceptions);
        }
    }

    private class b extends MethodVisitor {

        public b() {
            super(Opcodes.ASM5, null);
        }

    }

}
