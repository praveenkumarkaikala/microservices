package com.fundmatrix.navaccounting.common;

import com.fundmatrix.navaccounting.common.exception.BusinessException;
import com.fundmatrix.navaccounting.common.exception.ResourceNotFoundException;
import feign.FeignException;

import java.util.function.Supplier;

/**
 * Wraps outbound Feign calls so that remote failures surface through the same
 * {@link com.fundmatrix.navaccounting.common.exception.GlobalExceptionHandler} shape the
 * monolith used for local repository lookups: a 404 from the downstream service becomes a
 * {@link ResourceNotFoundException} (404 here too), any other Feign failure becomes a
 * {@link BusinessException} (422 here) instead of leaking a raw FeignException as a 500.
 */
public final class FeignSupport {

    private FeignSupport() {
    }

    public static <T> T call(Supplier<T> supplier, String entity, Object id) {
        try {
            return supplier.get();
        } catch (FeignException.NotFound ex) {
            throw ResourceNotFoundException.of(entity, id);
        } catch (FeignException ex) {
            throw new BusinessException(entity + " lookup failed (" + id + "): " + ex.getMessage());
        }
    }
}
