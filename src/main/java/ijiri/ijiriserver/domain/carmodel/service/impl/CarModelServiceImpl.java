package ijiri.ijiriserver.domain.carmodel.service.impl;

import ijiri.ijiriserver.domain.carmodel.dto.CarSpec;
import ijiri.ijiriserver.domain.carmodel.dto.response.CarModelResponse;
import ijiri.ijiriserver.domain.carmodel.entity.CarGeneration;
import ijiri.ijiriserver.domain.carmodel.entity.CarModel;
import ijiri.ijiriserver.domain.carmodel.entity.CarTrim;
import ijiri.ijiriserver.domain.carmodel.exception.CarModelStatusCode;
import ijiri.ijiriserver.domain.carmodel.repository.CarGenerationRepository;
import ijiri.ijiriserver.domain.carmodel.repository.CarModelRepository;
import ijiri.ijiriserver.domain.carmodel.repository.CarTrimRepository;
import ijiri.ijiriserver.domain.carmodel.service.CarModelService;
import ijiri.ijiriserver.global.exception.CustomException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CarModelServiceImpl implements CarModelService {

    private static final String MASTER_CSV = "car-master/car_master.csv";
    // manufacturer,model,generation_code,generation_name,start_year,end_year,trim
    private static final int COLUMN_COUNT = 7;

    private final CarModelRepository carModelRepository;
    private final CarGenerationRepository carGenerationRepository;
    private final CarTrimRepository carTrimRepository;

    @Override
    public CarModelResponse getCarModels() {
        return CarModelResponse.list(carModelRepository.findAllByOrderByDisplayOrderAscIdAsc());
    }

    @Override
    public CarModelResponse getCarModel(Long carModelId) {
        CarModel model = carModelRepository.findById(carModelId)
                .orElseThrow(() -> new CustomException(CarModelStatusCode.CAR_MODEL_NOT_FOUND));
        List<CarGeneration> generations = carGenerationRepository.findAllByCarModelIdOrderByStartYearAscIdAsc(
                carModelId
        );
        List<CarTrim> trims = carTrimRepository.findAllByCarGenerationIdInOrderByIdAsc(
                generations.stream().map(CarGeneration::getId).toList()
        );
        return CarModelResponse.detail(model, generations, trims);
    }

    @Override
    public void validateCarModelsExist(Collection<Long> carModelIds) {
        Collection<Long> distinct = new HashSet<>(carModelIds);
        if (carModelRepository.countByIdIn(distinct) != distinct.size()) {
            throw new CustomException(CarModelStatusCode.CAR_MODEL_NOT_FOUND);
        }
    }

    @Override
    public CarSpec getSpec(Long carModelId, Long generationId, Long trimId) {
        CarModel model = carModelRepository.findById(carModelId)
                .orElseThrow(() -> new CustomException(CarModelStatusCode.INVALID_CAR_SPEC));
        CarGeneration generation = carGenerationRepository.findById(generationId)
                .filter(found -> found.getCarModelId().equals(carModelId))
                .orElseThrow(() -> new CustomException(CarModelStatusCode.INVALID_CAR_SPEC));
        CarTrim trim = trimId == null ? null : carTrimRepository.findById(trimId)
                .filter(found -> found.getCarGenerationId().equals(generationId))
                .orElseThrow(() -> new CustomException(CarModelStatusCode.INVALID_CAR_SPEC));
        return CarSpec.of(model, generation, trim);
    }

    @Override
    public Map<Long, String> getModelNames(Collection<Long> carModelIds) {
        return carModelRepository.findAllById(carModelIds).stream()
                .collect(Collectors.toMap(CarModel::getId, CarModel::getName));
    }

    // 차종 마스터는 CSV 로 관리한다. 기동할 때마다 CSV 에 있고 DB 에 없는 행만 추가하므로 여러 번 실행해도 안전하다.
    // 이미 들어간 행의 수정·삭제는 게시물이 참조하므로 마이그레이션으로 따로 처리한다
    @EventListener(ApplicationReadyEvent.class)
    @Transactional
    public void importMasterData() {
        List<String[]> rows = readCsv();
        int nextOrder = carModelRepository.findAllByOrderByDisplayOrderAscIdAsc().size();
        int added = 0;
        for (String[] row : rows) {
            CarModel model = carModelRepository.findByManufacturerAndName(row[0], row[1]).orElse(null);
            if (model == null) {
                model = carModelRepository.save(CarModel.builder()
                        .manufacturer(row[0])
                        .name(row[1])
                        .displayOrder(nextOrder++)
                        .build()
                );
            }
            CarGeneration generation = findOrCreateGeneration(model.getId(), row);
            if (!carTrimRepository.existsByCarGenerationIdAndName(generation.getId(), row[6])) {
                carTrimRepository.save(CarTrim.builder()
                        .carGenerationId(generation.getId())
                        .name(row[6])
                        .build()
                );
                added++;
            }
        }
        log.info("Car master import finished: {} rows, {} new trims", rows.size(), added);
    }

    private CarGeneration findOrCreateGeneration(Long carModelId, String[] row) {
        return carGenerationRepository.findByCarModelIdAndCode(carModelId, row[2])
                .orElseGet(() -> carGenerationRepository.save(CarGeneration.builder()
                        .carModelId(carModelId)
                        .code(row[2])
                        .name(row[3])
                        .startYear(Integer.parseInt(row[4]))
                        .endYear(row[5].isEmpty() ? null : Integer.parseInt(row[5]))
                        .build()
                ));
    }

    // 첫 줄은 헤더. 값에 쉼표가 들어가지 않는 단순 CSV 만 받는다
    private List<String[]> readCsv() {
        ClassPathResource resource = new ClassPathResource(MASTER_CSV);
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8)
        )) {
            return reader.lines()
                    .skip(1)
                    .filter(line -> !line.isBlank())
                    .map(this::parseRow)
                    .toList();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read " + MASTER_CSV, e);
        }
    }

    private String[] parseRow(String line) {
        String[] columns = line.split(",", -1);
        if (columns.length != COLUMN_COUNT) {
            throw new IllegalStateException("Invalid car master row: " + line);
        }
        return Arrays.stream(columns)
                .map(String::strip)
                .toArray(String[]::new);
    }
}
