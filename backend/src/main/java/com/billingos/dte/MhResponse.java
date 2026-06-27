package com.billingos.dte;

import java.util.Map;

/**
 * Typed result from a MH DTE submission attempt.
 */
public record MhResponse(
        Kind           kind,
        String         mhCode,
        String         mhMessage,
        String         selloRecibido,
        Map<String, Object> requestJson,
        Map<String, Object> responseJson
) {
    public enum Kind { ACCEPTED, REJECTED, TRANSIENT_ERROR }

    static MhResponse accepted(String sello, String code, String msg,
                               Map<String, Object> req, Map<String, Object> res) {
        return new MhResponse(Kind.ACCEPTED, code, msg, sello, req, res);
    }

    static MhResponse rejected(String code, String msg,
                               Map<String, Object> req, Map<String, Object> res) {
        return new MhResponse(Kind.REJECTED, code, msg, null, req, res);
    }

    static MhResponse transientError(String msg, Map<String, Object> req) {
        return new MhResponse(Kind.TRANSIENT_ERROR, null, msg, null, req, null);
    }
}
