-- 이미 처리한 메시지 id. 중복 전달(at-least-once)된 part.usage-changed 를 두 번 세지 않는다
CREATE TABLE processed_message
(
    message_id   VARCHAR(36)  PRIMARY KEY,
    processed_at TIMESTAMP(6) NOT NULL
);
