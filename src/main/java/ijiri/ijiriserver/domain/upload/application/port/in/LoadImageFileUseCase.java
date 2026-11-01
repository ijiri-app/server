package ijiri.ijiriserver.domain.upload.application.port.in;

import ijiri.ijiriserver.domain.upload.application.data.ImageFileContent;

public interface LoadImageFileUseCase {

    ImageFileContent load(String imageKey);
}
