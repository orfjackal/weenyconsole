package net.orfjackal.weenyconsole;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author Esko Luontola
 * @since 1.8.2007
 */
class MethodCall {

    private String methodName;
    private String[] parameters;

    public MethodCall(String methodName, String[] srcParameters, int srcPos, int srcLen) {
        this.methodName = methodName;
        parameters = new String[srcLen];
        System.arraycopy(srcParameters, srcPos, parameters, 0, parameters.length);
    }

    public boolean matches(Method method) {
        return methodHasTheRightName(method)
                && parametersCanBeAssignedTo(method);
    }

    public void invoke(Method method, Object methodOwner) throws IllegalAccessException, InvocationTargetException {
        method.invoke(methodOwner, parametersForMethod(method, parameters));
    }

    private boolean methodHasTheRightName(Method method) {
        return method.getName().equals(methodName);
    }

    private boolean parametersCanBeAssignedTo(Method method) {
        return parametersForMethod(method, parameters) != null;
    }

    private static Object[] parametersForMethod(Method method, String[] words) {
        try {
            Class<?>[] types = method.getParameterTypes();
            Object[] parameters = new Object[words.length];
            for (int i = 0; i < types.length; i++) {
                parameters[i] = convertToType(words[i], types[i]);
            }
            return parameters;

        } catch (ConversionFailedException e) {
            return null;
        }
    }

    private static Object convertToType(String sourceValue, Class<?> targetType) throws ConversionFailedException {
        if (sourceValue == null) {
            if (targetType.isPrimitive()) {
                throw new ConversionFailedException(sourceValue, targetType);
            }
            return null;
        }
        if (targetType.isPrimitive()) {
            targetType = primitiveToWrapperType(targetType.getName());
        }
        if (targetType.equals(Boolean.class)
                && !sourceValue.equals(Boolean.toString(true))
                && !sourceValue.equals(Boolean.toString(false))) {
            throw new ConversionFailedException(sourceValue, targetType);
        }
        if (targetType.equals(Character.class) && sourceValue.length() == 1) {
            return sourceValue.charAt(0);
        }
        try {
            Constructor<?> constructor = targetType.getConstructor(String.class);
            return constructor.newInstance(sourceValue);
        } catch (Exception e) {
            throw new ConversionFailedException(sourceValue, targetType, e);
        }
    }

    private static Class<?> primitiveToWrapperType(String name) {
        Class<?> wrapper;
        if (name.equals("boolean")) {
            wrapper = Boolean.class;
        } else if (name.equals("byte")) {
            wrapper = Byte.class;
        } else if (name.equals("char")) {
            wrapper = Character.class;
        } else if (name.equals("short")) {
            wrapper = Short.class;
        } else if (name.equals("int")) {
            wrapper = Integer.class;
        } else if (name.equals("long")) {
            wrapper = Long.class;
        } else if (name.equals("float")) {
            wrapper = Float.class;
        } else if (name.equals("double")) {
            wrapper = Double.class;
        } else {
            throw new IllegalArgumentException(name);
        }
        return wrapper;
    }
}
