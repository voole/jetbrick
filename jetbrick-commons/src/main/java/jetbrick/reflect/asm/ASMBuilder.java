/**
 * Copyright 2013-2014 Guoqiang Chen, Shanghai, China. All rights reserved.
 *
 * Email: subchen@gmail.com
 * URL: http://subchen.github.io/
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package jetbrick.reflect.asm;

import static jetbrick.asm.Opcodes.*;
import java.lang.reflect.Modifier;
import java.util.List;
import jetbrick.asm.*;
import jetbrick.reflect.*;

final class ASMBuilder {
    private static final String SUN_MAGIC_ACCESSOR_KLASS = "sun/reflect/MagicAccessorImpl";
    private static final String FIELD_ARGUMENTS_LENGTH = "argumentsLength";
    private static final String METHOD_CHECK_ARGUMENTS = "checkArguments";

    private final ClassWriter cw;
    private final String generatedKlassNameInternal;
    private final String delegateKlassNameInternal;

    public ASMBuilder(String generatedKlassName, String delegateKlassName, Class<?> interfaceKlass) {
        generatedKlassNameInternal = generatedKlassName.replace('.', '/');
        delegateKlassNameInternal = delegateKlassName.replace('.', '/');

        cw = new ClassWriter(ClassWriter.COMPUTE_MAXS);
        String[] interfaces = new String[] { interfaceKlass.getName().replace('.', '/') };
        cw.visit(V1_1, ACC_PUBLIC + ACC_SUPER + ACC_FINAL, generatedKlassNameInternal, null, SUN_MAGIC_ACCESSOR_KLASS, interfaces);
    }

    public void insertArgumentsLengthField(List<? extends Executable> executables) {
        FieldVisitor fv = cw.visitField(ACC_PRIVATE + ACC_FINAL + ACC_STATIC, FIELD_ARGUMENTS_LENGTH, "[I", null, null);
        fv.visitEnd();

        int size = executables.size();
        MethodVisitor mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
        mv.visitCode();
        mv.visitIntInsn(BIPUSH, size);
        mv.visitIntInsn(NEWARRAY, T_INT);
        for (int i = 0; i < size; i++) {
            mv.visitInsn(DUP);
            mv.visitIntInsn(BIPUSH, i);
            mv.visitIntInsn(BIPUSH, executables.get(i).getParameterCount());
            mv.visitInsn(IASTORE);
        }
        mv.visitFieldInsn(PUTSTATIC, generatedKlassNameInternal, FIELD_ARGUMENTS_LENGTH, "[I");
        mv.visitInsn(RETURN);
        mv.visitMaxs(size > 0 ? 4 : 1, 0);
        mv.visitEnd();
    }

    public void insertCheckArgumentsMethod() {
        MethodVisitor mv = cw.visitMethod(ACC_PRIVATE, METHOD_CHECK_ARGUMENTS, "(I[Ljava/lang/Object;)V", null, null);
        mv.visitCode();

        Label labelStep2 = new Label();
        Label labelError2 = new Label();
        Label labelStep3 = new Label();
        Label labelSucc = new Label();

        // step1: if (args == null)
        mv.visitVarInsn(ALOAD, 2);
        mv.visitJumpInsn(IFNONNULL, labelStep2);

        throwNullPointerException(mv, "args is NULL");

        // step2: if (which < 0 || which >= argumentsLength.length)
        mv.visitLabel(labelStep2);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitJumpInsn(IFLT, labelError2);

        mv.visitVarInsn(ILOAD, 1);
        mv.visitFieldInsn(GETSTATIC, generatedKlassNameInternal, FIELD_ARGUMENTS_LENGTH, "[I");
        mv.visitInsn(ARRAYLENGTH);
        mv.visitJumpInsn(IF_ICMPLT, labelStep3);

        mv.visitLabel(labelError2);
        throwIllegalArgumentException(mv, "invalid offset: ", 1);

        // step3: if (args.length != argumentsLength[which]) {
        mv.visitLabel(labelStep3);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitInsn(ARRAYLENGTH);
        mv.visitFieldInsn(GETSTATIC, generatedKlassNameInternal, FIELD_ARGUMENTS_LENGTH, "[I");
        mv.visitVarInsn(ILOAD, 1);
        mv.visitInsn(IALOAD);
        mv.visitJumpInsn(IF_ICMPEQ, labelSucc);

        throwIllegalArgumentException(mv, "argument length is not match : ", 1);

        //
        mv.visitLabel(labelSucc);
        mv.visitInsn(RETURN);

        mv.visitMaxs(4, 3);
        mv.visitEnd();
    }

    public void insertConstructor() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
        mv.visitCode();
        mv.visitVarInsn(ALOAD, 0);
        mv.visitMethodInsn(INVOKESPECIAL, SUN_MAGIC_ACCESSOR_KLASS, "<init>", "()V", false);
        mv.visitInsn(RETURN);
        mv.visitMaxs(1, 1);
        mv.visitEnd();
    }

    public void insertNewInstance() {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "newInstance", "()Ljava/lang/Object;", null, null);
        mv.visitCode();
        mv.visitTypeInsn(NEW, delegateKlassNameInternal);
        mv.visitInsn(DUP);
        mv.visitMethodInsn(INVOKESPECIAL, delegateKlassNameInternal, "<init>", "()V", false);
        mv.visitInsn(ARETURN);
        mv.visitMaxs(2, 1);
        mv.visitEnd();
    }

    public void insertNewInstance(List<ConstructorInfo> constructors) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_VARARGS, "newInstance", "(I[Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        mv.visitCode();

        // check arguments
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 1);
        mv.visitVarInsn(ALOAD, 2);
        mv.visitMethodInsn(INVOKESPECIAL, generatedKlassNameInternal, METHOD_CHECK_ARGUMENTS, "(I[Ljava/lang/Object;)V", false);

        int n = constructors.size();
        if (n != 0) {
            mv.visitVarInsn(ILOAD, 1);
            Label[] labels = new Label[n];
            for (int i = 0; i < n; i++) {
                labels[i] = new Label();
            }
            Label defaultLabel = new Label();
            mv.visitTableSwitchInsn(0, labels.length - 1, defaultLabel, labels);

            StringBuilder buffer = new StringBuilder(128);
            for (int i = 0; i < n; i++) {
                mv.visitLabel(labels[i]);
                mv.visitFrame(F_SAME, 0, null, 0, null);

                mv.visitTypeInsn(NEW, delegateKlassNameInternal);
                mv.visitInsn(DUP);

                buffer.setLength(0);
                buffer.append('(');
                Class<?>[] paramTypes = constructors.get(i).getParameterTypes();
                for (int paramIndex = 0; paramIndex < paramTypes.length; paramIndex++) {
                    mv.visitVarInsn(ALOAD, 2);
                    mv.visitIntInsn(BIPUSH, paramIndex);
                    mv.visitInsn(AALOAD);

                    Type type = Type.getType(paramTypes[paramIndex]);
                    insertUnbox(mv, type);
                    buffer.append(type.getDescriptor());
                }
                buffer.append(")V");
                mv.visitMethodInsn(INVOKESPECIAL, delegateKlassNameInternal, "<init>", buffer.toString(), false);
                mv.visitInsn(ARETURN);
            }
            mv.visitLabel(defaultLabel);
            mv.visitFrame(F_SAME, 0, null, 0, null);
        }
        throwIllegalArgumentException(mv, "cannot find constructor, index is ", 1);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public void insertInvoke(List<MethodInfo> methods) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC + ACC_VARARGS, "invoke", "(Ljava/lang/Object;I[Ljava/lang/Object;)Ljava/lang/Object;", null, null);
        mv.visitCode();

        // check arguments
        mv.visitVarInsn(ALOAD, 0);
        mv.visitVarInsn(ILOAD, 2);
        mv.visitVarInsn(ALOAD, 3);
        mv.visitMethodInsn(INVOKESPECIAL, generatedKlassNameInternal, METHOD_CHECK_ARGUMENTS, "(I[Ljava/lang/Object;)V", false);

        int n = methods.size();
        if (n != 0) {
            mv.visitVarInsn(ILOAD, 2);
            Label[] labels = new Label[n];
            for (int i = 0; i < n; i++) {
                labels[i] = new Label();
            }
            Label defaultLabel = new Label();
            mv.visitTableSwitchInsn(0, labels.length - 1, defaultLabel, labels);

            StringBuilder buffer = new StringBuilder(128);
            for (int i = 0; i < n; i++) {
                mv.visitLabel(labels[i]);
                mv.visitFrame(F_SAME, 0, null, 0, null);

                MethodInfo method = methods.get(i);
                boolean isInterface = method.getDeclaringKlass().isInterface();
                boolean isStatic = method.isStatic();

                if (!isStatic) {
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitTypeInsn(CHECKCAST, delegateKlassNameInternal);
                }

                buffer.setLength(0);
                buffer.append('(');

                String methodName = method.getName();
                Class<?>[] paramTypes = method.getParameterTypes();
                Class<?> returnType = method.getReturnType();
                for (int paramIndex = 0; paramIndex < paramTypes.length; paramIndex++) {
                    mv.visitVarInsn(ALOAD, 3);
                    mv.visitIntInsn(BIPUSH, paramIndex);
                    mv.visitInsn(AALOAD);
                    Type type = Type.getType(paramTypes[paramIndex]);
                    insertUnbox(mv, type);
                    buffer.append(type.getDescriptor());
                }
                buffer.append(')');
                buffer.append(Type.getDescriptor(returnType));
                int opcode = isInterface ? INVOKEINTERFACE : (isStatic ? INVOKESTATIC : INVOKEVIRTUAL);
                mv.visitMethodInsn(opcode, delegateKlassNameInternal, methodName, buffer.toString(), isInterface);

                insertBox(mv, Type.getType(returnType));
                mv.visitInsn(ARETURN);
            }
            mv.visitLabel(defaultLabel);
            mv.visitFrame(F_SAME, 0, null, 0, null);
        }
        throwIllegalArgumentException(mv, "cannot find method, index is ", 2);

        mv.visitMaxs(0, 0);
        mv.visitEnd();
    }

    public void insertGetObject(List<FieldInfo> fields) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "get", "(Ljava/lang/Object;I)Ljava/lang/Object;", null, null);
        mv.visitCode();

        int n = fields.size();
        if (n != 0) {
            mv.visitVarInsn(ILOAD, 2);
            Label[] labels = new Label[n];
            for (int i = 0; i < n; i++) {
                labels[i] = new Label();
            }
            Label defaultLabel = new Label();
            mv.visitTableSwitchInsn(0, labels.length - 1, defaultLabel, labels);

            for (int i = 0; i < n; i++) {
                mv.visitLabel(labels[i]);
                mv.visitFrame(F_SAME, 0, null, 0, null);

                FieldInfo field = fields.get(i);
                Type type = Type.getType(field.getType());
                if (Modifier.isStatic(field.getModifiers())) {
                    mv.visitFieldInsn(GETSTATIC, delegateKlassNameInternal, field.getName(), type.getDescriptor());
                } else {
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitTypeInsn(CHECKCAST, delegateKlassNameInternal);
                    mv.visitFieldInsn(GETFIELD, delegateKlassNameInternal, field.getName(), type.getDescriptor());
                }
                insertBox(mv, type);
                mv.visitInsn(ARETURN);
            }
            mv.visitLabel(defaultLabel);
            mv.visitFrame(F_SAME, 0, null, 0, null);
        }
        throwIllegalArgumentException(mv, "cannot find field, index is ", 2);

        int maxStack = fields.isEmpty() ? 5 : 6;
        mv.visitMaxs(maxStack, 3);
        mv.visitEnd();
    }

    public void insertSetObject(List<FieldInfo> fields) {
        MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "set", "(Ljava/lang/Object;ILjava/lang/Object;)V", null, null);
        mv.visitCode();

        int n = fields.size();
        if (n != 0) {
            mv.visitVarInsn(ILOAD, 2);
            Label[] labels = new Label[n];
            for (int i = 0; i < n; i++) {
                labels[i] = new Label();
            }
            Label defaultLabel = new Label();
            mv.visitTableSwitchInsn(0, labels.length - 1, defaultLabel, labels);

            for (int i = 0; i < n; i++) {
                mv.visitLabel(labels[i]);
                mv.visitFrame(F_SAME, 0, null, 0, null);

                FieldInfo field = fields.get(i);
                Type type = Type.getType(field.getType());
                boolean isStatic = Modifier.isStatic(field.getModifiers());
                if (!isStatic) {
                    mv.visitVarInsn(ALOAD, 1);
                    mv.visitTypeInsn(CHECKCAST, delegateKlassNameInternal);
                }
                mv.visitVarInsn(ALOAD, 3);
                insertUnbox(mv, type);
                mv.visitFieldInsn(isStatic ? PUTSTATIC : PUTFIELD, delegateKlassNameInternal, field.getName(), type.getDescriptor());
                mv.visitInsn(RETURN);
            }
            mv.visitLabel(defaultLabel);
            mv.visitFrame(F_SAME, 0, null, 0, null);
        }
        throwIllegalArgumentException(mv, "cannot find field, index is ", 2);

        int maxStack = fields.isEmpty() ? 5 : 6;
        mv.visitMaxs(maxStack, 4);
        mv.visitEnd();
    }

    private static void throwNullPointerException(MethodVisitor mv, String message) {
        mv.visitTypeInsn(NEW, "java/lang/NullPointerException");
        mv.visitInsn(DUP);
        mv.visitLdcInsn("args is null");
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/NullPointerException", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);
    }

    private static void throwIllegalArgumentException(MethodVisitor mv, String message, int indexPosition) {
        mv.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
        mv.visitInsn(DUP);
        mv.visitLdcInsn(message);
        mv.visitVarInsn(ILOAD, indexPosition); // index type must be "int"
        mv.visitMethodInsn(INVOKESTATIC, "java/lang/String", "valueOf", "(I)Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/String", "concat", "(Ljava/lang/String;)Ljava/lang/String;", false);
        mv.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "(Ljava/lang/String;)V", false);
        mv.visitInsn(ATHROW);
    }

    private static void insertBox(MethodVisitor mv, Type type) {
        switch (type.getSort()) {
        case Type.VOID:
            mv.visitInsn(ACONST_NULL);
            break;
        case Type.BOOLEAN:
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Boolean", "valueOf", "(Z)Ljava/lang/Boolean;", false);
            break;
        case Type.BYTE:
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Byte", "valueOf", "(B)Ljava/lang/Byte;", false);
            break;
        case Type.CHAR:
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Character", "valueOf", "(C)Ljava/lang/Character;", false);
            break;
        case Type.SHORT:
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Short", "valueOf", "(S)Ljava/lang/Short;", false);
            break;
        case Type.INT:
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Integer", "valueOf", "(I)Ljava/lang/Integer;", false);
            break;
        case Type.FLOAT:
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Float", "valueOf", "(F)Ljava/lang/Float;", false);
            break;
        case Type.LONG:
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Long", "valueOf", "(J)Ljava/lang/Long;", false);
            break;
        case Type.DOUBLE:
            mv.visitMethodInsn(INVOKESTATIC, "java/lang/Double", "valueOf", "(D)Ljava/lang/Double;", false);
            break;
        }
    }

    private static void insertUnbox(MethodVisitor mv, Type type) {
        switch (type.getSort()) {
        case Type.BOOLEAN:
            mv.visitTypeInsn(CHECKCAST, "java/lang/Boolean");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Boolean", "booleanValue", "()Z", false);
            break;
        case Type.BYTE:
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "byteValue", "()B", false);
            break;
        case Type.CHAR:
            mv.visitTypeInsn(CHECKCAST, "java/lang/Character");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Character", "charValue", "()C", false);
            break;
        case Type.SHORT:
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "shortValue", "()S", false);
            break;
        case Type.INT:
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "intValue", "()I", false);
            break;
        case Type.FLOAT:
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "floatValue", "()F", false);
            break;
        case Type.LONG:
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "longValue", "()J", false);
            break;
        case Type.DOUBLE:
            mv.visitTypeInsn(CHECKCAST, "java/lang/Number");
            mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Number", "doubleValue", "()D", false);
            break;
        case Type.ARRAY:
            mv.visitTypeInsn(CHECKCAST, type.getDescriptor());
            break;
        case Type.OBJECT:
            String internalName = type.getInternalName();
            if (!"java/lang/Object".equals(internalName)) {
                mv.visitTypeInsn(CHECKCAST, internalName);
            }
            break;
        }
    }

    public byte[] asByteCode() {
        cw.visitEnd();
        return cw.toByteArray();
    }
}