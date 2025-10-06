package com.sivalabs.bookstore.testsupport;

import java.util.Collections;
import java.util.Iterator;
import java.util.function.Supplier;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Simple {@link ObjectProvider} implementation for unit tests that wraps a {@link Supplier}.
 *
 * @param <T> bean type to supply
 */
public class TestObjectProvider<T> implements ObjectProvider<T> {

    private final Supplier<T> supplier;

    public TestObjectProvider(Supplier<T> supplier) {
        this.supplier = supplier;
    }

    @Override
    public T getObject() {
        return supplier.get();
    }

    @Override
    public T getObject(Object... args) {
        return supplier.get();
    }

    @Override
    public T getIfAvailable() {
        return supplier.get();
    }

    @Override
    public T getIfUnique() {
        return supplier.get();
    }

    @Override
    public Iterator<T> iterator() {
        T value = supplier.get();
        return value == null
                ? Collections.emptyIterator()
                : Collections.singletonList(value).iterator();
    }
}
