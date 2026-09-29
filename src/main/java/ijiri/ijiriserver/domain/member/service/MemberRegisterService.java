package ijiri.ijiriserver.domain.member.service;

import ijiri.ijiriserver.domain.member.dto.MemberRegisterCommand;
import ijiri.ijiriserver.domain.member.dto.MemberRegisterResult;

/**
 * provider + providerMemberId 로 회원을 찾고, 없으면 가입시킨다.
 */
public interface MemberRegisterService {

    MemberRegisterResult registerIfAbsent(MemberRegisterCommand command);
}
