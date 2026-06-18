-- Analytics records for monitoring response quality and usage
CREATE TABLE IF NOT EXISTS analytics (
    id                UUID            NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    conversation_id   UUID,
    response_time_ms  BIGINT,
    confidence_score  DECIMAL(5, 4),
    question_language VARCHAR(10),
    user_country      VARCHAR(10),
    was_answered      BOOLEAN         NOT NULL DEFAULT FALSE,
    timestamp         TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_analytics_timestamp    ON analytics (timestamp DESC);
CREATE INDEX IF NOT EXISTS idx_analytics_user_country ON analytics (user_country);
CREATE INDEX IF NOT EXISTS idx_analytics_was_answered ON analytics (was_answered);
