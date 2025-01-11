CREATE TABLE channels (
    id BIGSERIAL PRIMARY KEY,
    channel_id VARCHAR(255) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT NOT NULL,
    subscriber_count BIGINT NOT NULL,
    video_count BIGINT NOT NULL,
    view_count BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE channel_stats (
    id BIGSERIAL PRIMARY KEY,
    channel_id BIGINT NOT NULL REFERENCES channels(id),
    subscriber_count BIGINT NOT NULL,
    video_count BIGINT NOT NULL,
    view_count BIGINT NOT NULL,
    collected_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_channel_stats_channel_id ON channel_stats(channel_id);
CREATE INDEX idx_channel_stats_collected_at ON channel_stats(collected_at);
