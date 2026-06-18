-- Conversation history per Teams conversation thread
CREATE TABLE IF NOT EXISTS conversations (
    id                      UUID            NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    teams_conversation_id   VARCHAR(500)    NOT NULL,
    user_id                 VARCHAR(500)    NOT NULL,
    user_name               VARCHAR(255),
    user_country            VARCHAR(10),
    question                TEXT            NOT NULL,
    response                TEXT,
    confidence_score        DECIMAL(5, 4),
    timestamp               TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_conversations_teams_id  ON conversations (teams_conversation_id);
CREATE INDEX IF NOT EXISTS idx_conversations_user_id   ON conversations (user_id);
CREATE INDEX IF NOT EXISTS idx_conversations_timestamp ON conversations (timestamp DESC);
