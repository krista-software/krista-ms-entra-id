package app.krista.extensions.cyber_security.iam.microsoft_entra_id.catalog.util;

import app.krista.ksdk.telemetry.TelemetryMetrics;
import org.jvnet.hk2.annotations.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * Helper class for recording telemetry metrics for Microsoft Entra ID operations.
 * Provides centralized telemetry functionality for all Microsoft Entra ID-related operations.
 */
@Singleton
@Service
public class TelemetryHelper {
    private static final Logger logger = LoggerFactory.getLogger(TelemetryHelper.class);
    private final TelemetryMetrics telemetryMetrics;

    @Inject
    public TelemetryHelper(TelemetryMetrics telemetryMetrics) {
        this.telemetryMetrics = telemetryMetrics;
    }

    /**
     * Records the start of an Microsoft Entra ID operation.
     *
     * @param operationName the name of the operation being started
     */
    public void recordOperationStart(String operationName) {
        try {
            telemetryMetrics.incrementCounter("azure_ad.operation.started." + operationName, 1L);
            logger.debug("Telemetry: Started operation {}", operationName);
        } catch (Exception e) {
            logger.warn("Failed to record telemetry for operation start: {}", e.getMessage());
        }
    }

    /**
     * Records the successful completion of an Microsoft Entra ID operation.
     *
     * @param operationName the name of the operation that completed successfully
     * @param durationMs the duration of the operation in milliseconds
     */
    public void recordOperationSuccess(String operationName, long durationMs) {
        try {
            telemetryMetrics.incrementCounter("azure_ad.operation.completed." + operationName, 1L);
            telemetryMetrics.incrementCounter("azure_ad.operation.duration." + operationName, durationMs);
            logger.debug("Telemetry: Completed operation {} in {}ms", operationName, durationMs);
        } catch (Exception e) {
            logger.warn("Failed to record telemetry for operation success: {}", e.getMessage());
        }
    }

    /**
     * Records the failure of an Microsoft Entra ID operation.
     *
     * @param operationName the name of the operation that failed
     * @param durationMs the duration of the operation before failure in milliseconds
     * @param error the exception that caused the failure
     */
    public void recordOperationFailure(String operationName, long durationMs, Exception error) {
        try {
            telemetryMetrics.incrementCounter("azure_ad.operation.failed." + operationName, 1L);
            telemetryMetrics.incrementCounter("azure_ad.operation.duration." + operationName, durationMs);
            telemetryMetrics.incrementCounter("azure_ad.operation.error." + error.getClass().getSimpleName(), 1L);
            logger.debug("Telemetry: Failed operation {} after {}ms with error: {}", operationName, durationMs, error.getMessage());
        } catch (Exception e) {
            logger.warn("Failed to record telemetry for operation failure: {}", e.getMessage());
        }
    }

    /**
     * Records the count of accounts processed in an Microsoft Entra ID operation.
     *
     * @param operationName the name of the operation
     * @param count the number of accounts processed
     */
    public void recordAccountCount(String operationName, int count) {
        try {
            telemetryMetrics.incrementCounter("azure_ad.account.count." + operationName, (long) count);
            logger.debug("Telemetry: Recorded account count {} for operation {}", count, operationName);
        } catch (Exception e) {
            logger.warn("Failed to record telemetry for account count: {}", e.getMessage());
        }
    }

    /**
     * Starts a new telemetry operation tracking.
     *
     * @param operationName the name of the operation to track
     * @return TelemetryOperation instance for tracking this operation
     */
    public TelemetryOperation startOperation(String operationName) {
        return new TelemetryOperation(operationName, this);
    }

    /**
     * Inner class for tracking individual operations with automatic timing.
     */
    public static class TelemetryOperation {
        private final String operationName;
        private final long startTime;
        private final TelemetryHelper helper;

        public TelemetryOperation(String operationName, TelemetryHelper helper) {
            this.operationName = operationName;
            this.helper = helper;
            this.startTime = System.currentTimeMillis();
            helper.recordOperationStart(operationName);
        }

        /**
         * Records successful completion of the operation.
         */
        public void recordSuccess() {
            long duration = System.currentTimeMillis() - startTime;
            helper.recordOperationSuccess(operationName, duration);
        }

        /**
         * Records successful completion with account count.
         *
         * @param accountCount the number of accounts processed
         */
        public void recordSuccess(int accountCount) {
            recordSuccess();
            helper.recordAccountCount(operationName, accountCount);
        }

        /**
         * Records failure of the operation.
         *
         * @param error the exception that caused the failure
         */
        public void recordFailure(Exception error) {
            long duration = System.currentTimeMillis() - startTime;
            helper.recordOperationFailure(operationName, duration, error);
        }
    }
}
