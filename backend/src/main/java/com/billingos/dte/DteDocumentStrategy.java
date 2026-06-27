package com.billingos.dte;

import java.util.Map;

public interface DteDocumentStrategy {
    String documentTypeCode();
    Map<String, Object> buildPayload(DteContext ctx, String generationCode, String controlNumber);
}
