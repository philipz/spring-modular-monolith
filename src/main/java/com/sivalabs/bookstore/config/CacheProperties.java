package com.sivalabs.bookstore.config;

import jakarta.validation.constraints.Min;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

/**
 * Configuration properties for Hazelcast cache settings.
 *
 * This class provides externalized configuration for cache behavior,
 * allowing different settings for different environments.
 */
@ConfigurationProperties(prefix = "bookstore.cache")
@Validated
public class CacheProperties {

    /**
     * Whether cache is enabled. Default is true.
     */
    private boolean enabled = true;

    /**
     * Maximum number of entries in the cache. Default is 1000.
     */
    @Min(1) private int maxSize = 1000;

    /**
     * Time-to-live for cache entries in seconds. Default is 3600 (1 hour).
     */
    @Min(0) private int timeToLiveSeconds = 3600;

    /**
     * Time-to-live for inventory cache entries in seconds. Default is 1800 (30 minutes).
     */
    @Min(0) private int inventoryTimeToLiveSeconds = 1800;

    /**
     * Whether write-through mode is enabled. Default is true.
     */
    private boolean writeThrough = true;

    /**
     * Write batch size for batch operations. Default is 1.
     */
    @Min(1) private int writeBatchSize = 1;

    /**
     * Write delay in seconds for write-behind mode. Default is 0 (write-through).
     */
    @Min(0) private int writeDelaySeconds = 0;

    /**
     * Whether cache metrics are enabled. Default is true.
     */
    private boolean metricsEnabled = true;

    /**
     * Whether read backup data is enabled. Default is true.
     */
    private boolean readBackupData = true;

    /**
     * When true, health check basic operations run in read-only mode (no put/remove),
     * using only get operations to avoid triggering MapStore writes. Default is true.
     */
    private boolean basicOperationsReadOnly = true;

    /**
     * Enable/disable health check basic operations block. Default is true.
     */
    private boolean testBasicOperationsEnabled = true;

    /**
     * Maximum idle time for cache entries in seconds. Default is 0 (disabled).
     */
    @Min(0) private int maxIdleSeconds = 0;

    /**
     * Number of backup replicas for cache entries. Default is 1.
     */
    @Min(0) private int backupCount = 1;

    /**
     * Circuit breaker failure threshold before opening the circuit. Default is 5 failures.
     */
    @Min(1) private int circuitBreakerFailureThreshold = 5;

    /**
     * Circuit breaker recovery timeout in milliseconds. Default is 30000 ms (30 seconds).
     */
    @Min(0) private long circuitBreakerRecoveryTimeoutMs = 30_000L;

    public CacheProperties() {}

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public int getMaxSize() {
        return maxSize;
    }

    public void setMaxSize(int maxSize) {
        this.maxSize = maxSize;
    }

    public int getTimeToLiveSeconds() {
        return timeToLiveSeconds;
    }

    public void setTimeToLiveSeconds(int timeToLiveSeconds) {
        this.timeToLiveSeconds = timeToLiveSeconds;
    }

    public int getInventoryTimeToLiveSeconds() {
        return inventoryTimeToLiveSeconds;
    }

    public void setInventoryTimeToLiveSeconds(int inventoryTimeToLiveSeconds) {
        this.inventoryTimeToLiveSeconds = inventoryTimeToLiveSeconds;
    }

    public boolean isWriteThrough() {
        return writeThrough;
    }

    public void setWriteThrough(boolean writeThrough) {
        this.writeThrough = writeThrough;
    }

    public int getWriteBatchSize() {
        return writeBatchSize;
    }

    public void setWriteBatchSize(int writeBatchSize) {
        this.writeBatchSize = writeBatchSize;
    }

    public int getWriteDelaySeconds() {
        return writeDelaySeconds;
    }

    public void setWriteDelaySeconds(int writeDelaySeconds) {
        this.writeDelaySeconds = writeDelaySeconds;
    }

    public boolean isMetricsEnabled() {
        return metricsEnabled;
    }

    public void setMetricsEnabled(boolean metricsEnabled) {
        this.metricsEnabled = metricsEnabled;
    }

    public boolean isReadBackupData() {
        return readBackupData;
    }

    public void setReadBackupData(boolean readBackupData) {
        this.readBackupData = readBackupData;
    }

    public boolean isBasicOperationsReadOnly() {
        return basicOperationsReadOnly;
    }

    public void setBasicOperationsReadOnly(boolean basicOperationsReadOnly) {
        this.basicOperationsReadOnly = basicOperationsReadOnly;
    }

    public boolean isTestBasicOperationsEnabled() {
        return testBasicOperationsEnabled;
    }

    public void setTestBasicOperationsEnabled(boolean testBasicOperationsEnabled) {
        this.testBasicOperationsEnabled = testBasicOperationsEnabled;
    }

    public int getMaxIdleSeconds() {
        return maxIdleSeconds;
    }

    public void setMaxIdleSeconds(int maxIdleSeconds) {
        this.maxIdleSeconds = maxIdleSeconds;
    }

    public int getBackupCount() {
        return backupCount;
    }

    public void setBackupCount(int backupCount) {
        this.backupCount = backupCount;
    }

    public int getCircuitBreakerFailureThreshold() {
        return circuitBreakerFailureThreshold;
    }

    public void setCircuitBreakerFailureThreshold(int circuitBreakerFailureThreshold) {
        this.circuitBreakerFailureThreshold = circuitBreakerFailureThreshold;
    }

    public long getCircuitBreakerRecoveryTimeoutMs() {
        return circuitBreakerRecoveryTimeoutMs;
    }

    public void setCircuitBreakerRecoveryTimeoutMs(long circuitBreakerRecoveryTimeoutMs) {
        this.circuitBreakerRecoveryTimeoutMs = circuitBreakerRecoveryTimeoutMs;
    }

    @Override
    public String toString() {
        return "CacheProperties{" + "enabled="
                + enabled + ", maxSize="
                + maxSize + ", timeToLiveSeconds="
                + timeToLiveSeconds + ", inventoryTimeToLiveSeconds="
                + inventoryTimeToLiveSeconds + ", writeThrough="
                + writeThrough + ", writeBatchSize="
                + writeBatchSize + ", writeDelaySeconds="
                + writeDelaySeconds + ", metricsEnabled="
                + metricsEnabled + ", readBackupData="
                + readBackupData + ", basicOperationsReadOnly="
                + basicOperationsReadOnly + ", testBasicOperationsEnabled="
                + testBasicOperationsEnabled + ", maxIdleSeconds="
                + maxIdleSeconds + ", backupCount="
                + backupCount + ", circuitBreakerFailureThreshold="
                + circuitBreakerFailureThreshold + ", circuitBreakerRecoveryTimeoutMs="
                + circuitBreakerRecoveryTimeoutMs + '}';
    }
}
