package io.github.stellhub.stellspec.internal;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class MessageTruncatorTest {

    @Test
    void shouldKeepShortMessageUntouched() {
        MessageTruncator.Result result = MessageTruncator.truncate("hello");
        assertEquals("hello", result.getMessage());
        assertTrue(result.getAttributes().isEmpty());
    }

    @Test
    void shouldMarkLongMessageAsTruncated() {
        String message = "a".repeat(MessageTruncator.MAX_BODY_BYTES + 16);
        MessageTruncator.Result result = MessageTruncator.truncate(message);
        assertTrue((Boolean) result.getAttributes().get("log.body_truncated"));
        assertEquals(MessageTruncator.MAX_BODY_BYTES, result.getMessage().length());
    }
}
