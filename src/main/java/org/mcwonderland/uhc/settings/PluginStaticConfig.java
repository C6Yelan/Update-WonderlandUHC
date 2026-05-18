package org.mcwonderland.uhc.settings;

import org.bukkit.configuration.file.YamlConfiguration;
import org.mcwonderland.uhc.WonderlandUHC;
import org.mcwonderland.uhc.platform.sound.PluginSound;
import org.mcwonderland.uhc.util.SoundConfigParser;

import java.io.File;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.util.ArrayList;
import java.util.List;

abstract class PluginStaticConfig {

    private static final Object UNSUPPORTED_VALUE = new Object();
    private static LoadContext loadContext;

    protected static void loadStaticConfiguration(Class<?> configClass, String fileName) {
        LoadContext previous = loadContext;
        File file = new File(WonderlandUHC.getInstance().getDataFolder(), fileName);
        loadContext = new LoadContext(YamlConfiguration.loadConfiguration(file));

        try {
            loadFields(configClass, configClass);
            invokeInit(configClass);
            checkFields(configClass);
        } finally {
            loadContext = previous;
        }
    }

    protected static void setPathPrefix(String pathPrefix) {
        context().pathPrefix = pathPrefix;
    }

    protected static String getString(String path) {
        String fullPath = fullPath(path);
        return context().configuration.getString(fullPath);
    }

    protected static List<String> getStringList(String path) {
        List<?> values = context().configuration.getList(fullPath(path), List.of());
        List<String> strings = new ArrayList<>();

        for (Object value : values)
            strings.add(String.valueOf(value));

        return strings;
    }

    protected static Integer getInteger(String path) {
        String fullPath = fullPath(path);
        return context().configuration.isSet(fullPath) ? context().configuration.getInt(fullPath) : null;
    }

    protected static Boolean getBoolean(String path) {
        String fullPath = fullPath(path);
        return context().configuration.isSet(fullPath) ? context().configuration.getBoolean(fullPath) : null;
    }

    protected static <T> T get(String path, Class<T> type) {
        Object value = context().configuration.get(fullPath(path));

        if (value == null)
            return null;

        if (type.isEnum())
            return enumValue(type, value.toString());

        if (type.isInstance(value))
            return type.cast(value);

        throw new IllegalArgumentException("Unsupported value type for " + type.getName());
    }

    private static void loadFields(Class<?> rootClass, Class<?> targetClass) {
        for (Field field : targetClass.getDeclaredFields())
            loadField(rootClass, field);

        for (Class<?> innerClass : targetClass.getDeclaredClasses())
            loadFields(rootClass, innerClass);
    }

    private static void loadField(Class<?> rootClass, Field field) {
        if (!isConfigField(field))
            return;

        try {
            field.setAccessible(true);

            if (!field.getType().isPrimitive())
                field.set(null, null);

            Object value = getFieldValue(rootClass, field);

            if (value != UNSUPPORTED_VALUE)
                field.set(null, value);
        } catch (ReflectiveOperationException ex) {
            throw new IllegalStateException("Failed to load config field " + field.getDeclaringClass().getName() + "#" + field.getName(), ex);
        }
    }

    private static Object getFieldValue(Class<?> rootClass, Field field) {
        String path = getPathInYaml(rootClass, field);
        Class<?> type = field.getType();

        if (!context().configuration.isSet(path))
            return null;

        if (type == String.class)
            return context().configuration.getString(path);

        if (type == Integer.class)
            return context().configuration.getInt(path);

        if (type == Boolean.class)
            return context().configuration.getBoolean(path);

        if (type == PluginSound.class)
            return SoundConfigParser.parse(context().configuration.getString(path));

        if (List.class.isAssignableFrom(type))
            return getListValue(field, path);

        if (type.isEnum())
            return enumValue(type, context().configuration.getString(path));

        return UNSUPPORTED_VALUE;
    }

    private static Object getListValue(Field field, String path) {
        if (!(field.getGenericType() instanceof ParameterizedType parameterizedType))
            return context().configuration.getList(path);

        Object listType = parameterizedType.getActualTypeArguments()[0];

        if (listType == String.class)
            return getStringListWithoutPrefix(path);

        if (listType == Integer.class)
            return context().configuration.getIntegerList(path);

        return context().configuration.getList(path);
    }

    private static List<String> getStringListWithoutPrefix(String path) {
        List<?> values = context().configuration.getList(path, List.of());
        List<String> strings = new ArrayList<>();

        for (Object value : values)
            strings.add(String.valueOf(value));

        return strings;
    }

    private static void invokeInit(Class<?> targetClass) {
        for (Method method : targetClass.getDeclaredMethods()) {
            if (!isInitMethod(method))
                continue;

            try {
                method.setAccessible(true);
                method.invoke(null);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("Failed to access init method in " + targetClass.getName(), ex);
            } catch (InvocationTargetException ex) {
                throw new IllegalStateException("Failed to initialize config class " + targetClass.getName(), ex.getCause());
            }
        }

        for (Class<?> innerClass : targetClass.getDeclaredClasses())
            invokeInit(innerClass);
    }

    private static void checkFields(Class<?> targetClass) {
        for (Field field : targetClass.getDeclaredFields()) {
            if (!isConfigField(field))
                continue;

            try {
                field.setAccessible(true);

                if (field.get(null) == null)
                    throw new IllegalStateException("Missing configuration value for " + field.getDeclaringClass().getSimpleName() + "." + field.getName());
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException("Failed to check config field " + field.getDeclaringClass().getName() + "#" + field.getName(), ex);
            }
        }

        for (Class<?> innerClass : targetClass.getDeclaredClasses())
            checkFields(innerClass);
    }

    private static boolean isConfigField(Field field) {
        int modifiers = field.getModifiers();

        return Modifier.isPublic(modifiers)
                && Modifier.isStatic(modifiers)
                && !Modifier.isFinal(modifiers)
                && !field.isSynthetic();
    }

    private static boolean isInitMethod(Method method) {
        int modifiers = method.getModifiers();

        return method.getName().equals("init")
                && Modifier.isStatic(modifiers)
                && method.getParameterCount() == 0;
    }

    private static String getPathInYaml(Class<?> rootClass, Field field) {
        StringBuilder builder = new StringBuilder(toYamlName(field.getName()));
        Class<?> fieldClass = field.getDeclaringClass();

        while (fieldClass != rootClass) {
            builder.insert(0, fieldClass.getSimpleName() + ".");
            fieldClass = fieldClass.getDeclaringClass();
        }

        return builder.toString();
    }

    private static String toYamlName(String fieldName) {
        String[] parts = fieldName.toLowerCase().split("_");
        StringBuilder builder = new StringBuilder();

        for (String part : parts) {
            if (builder.length() > 0)
                builder.append('_');

            if (part.isEmpty())
                continue;

            builder.append(Character.toUpperCase(part.charAt(0)));

            if (part.length() > 1)
                builder.append(part.substring(1));
        }

        return builder.toString();
    }

    private static String fullPath(String path) {
        String pathPrefix = context().pathPrefix;

        if (pathPrefix == null || pathPrefix.isEmpty())
            return path;

        return pathPrefix + "." + path;
    }

    @SuppressWarnings("unchecked")
    private static <T> T enumValue(Class<T> type, String value) {
        String enumName = value.replace('-', '_').replace(' ', '_');

        for (Object constant : type.getEnumConstants()) {
            Enum<?> enumConstant = (Enum<?>) constant;

            if (enumConstant.name().equalsIgnoreCase(enumName))
                return (T) constant;
        }

        throw new IllegalArgumentException("Unknown enum value '" + value + "' for " + type.getName());
    }

    private static LoadContext context() {
        if (loadContext == null)
            throw new IllegalStateException("Static configuration is not loading.");

        return loadContext;
    }

    private static final class LoadContext {
        private final YamlConfiguration configuration;
        private String pathPrefix;

        private LoadContext(YamlConfiguration configuration) {
            this.configuration = configuration;
        }
    }
}
