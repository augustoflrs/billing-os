package com.billingos.dte;

import java.time.Instant;
import java.util.List;

public class DteDto {

    public record DteStatusResponse(
            String id,
            String controlNumber,
            String generationCode,
            String statusCode,
            String statusName,
            String mhCode,
            String mhMessage,
            Instant submittedAt,
            Instant acceptedAt,
            int attemptCount,
            Instant nextAttemptAt
    ) {}

    public record DteEventResponse(
            String id,
            String eventTypeCode,
            String eventLabel,
            Instant eventTime
    ) {}

    public record DteEventsResponse(List<DteEventResponse> events) {}
}
