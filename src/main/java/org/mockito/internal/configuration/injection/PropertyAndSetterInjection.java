/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */

package org.mockito.internal.configuration.injection;

import static org.mockito.internal.exceptions.Reporter.cannotInitializeForInjectMocksAnnotation;
import static org.mockito.internal.exceptions.Reporter.fieldInitialisationThrewException;
import static org.mockito.internal.util.collections.Sets.newMockSafeHashSet;
import static org.mockito.internal.util.reflection.SuperTypesLastSorter.sortSuperTypesLast;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.mockito.exceptions.base.MockitoException;
import org.mockito.internal.configuration.injection.filter.MockCandidateFilter;
import org.mockito.internal.configuration.injection.filter.NameBasedCandidateFilter;
import org.mockito.internal.configuration.injection.filter.TerminalMockCandidateFilter;
import org.mockito.internal.configuration.injection.filter.TypeBasedCandidateFilter;
import org.mockito.internal.util.collections.ListUtil;
import org.mockito.internal.util.reflection.FieldInitializationReport;
import org.mockito.internal.util.reflection.FieldInitializer;

/**
 * Inject mocks using first setters then fields, if no setters available.
 *
 * <p>
 * <u>Algorithm :<br></u>
 * for each field annotated by @InjectMocks
 *   <ul>
 *   <li>initialize field annotated by @InjectMocks
 *   <li>for each fields of a class in @InjectMocks type hierarchy
 *     <ul>
 *     <li>make a copy of mock candidates
 *     <li>order fields from sub-type to super-type, then by field name
 *     <li>for the list of fields in a class try two passes of :
 *         <ul>
 *             <li>find mock candidate by type
 *             <li>if more than <b>*one*</b> candidate find mock candidate on name
 *             <li>if one mock candidate then
 *                 <ul>
 *                     <li>set mock by property setter if possible
 *                     <li>else set mock by field injection
 *                 </ul>
 *             <li>remove mock from mocks copy (mocks are just injected once in a class)
 *             <li>remove injected field from list of class fields
 *         </ul>
 *     <li>else don't fail, user will then provide dependencies
 *     </ul>
 *   </ul>
 * </p>
 *
 * <p>
 * <u>Note:</u> If the field needing injection is not initialized, the strategy tries
 * to create one using a no-arg constructor of the field type.
 * </p>
 */
public class PropertyAndSetterInjection extends MockInjectionStrategy {

    private final MockCandidateFilter mockCandidateFilter =
            new TypeBasedCandidateFilter(
                    new NameBasedCandidateFilter(
                            new TerminalMockCandidateFilter()));

    private final ListUtil.Filter<Field> notFinalOrStatic = new ListUtil.Filter<Field>() {
        public boolean isOut(Field object) {
            return Modifier.isFinal(object.getModifiers()) || Modifier.isStatic(object.getModifiers());
        }
    };


    public boolean processInjection(Field injectMocksField, Object injectMocksFieldOwner, Set<Object> mockCandidates) {
        // 直接无参构造注入属性
        FieldInitializationReport report = initializeInjectMocksField(injectMocksField, injectMocksFieldOwner);

        // for each field in the class hierarchy
        boolean injectionOccurred = false;
        Class<?> fieldClass = report.fieldClass();
        Object fieldInstanceNeedingInjection = report.fieldInstance();
        // 遍历属性父级注入
        while (fieldClass != Object.class) {
            injectionOccurred |= injectMockCandidates(fieldClass, fieldInstanceNeedingInjection, newMockSafeHashSet(mockCandidates));
            fieldClass = fieldClass.getSuperclass();
        }
        return injectionOccurred;
    }

    private FieldInitializationReport initializeInjectMocksField(Field field, Object fieldOwner) {
        try {
            return new FieldInitializer(fieldOwner, field).initialize();
        } catch (MockitoException e) {
            if(e.getCause() instanceof InvocationTargetException) {
                Throwable realCause = e.getCause().getCause();
                throw fieldInitialisationThrewException(field, realCause);
            }
            throw cannotInitializeForInjectMocksAnnotation(field.getName(),e.getMessage());
        }
    }


    /**
     * 属性注入
     *
     * @param awaitingInjectionClazz    注入的class
     * @param injectee                  被注入的属性的实体
     * @param mocks                     所有的mocks
     * @return
     */
    private boolean injectMockCandidates(Class<?> awaitingInjectionClazz, Object injectee, Set<Object> mocks) {
        boolean injectionOccurred;
        List<Field> orderedCandidateInjecteeFields = orderedInstanceFieldsFrom(awaitingInjectionClazz);
        // pass 1
        injectionOccurred = injectMockCandidatesOnFields(mocks, injectee, false, orderedCandidateInjecteeFields);
        // pass 2  TODO 为什么注入两次？
        injectionOccurred |= injectMockCandidatesOnFields(mocks, injectee, injectionOccurred, orderedCandidateInjecteeFields);
        return injectionOccurred;
    }

    private boolean injectMockCandidatesOnFields(Set<Object> mocks,
                                                 Object injectee,
                                                 boolean injectionOccurred,
                                                 List<Field> orderedCandidateInjecteeFields) {
        for (Iterator<Field> it = orderedCandidateInjecteeFields.iterator(); it.hasNext(); ) {
            Field candidateField = it.next();
            // 返回被注入的mock
            Object injected = mockCandidateFilter.filterCandidate(mocks, candidateField, orderedCandidateInjecteeFields, injectee)
                                                 .thenInject();
            if (injected != null) {
                injectionOccurred |= true;
                // 移除注入成功的mock
                mocks.remove(injected);
                // 移除注入的field
                it.remove();
            }
        }
        return injectionOccurred;
    }

    private List<Field> orderedInstanceFieldsFrom(Class<?> awaitingInjectionClazz) {
        List<Field> declaredFields = Arrays.asList(awaitingInjectionClazz.getDeclaredFields());
        declaredFields = ListUtil.filter(declaredFields, notFinalOrStatic);

        return sortSuperTypesLast(declaredFields);
    }
}
