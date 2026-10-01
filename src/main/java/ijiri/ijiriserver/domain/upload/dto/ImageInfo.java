package ijiri.ijiriserver.domain.upload.dto;

import ijiri.ijiriserver.domain.upload.entity.UploadedImage;

/**
 * 게시물에 붙일 때 복사해 두는 사진 정보. width, height 는 null 일 수 있다.
 */
public record ImageInfo(
        Long id,
        String url,
        Integer width,
        Integer height
) {

    public static ImageInfo from(UploadedImage image) {
        return new ImageInfo(image.getId(), image.getUrl(), image.getWidth(), image.getHeight());
    }
}
