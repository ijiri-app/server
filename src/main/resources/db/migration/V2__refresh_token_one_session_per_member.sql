-- 계정당 세션 1개를 DB 가 보장한다. 같은 계정의 로그인이 동시에 들어와도 refresh token 은 한 건만 남는다.
-- session_id 는 V1 이전 데이터를 위한 nullable 이었으므로 NOT NULL 로 바꾼다.

-- 세션이 없는 옛 행과, 경합으로 한 회원에게 여러 개 남은 행은 가장 최근 것만 남긴다
DELETE FROM refresh_token WHERE session_id IS NULL;
DELETE FROM refresh_token older
    USING refresh_token newer
WHERE older.member_id = newer.member_id
  AND older.id < newer.id;

DROP INDEX idx_refresh_token_member_id;
ALTER TABLE refresh_token ADD CONSTRAINT uk_refresh_token_member_id UNIQUE (member_id);
ALTER TABLE refresh_token ALTER COLUMN session_id SET NOT NULL;
