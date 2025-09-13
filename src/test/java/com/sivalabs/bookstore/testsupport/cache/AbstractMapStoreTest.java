package com.sivalabs.bookstore.testsupport.cache;

import static org.assertj.core.api.Assertions.assertThat;

import com.hazelcast.core.HazelcastInstance;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Abstract base class for MapStore unit tests to reduce code duplication.
 *
 * @param <K> the key type
 * @param <V> the value type
 * @param <M> the MapStore type
 * @param <R> the Repository type
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("Abstract MapStore Unit Tests")
public abstract class AbstractMapStoreTest<K, V, M, R> {

    @Mock
    protected R repository;

    @Mock
    protected HazelcastInstance hazelcastInstance;

    protected M mapStore;
    protected V testEntity1;
    protected V testEntity2;

    @BeforeEach
    protected void setUp() {
        // Subclasses should override this method to provide proper initialization
        if (mapStore == null) {
            mapStore = createMapStore(repository);
            testEntity1 = createTestEntity1();
            testEntity2 = createTestEntity2();
        }
    }

    /**
     * Create the MapStore instance to test.
     *
     * @param repository the mocked repository
     * @return MapStore instance
     */
    protected abstract M createMapStore(R repository);

    /**
     * Create the first test entity.
     *
     * @return test entity
     */
    protected abstract V createTestEntity1();

    /**
     * Create the second test entity.
     *
     * @return test entity
     */
    protected abstract V createTestEntity2();

    /**
     * Get the key for the first test entity.
     *
     * @return key
     */
    protected abstract K getTestEntity1Key();

    /**
     * Get the key for the second test entity.
     *
     * @return key
     */
    protected abstract K getTestEntity2Key();

    /**
     * Get a non-existent key for testing.
     *
     * @return non-existent key
     */
    protected abstract K getNonExistentKey();

    /**
     * Setup repository mock to return the first test entity.
     *
     * @param key the key to mock
     * @param entity the entity to return
     */
    protected abstract void mockRepositoryFindByKey(K key, V entity);

    /**
     * Setup repository mock to return empty.
     *
     * @param key the key to mock
     */
    protected abstract void mockRepositoryFindByKeyEmpty(K key);

    /**
     * Setup repository mock to throw exception.
     *
     * @param key the key to mock
     * @param exception the exception to throw
     */
    protected abstract void mockRepositoryFindByKeyException(K key, RuntimeException exception);

    /**
     * Setup repository mock for findAll operation.
     *
     * @param entities the entities to return
     */
    @SuppressWarnings("unchecked")
    protected abstract void mockRepositoryFindAll(V... entities);

    /**
     * Setup repository mock for findAll exception.
     *
     * @param exception the exception to throw
     */
    protected abstract void mockRepositoryFindAllException(RuntimeException exception);

    /**
     * Verify repository findByKey was called.
     *
     * @param key the expected key
     */
    protected abstract void verifyRepositoryFindByKey(K key);

    /**
     * Verify repository findAll was called.
     */
    protected abstract void verifyRepositoryFindAll();

    /**
     * Get the load method from MapStore.
     *
     * @param key the key to load
     * @return loaded entity or null
     */
    protected abstract V loadFromMapStore(K key);

    /**
     * Get the loadAll method from MapStore.
     *
     * @param keys the keys to load
     * @return map of loaded entities
     */
    protected abstract Map<K, V> loadAllFromMapStore(Collection<K> keys);

    /**
     * Get the loadAllKeys method from MapStore.
     *
     * @return all keys
     */
    protected abstract Iterable<K> loadAllKeysFromMapStore();

    /**
     * Call the store method on MapStore.
     *
     * @param key the key
     * @param entity the entity
     */
    protected abstract void storeInMapStore(K key, V entity);

    /**
     * Call the storeAll method on MapStore.
     *
     * @param entities the entities to store
     */
    protected abstract void storeAllInMapStore(Map<K, V> entities);

    /**
     * Call the delete method on MapStore.
     *
     * @param key the key
     */
    protected abstract void deleteFromMapStore(K key);

    /**
     * Call the deleteAll method on MapStore.
     *
     * @param keys the keys
     */
    protected abstract void deleteAllFromMapStore(Collection<K> keys);

    /**
     * Initialize the MapStore lifecycle.
     */
    protected abstract void initMapStore();

    /**
     * Destroy the MapStore lifecycle.
     */
    protected abstract void destroyMapStore();

    /**
     * Assert that two entities are equal.
     *
     * @param expected expected entity
     * @param actual actual entity
     */
    protected abstract void assertEntitiesEqual(V expected, V actual);

    @Test
    @DisplayName("Should initialize successfully with repository")
    void shouldInitializeWithRepository() {
        assertThat(mapStore).isNotNull();
    }

    @Test
    @DisplayName("Should complete lifecycle init without errors")
    void shouldInitializeLifecycle() {
        // Should not throw any exception
        initMapStore();
    }

    @Test
    @DisplayName("Should complete lifecycle destroy without errors")
    void shouldDestroyLifecycle() {
        // Should not throw any exception
        destroyMapStore();
    }

    @Test
    @DisplayName("Should store entity successfully - validation only")
    void shouldStoreEntitySuccessfully() {
        K key = getTestEntity1Key();

        // Store operation should complete without exception
        storeInMapStore(key, testEntity1);
    }

    @Test
    @DisplayName("Should handle store with null entity")
    void shouldHandleStoreWithNullEntity() {
        K key = getTestEntity1Key();

        // Should complete without throwing exception
        storeInMapStore(key, null);
    }

    @Test
    @DisplayName("Should load entity successfully from repository")
    void shouldLoadEntitySuccessfully() {
        K key = getTestEntity1Key();
        mockRepositoryFindByKey(key, testEntity1);

        V result = loadFromMapStore(key);

        assertThat(result).isNotNull();
        assertEntitiesEqual(testEntity1, result);
        verifyRepositoryFindByKey(key);
    }

    @Test
    @DisplayName("Should return null when entity not found")
    void shouldReturnNullWhenEntityNotFound() {
        K key = getNonExistentKey();
        mockRepositoryFindByKeyEmpty(key);

        V result = loadFromMapStore(key);

        assertThat(result).isNull();
        verifyRepositoryFindByKey(key);
    }

    @Test
    @DisplayName("Should handle load exception gracefully")
    void shouldHandleLoadException() {
        K key = getTestEntity1Key();
        RuntimeException testException = new RuntimeException("Database error");
        mockRepositoryFindByKeyException(key, testException);

        // Should not throw exception and return null
        V result = loadFromMapStore(key);

        assertThat(result).isNull();
        verifyRepositoryFindByKey(key);
    }

    @Test
    @DisplayName("Should load multiple entities successfully")
    void shouldLoadAllEntitiesSuccessfully() {
        K key1 = getTestEntity1Key();
        K key2 = getTestEntity2Key();
        K key3 = getNonExistentKey();
        Collection<K> keys = Arrays.asList(key1, key2, key3);

        mockRepositoryFindByKey(key1, testEntity1);
        mockRepositoryFindByKey(key2, testEntity2);
        mockRepositoryFindByKeyEmpty(key3);

        Map<K, V> result = loadAllFromMapStore(keys);

        assertThat(result).hasSize(2);
        assertThat(result).containsKey(key1);
        assertThat(result).containsKey(key2);
        assertThat(result).doesNotContainKey(key3);
        assertEntitiesEqual(testEntity1, result.get(key1));
        assertEntitiesEqual(testEntity2, result.get(key2));

        verifyRepositoryFindByKey(key1);
        verifyRepositoryFindByKey(key2);
        verifyRepositoryFindByKey(key3);
    }

    @Test
    @DisplayName("Should handle loadAll with empty collection")
    void shouldHandleLoadAllWithEmptyCollection() {
        Collection<K> emptyKeys = Arrays.asList();

        Map<K, V> result = loadAllFromMapStore(emptyKeys);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle loadAll exception gracefully")
    void shouldHandleLoadAllException() {
        K key = getTestEntity1Key();
        Collection<K> keys = Arrays.asList(key);
        RuntimeException testException = new RuntimeException("Database error");
        mockRepositoryFindByKeyException(key, testException);

        // Should return empty map instead of throwing
        Map<K, V> result = loadAllFromMapStore(keys);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Should handle loadAll with mixed success and failure results")
    void shouldHandleLoadAllWithMixedResults() {
        K key1 = getTestEntity1Key();
        K key2 = getTestEntity2Key();
        K errorKey = getNonExistentKey();
        Collection<K> keys = Arrays.asList(key1, errorKey, key2);

        mockRepositoryFindByKey(key1, testEntity1);
        mockRepositoryFindByKeyException(errorKey, new RuntimeException("Specific error"));
        mockRepositoryFindByKey(key2, testEntity2);

        Map<K, V> result = loadAllFromMapStore(keys);

        // Should continue processing other entities even if one fails
        assertThat(result).hasSize(2);
        assertThat(result).containsKey(key1);
        assertThat(result).containsKey(key2);
        assertThat(result).doesNotContainKey(errorKey);
    }

    @Test
    @DisplayName("Should handle delete operation gracefully")
    void shouldHandleDeleteOperation() {
        K key = getTestEntity1Key();

        // Should complete without throwing exception
        deleteFromMapStore(key);
    }

    @Test
    @DisplayName("Should handle deleteAll operation gracefully")
    void shouldHandleDeleteAllOperation() {
        Collection<K> keys = Arrays.asList(getTestEntity1Key(), getTestEntity2Key());

        // Should complete without throwing exception
        deleteAllFromMapStore(keys);
    }

    @Test
    @DisplayName("Should handle storeAll operation gracefully")
    void shouldHandleStoreAllOperation() {
        Map<K, V> entities = Map.of(
                getTestEntity1Key(), testEntity1,
                getTestEntity2Key(), testEntity2);

        // Should complete without throwing exception
        storeAllInMapStore(entities);
    }
}
