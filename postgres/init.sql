CREATE TABLE IF NOT EXISTS spring_ai_chat_memory (
    conversation_id VARCHAR(256) NOT NULL,
    content         TEXT        NOT NULL,
    type            VARCHAR(100) NOT NULL,
    "timestamp"     TIMESTAMP WITH TIME ZONE DEFAULT CURRENT_TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS spring_ai_chat_memory_conversation_id_timestamp_idx
    ON spring_ai_chat_memory (conversation_id, "timestamp");
