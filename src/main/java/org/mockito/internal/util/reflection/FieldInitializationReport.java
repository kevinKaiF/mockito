/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package org.mockito.internal.util.reflection;

/**
 * Report on field initialization
 */
public class FieldInitializationReport {
    // 属性实体
    private final Object fieldInstance;
    // 是否使用无参构造方法初始化
    private final boolean wasInitialized;
    // 是否是构造参数方式初始化
    private final boolean wasInitializedUsingConstructorArgs;

    public FieldInitializationReport(Object fieldInstance, boolean wasInitialized, boolean wasInitializedUsingConstructorArgs) {
        this.fieldInstance = fieldInstance;
        this.wasInitialized = wasInitialized;
        this.wasInitializedUsingConstructorArgs = wasInitializedUsingConstructorArgs;
    }

    /**
     * Returns the actual field instance.
     *
     * @return the actual instance
     */
    public Object fieldInstance() {
        return fieldInstance;
    }

    /**
     * Indicate whether the field was created during the process or not.
     *
     * @return <code>true</code> if created, <code>false</code> if the field did already hold an instance.
     */
    public boolean fieldWasInitialized() {
        return wasInitialized;
    }

    /**
     * Indicate whether the field was created using constructor args.
     *
     * @return <code>true</code> if field was created using constructor parameters.
     */
    public boolean fieldWasInitializedUsingContructorArgs() {
        return wasInitializedUsingConstructorArgs;
    }

    /**
     * Returns the class of the actual instance in the field.
     *
     * @return Class of the instance
     */
    public Class<?> fieldClass() {
        return fieldInstance != null ? fieldInstance.getClass() : null;
    }
}

