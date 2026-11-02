package ijiri.ijiriserver.domain.carmodel.application.service

import ijiri.ijiriserver.domain.carmodel.application.data.CarModelInfo
import ijiri.ijiriserver.domain.carmodel.application.data.response.CarModelResponse
import ijiri.ijiriserver.domain.carmodel.application.port.`in`.GetCarModelInfosUseCase
import ijiri.ijiriserver.domain.carmodel.application.port.`in`.GetCarModelNamesUseCase
import ijiri.ijiriserver.domain.carmodel.application.port.`in`.GetCarModelUseCase
import ijiri.ijiriserver.domain.carmodel.application.port.`in`.GetCarModelsUseCase
import ijiri.ijiriserver.domain.carmodel.application.port.`in`.GetCarSpecByTrimUseCase
import ijiri.ijiriserver.domain.carmodel.application.port.`in`.ValidateCarModelsExistUseCase
import ijiri.ijiriserver.domain.carmodel.application.port.`in`.ValidateModelYearUseCase
import ijiri.ijiriserver.domain.carmodel.application.port.out.CarGenerationPort
import ijiri.ijiriserver.domain.carmodel.application.port.out.CarModelPort
import ijiri.ijiriserver.domain.carmodel.application.port.out.CarTrimPort
import ijiri.ijiriserver.domain.carmodel.entity.CarGeneration
import ijiri.ijiriserver.domain.carmodel.entity.CarModel
import ijiri.ijiriserver.domain.carmodel.entity.CarSpec
import ijiri.ijiriserver.domain.carmodel.entity.CarTrim
import ijiri.ijiriserver.domain.carmodel.exception.CarModelStatusCode
import ijiri.ijiriserver.global.exception.exception.CustomException
import org.slf4j.LoggerFactory
import org.springframework.boot.context.event.ApplicationReadyEvent
import org.springframework.context.event.EventListener
import org.springframework.core.io.ClassPathResource
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.IOException
import java.io.UncheckedIOException
import java.time.Clock
import java.time.LocalDate
import java.util.Locale

@Service
@Transactional(readOnly = true)
class CarModelService(
    private val carModelPort: CarModelPort,
    private val carGenerationPort: CarGenerationPort,
    private val carTrimPort: CarTrimPort,
    private val clock: Clock,
) : GetCarModelsUseCase,
    GetCarModelUseCase,
    ValidateCarModelsExistUseCase,
    GetCarSpecByTrimUseCase,
    ValidateModelYearUseCase,
    GetCarModelInfosUseCase,
    GetCarModelNamesUseCase {

    override fun getCarModels(keyword: String?, coreOnly: Boolean): CarModelResponse {
        val normalized = keyword?.trim()?.lowercase(Locale.ROOT) ?: ""
        return CarModelResponse.list(carModelPort.search(normalized, coreOnly))
    }

    override fun getCarModel(carModelId: Long): CarModelResponse {
        val model = carModelPort.findByIdAndHiddenFalse(carModelId)
            ?: throw CustomException(CarModelStatusCode.CAR_MODEL_NOT_FOUND)
        val generations = carGenerationPort.findAllByCarModelIdOrderByStartYearAscIdAsc(carModelId)
        val trims = carTrimPort.findAllByCarGenerationIdInOrderByIdAsc(generations.map(CarGeneration::id))
        return CarModelResponse.detail(model, generations, trims)
    }

    override fun validateCarModelsExist(carModelIds: Collection<Long>) {
        val distinct = carModelIds.toHashSet()
        if (carModelPort.countByIdInAndHiddenFalse(distinct) != distinct.size.toLong()) {
            throw CustomException(CarModelStatusCode.CAR_MODEL_NOT_FOUND)
        }
    }

    // 숨긴 차종이어도 이미 연결된 보유 차량·게시물은 계속 보여야 하므로 숨김 여부는 보지 않는다
    override fun getSpecByTrim(trimId: Long): CarSpec {
        val trim = carTrimPort.findById(trimId)
            ?: throw CustomException(CarModelStatusCode.INVALID_TRIM)
        val generation = carGenerationPort.findById(trim.carGenerationId) ?: throw NoSuchElementException()
        val model = carModelPort.findById(generation.carModelId) ?: throw NoSuchElementException()
        return CarSpec.of(model, generation, trim)
    }

    override fun validateModelYear(generationId: Long, year: Int) {
        val generation = carGenerationPort.findById(generationId)
            ?: throw CustomException(CarModelStatusCode.INVALID_TRIM)
        if (!generation.coversYear(year, LocalDate.now(clock).year)) {
            throw CustomException(CarModelStatusCode.INVALID_MODEL_YEAR)
        }
    }

    override fun getCarModelInfos(carModelIds: List<Long>): List<CarModelInfo> {
        val models = carModelPort.findAllById(carModelIds).associateBy(CarModel::id)
        return carModelIds.mapNotNull { models[it] }
            .map(CarModelInfo::from)
    }

    override fun getModelNames(carModelIds: Collection<Long>): Map<Long, String> {
        return carModelPort.findAllById(carModelIds).associate { it.id to it.name }
    }

    // 차종 마스터는 CSV 로 관리한다. 기동할 때마다 CSV 에 있고 DB 에 없는 행만 추가하므로 여러 번 실행해도 안전하다.
    // 이미 들어간 행의 수정·숨김은 게시물이 참조하므로 마이그레이션 또는 관리자 기능으로 처리한다
    @EventListener(ApplicationReadyEvent::class)
    @Transactional
    fun importMasterData() {
        val rows = readCsv()
        var nextOrder = carModelPort.count().toInt()
        var added = 0
        for (row in rows) {
            val model = carModelPort.findByBrandAndName(row[0], row[1])
                ?: carModelPort.save(
                    CarModel(
                        brand = row[0],
                        name = row[1],
                        core = row[2].toBoolean(),
                        displayOrder = nextOrder++,
                    ),
                )
            val generation = findOrCreateGeneration(model.id, row)
            if (!carTrimPort.existsByCarGenerationIdAndName(generation.id, row[6])) {
                carTrimPort.save(
                    CarTrim(
                        carGenerationId = generation.id,
                        name = row[6],
                    ),
                )
                added++
            }
        }
        log.info("Car master import finished: {} rows, {} new trims", rows.size, added)
    }

    private fun findOrCreateGeneration(carModelId: Long, row: List<String>): CarGeneration {
        return carGenerationPort.findByCarModelIdAndCode(carModelId, row[3])
            ?: carGenerationPort.save(
                CarGeneration(
                    carModelId = carModelId,
                    code = row[3],
                    startYear = row[4].toInt(),
                    endYear = if (row[5].isEmpty()) null else row[5].toInt(),
                ),
            )
    }

    // 첫 줄은 헤더. 값에 쉼표가 들어가지 않는 단순 CSV 만 받는다
    private fun readCsv(): List<List<String>> {
        val resource = ClassPathResource(MASTER_CSV)
        try {
            return resource.inputStream.bufferedReader(Charsets.UTF_8).useLines { lines ->
                lines.drop(1)
                    .filter { line -> line.isNotBlank() }
                    .map(::parseRow)
                    .toList()
            }
        } catch (e: IOException) {
            throw UncheckedIOException("Failed to read $MASTER_CSV", e)
        }
    }

    private fun parseRow(line: String): List<String> {
        val columns = line.split(",")
        if (columns.size != COLUMN_COUNT) {
            throw IllegalStateException("Invalid car master row: $line")
        }
        return columns.map(String::trim)
    }

    companion object {
        private const val MASTER_CSV = "car-master/car_master.csv"

        // brand,model,is_core,generation_code,start_year,end_year,trim
        private const val COLUMN_COUNT = 7
        private val log = LoggerFactory.getLogger(CarModelService::class.java)
    }
}
