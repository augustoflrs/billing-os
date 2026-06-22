package com.billingos.common;

import java.security.SecureRandom;
import java.time.Instant;

/**
 * Generates Crockford Base32 ULIDs — sortable, URL-safe 26-char IDs.
 * Used as primary keys across all entities (varchar(26) columns).
 */
public final class UlidGenerator {

    private static final char[] ENCODING = "0123456789ABCDEFGHJKMNPQRSTVWXYZ".toCharArray();
    private static final SecureRandom RANDOM = new SecureRandom();

    private UlidGenerator() {}

    public static String generate() {
        long now = Instant.now().toEpochMilli();
        byte[] entropy = new byte[10];
        RANDOM.nextBytes(entropy);

        char[] chars = new char[26];

        // 10 chars for timestamp (48 bits)
        chars[0] = ENCODING[(int) ((now >>> 45) & 0x1F)];
        chars[1] = ENCODING[(int) ((now >>> 40) & 0x1F)];
        chars[2] = ENCODING[(int) ((now >>> 35) & 0x1F)];
        chars[3] = ENCODING[(int) ((now >>> 30) & 0x1F)];
        chars[4] = ENCODING[(int) ((now >>> 25) & 0x1F)];
        chars[5] = ENCODING[(int) ((now >>> 20) & 0x1F)];
        chars[6] = ENCODING[(int) ((now >>> 15) & 0x1F)];
        chars[7] = ENCODING[(int) ((now >>> 10) & 0x1F)];
        chars[8] = ENCODING[(int) ((now >>> 5) & 0x1F)];
        chars[9] = ENCODING[(int) (now & 0x1F)];

        // 16 chars for randomness (80 bits)
        chars[10] = ENCODING[(entropy[0] & 0xFF) >>> 3];
        chars[11] = ENCODING[((entropy[0] & 0x07) << 2) | ((entropy[1] & 0xFF) >>> 6)];
        chars[12] = ENCODING[(entropy[1] & 0x3E) >>> 1];
        chars[13] = ENCODING[((entropy[1] & 0x01) << 4) | ((entropy[2] & 0xFF) >>> 4)];
        chars[14] = ENCODING[((entropy[2] & 0x0F) << 1) | ((entropy[3] & 0xFF) >>> 7)];
        chars[15] = ENCODING[(entropy[3] & 0x7C) >>> 2];
        chars[16] = ENCODING[((entropy[3] & 0x03) << 3) | ((entropy[4] & 0xFF) >>> 5)];
        chars[17] = ENCODING[entropy[4] & 0x1F];
        chars[18] = ENCODING[(entropy[5] & 0xFF) >>> 3];
        chars[19] = ENCODING[((entropy[5] & 0x07) << 2) | ((entropy[6] & 0xFF) >>> 6)];
        chars[20] = ENCODING[(entropy[6] & 0x3E) >>> 1];
        chars[21] = ENCODING[((entropy[6] & 0x01) << 4) | ((entropy[7] & 0xFF) >>> 4)];
        chars[22] = ENCODING[((entropy[7] & 0x0F) << 1) | ((entropy[8] & 0xFF) >>> 7)];
        chars[23] = ENCODING[(entropy[8] & 0x7C) >>> 2];
        chars[24] = ENCODING[((entropy[8] & 0x03) << 3) | ((entropy[9] & 0xFF) >>> 5)];
        chars[25] = ENCODING[entropy[9] & 0x1F];

        return new String(chars);
    }
}
