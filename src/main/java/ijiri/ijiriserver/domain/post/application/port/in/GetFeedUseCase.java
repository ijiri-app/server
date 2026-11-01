package ijiri.ijiriserver.domain.post.application.port.in;

import ijiri.ijiriserver.domain.carmodel.entity.BuildStyle;
import ijiri.ijiriserver.domain.post.application.data.response.PostResponse;

import java.util.List;

public interface GetFeedUseCase {

    /**
     * carModelId 가 있으면 그 차종, 없으면 carModelIds, 그것도 없으면 로그인한 회원의 관심 차종 전체.
     * 관심 차종도 없으면 전체 게시물. viewerId 는 비로그인이면 null.
     */
    PostResponse getFeed(
            Long viewerId,
            Long carModelId,
            List<Long> carModelIds,
            BuildStyle buildStyle,
            Long cursor,
            int size
    );
}
