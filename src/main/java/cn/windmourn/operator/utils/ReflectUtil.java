package cn.windmourn.operator.utils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;

public class ReflectUtil {

    public static <T> T getOfT(Object obj, Class<T> type) {
        for (Field field : obj.getClass().getDeclaredFields()) {
            if (type.equals(field.getType())) {
                return get(obj, field, type);
            }
        }
        return null;
    }

    public static <T> T get(Object obj, String name, Class<T> type) {
        return get(obj, obj.getClass(), name, type);
    }

    public static <T> T get(Object obj, Class<?> clazz, String name, Class<T> type) {
        for (Field field : clazz.getDeclaredFields()) {
            if (name.equals(field.getName())) {
                return get(obj, field, type);
            }
        }
        throw new IllegalArgumentException("No field: " + name);
    }

    public static <T> T invoke(Object obj, String name, Class<T> type, Object... args) {
        return invoke(obj, obj.getClass(), name, type, args);
    }

    public static <T> T invoke(Object obj, Class<?> clazz, String name, Class<T> type, Object... args) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (name.equals(method.getName())) {
                return invoke(obj, method, type, args);
            }
        }
        throw new IllegalArgumentException("No method: " + name);
    }

    public static void invoke(Object obj, String name, Object... args) {
        invoke(obj, obj.getClass(), name, args);
    }

    public static void invoke(Object obj, Class<?> clazz, String name, Object... args) {
        for (Method method : clazz.getDeclaredMethods()) {
            if (name.equals(method.getName())) {
                invoke(obj, method, args);
            }
        }
        throw new IllegalArgumentException("No method: " + name);
    }

    public static void setStatic(String name, Class<?> clazz, Object val) {
        try {
            Field field = clazz.getDeclaredField(name);
            field.setAccessible(true);
            if (Modifier.isFinal(field.getModifiers())) {
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(field, field.getModifiers() & 0xFFFFFFEF);
            }
            field.set(null, val);
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
        }
    }

    public static <T> T get(Object obj, Field field, Class<T> type) {
        try {
            field.setAccessible(true);
            return type.cast(field.get(obj));
        } catch (ReflectiveOperationException ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public static <T> T invoke(Object obj, Method method, Class<T> type, Object... args) {
        try {
            method.setAccessible(true);
            return type.cast(method.invoke(obj, args));
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void invoke(Object obj, Method method, Object... args) {
        try {
            method.setAccessible(true);
            method.invoke(obj, args);
        } catch (ReflectiveOperationException e) {
            e.printStackTrace();
        }
    }

}
