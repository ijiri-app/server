# 공개 리스너의 모든 라우팅이 포함한다. 검증 결과 헤더를 서비스에 넘기고, 클라이언트가 보낸 같은 이름의 헤더는 덮어쓴다
# (값이 비면 헤더를 보내지 않는다)
auth_request /_auth;
auth_request_set $member_id $upstream_http_x_member_id;
auth_request_set $member_role $upstream_http_x_member_role;
proxy_http_version 1.1;
proxy_set_header Connection "";
proxy_set_header Host $host;
# cloudflared 가 넣은 실제 클라이언트 IP 뒤에 덧붙인다. Spring 은 내부망 프록시를 건너뛰고 클라이언트 IP 를 얻는다
proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
proxy_set_header X-Forwarded-Proto $http_x_forwarded_proto;
proxy_set_header X-Member-Id $member_id;
proxy_set_header X-Member-Role $member_role;
