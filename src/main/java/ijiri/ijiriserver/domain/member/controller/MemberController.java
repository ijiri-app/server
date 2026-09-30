package ijiri.ijiriserver.domain.member.controller;

import ijiri.ijiriserver.domain.member.dto.response.MemberResponse;
import ijiri.ijiriserver.domain.member.exception.MemberStatusCode;
import ijiri.ijiriserver.domain.member.service.MemberService;
import ijiri.ijiriserver.global.jwt.JwtCookieManager;
import ijiri.ijiriserver.global.response.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Member", description = "회원")
@RestController
@RequestMapping("/members")
@RequiredArgsConstructor
public class MemberController {

    private final MemberService memberService;
    private final JwtCookieManager jwtCookieManager;

    @Operation(
            summary = "내 정보 조회"
    )
    @GetMapping("/me")
    public BaseResponse<MemberResponse> getMe(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId
    ) {
        return BaseResponse.ok(memberService.getMember(Long.valueOf(memberId)));
    }

    @Operation(
            summary = "회원 탈퇴",
            description = "카카오 회원은 카카오 연결 끊기까지 함께 처리. 토큰 쿠키도 만료"
    )
    @DeleteMapping("/me")
    public BaseResponse<MemberResponse> withdraw(
            @Parameter(hidden = true) @AuthenticationPrincipal String memberId,
            HttpServletResponse httpResponse
    ) {
        MemberResponse response = memberService.withdraw(Long.valueOf(memberId));
        jwtCookieManager.expireTokenCookies(httpResponse);
        return BaseResponse.of(MemberStatusCode.WITHDRAW_SUCCESS, response);
    }
}
