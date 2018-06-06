/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.configuration.injection.filter;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 基于类型的过滤
 */
public class TypeBasedCandidateFilter implements MockCandidateFilter {

    private final MockCandidateFilter next;

    public TypeBasedCandidateFilter(MockCandidateFilter next) {
        this.next = next;
    }

    public OngoingInjector filterCandidate(final Collection<Object> mocks,
                                           final Field candidateFieldToBeInjected,
                                           final List<Field> allRemainingCandidateFields,
                                           final Object injectee) {
        // 过滤所有field对应的类型的mocks
        List<Object> mockTypeMatches = new ArrayList<Object>();
        for (Object mock : mocks) {
            if (candidateFieldToBeInjected.getType().isAssignableFrom(mock.getClass())) {
                mockTypeMatches.add(mock);
            }
        }

        return next.filterCandidate(mockTypeMatches, candidateFieldToBeInjected, allRemainingCandidateFields, injectee);
    }
}
