/*
 * Copyright (c) 2007 Mockito contributors
 * This program is made available under the terms of the MIT License.
 */
package org.mockito.internal.configuration.injection.filter;

import org.mockito.internal.util.MockUtil;

import static org.mockito.internal.util.MockUtil.getMockName;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * 基于mock名称的过滤
 * 注意：在生成mock名称之前，MockScanner的addPreparedMocks方法中{@link MockUtil#maybeRedefineMockName(Object, String)}
 * 重新注入新的mock名称，且为field的名称
 * @see org.mockito.internal.configuration.injection.scanner.MockScanner#addPreparedMocks(Set)
 */
public class NameBasedCandidateFilter implements MockCandidateFilter {
    private final MockCandidateFilter next;

    public NameBasedCandidateFilter(MockCandidateFilter next) {
        this.next = next;
    }

    public OngoingInjector filterCandidate(final Collection<Object> mocks,
                                           final Field candidateFieldToBeInjected,
                                           final List<Field> allRemainingCandidateFields,
                                           final Object injectee) {
        // 如果mock只有一个，且是另一个field，就不需要再注入了
        // 同一个类中有两个属性是同一个类型，这种情况就不需要再注入了
        if (mocks.size() == 1
                && anotherCandidateMatchesMockName(mocks, candidateFieldToBeInjected, allRemainingCandidateFields)) {
            return OngoingInjector.nop;
        }

        return next.filterCandidate(tooMany(mocks) ? selectMatchingName(mocks, candidateFieldToBeInjected) : mocks,
                                    candidateFieldToBeInjected,
                                    allRemainingCandidateFields,
                                    injectee);
    }

    private boolean tooMany(Collection<Object> mocks) {
        return mocks.size() > 1;
    }

    /**
     * 筛选field的名称和mock名称一致的mock对象
     *
     * @param mocks
     * @param candidateFieldToBeInjected
     * @return
     */
    private List<Object> selectMatchingName(Collection<Object> mocks, Field candidateFieldToBeInjected) {
        List<Object> mockNameMatches = new ArrayList<Object>();
        for (Object mock : mocks) {
            // 获取属性名称与mock equals的
            if (candidateFieldToBeInjected.getName().equals(getMockName(mock).toString())) {
                mockNameMatches.add(mock);
            }
        }
        return mockNameMatches;
    }

    /*
     * In this case we have to check whether we have conflicting naming
     * fields. E.g. 2 fields of the same type, but we have to make sure
     * we match on the correct name.
     *
     * Therefore we have to go through all other fields and make sure
     * whenever we find a field that does match its name with the mock
     * name, we should take that field instead.
     */
    private boolean anotherCandidateMatchesMockName(final Collection<Object> mocks,
                                                    final Field candidateFieldToBeInjected,
                                                    final List<Field> allRemainingCandidateFields) {
        String mockName = getMockName(mocks.iterator().next()).toString();

        for (Field otherCandidateField : allRemainingCandidateFields) {
            if (!otherCandidateField.equals(candidateFieldToBeInjected)
                    && otherCandidateField.getType().equals(candidateFieldToBeInjected.getType())
                    && otherCandidateField.getName().equals(mockName)) { // field的name equals mockName
                return true;
            }
        }
        return false;
    }
}
