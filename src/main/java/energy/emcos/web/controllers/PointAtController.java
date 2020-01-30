package energy.emcos.web.controllers;

import energy.emcos.model.entity.ParamAt;
import energy.emcos.model.entity.PointAt;
import energy.emcos.model.entity.PointAtParam;
import energy.emcos.model.repo.ParamAtRepo;
import energy.emcos.model.repo.PointAtParamRepo;
import energy.emcos.model.repo.PointAtRepo;
import energy.emcos.web.dto.PointAtDto;
import energy.emcos.web.dto.PointAtParamDto;
import energy.emcos.web.dto.PointPtParamDto;
import lombok.RequiredArgsConstructor;
import org.dozer.DozerBeanMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import javax.annotation.PostConstruct;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import static energy.emcos.util.Util.first;
import static java.util.stream.Collectors.toList;

@RestController
@RequiredArgsConstructor
public class PointAtController {
    private final PointAtRepo pointAtRepo;
    private final ParamAtRepo paramAtRepo;
    private final PointAtParamRepo pointAtParamRepo;
    private final DozerBeanMapper mapper;

    @PostConstruct
    private void init() {
        toPointAtDto = t -> mapper.map(t, PointAtDto.class);
        toPointAt = t -> mapper.map(t, PointAt.class);
        beforeSave = t -> t;
    }

    @GetMapping(value = "/api/v1/{lang}/emcos/point-at", produces = "application/json")
    public ResponseEntity<List<PointAtDto>> getAll(@PathVariable(value = "lang")  String lang) {
        List<PointAtDto> list = pointAtRepo.findAll()
            .stream()
            .map(toPointAtDto::apply)
            .collect(toList());

        return ResponseEntity.ok(list);
    }

    @GetMapping(value = "/api/v1/{lang}/emcos/point-at/{code}", produces = "application/json")
    public ResponseEntity<PointAtDto> getByCode(
        @PathVariable(value = "lang") String lang,
        @PathVariable(value = "code") String code
    ) {
        PointAtDto newDto = first(pointAtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .andThen(toPointAtDto)
            .apply(code);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(newDto);
    }

    @PostMapping(value = "/api/v1/{lang}/emcos/point-at", produces = "application/json")
    public ResponseEntity<PointAtDto> create(
        @PathVariable(value = "lang") String lang,
        @RequestBody PointAtDto dto
    ) {
        PointAtDto newDto = first(toPointAt)
            .andThen(beforeSave)
            .andThen(pointAtRepo::save)
            .andThen(toPointAtDto)
            .apply(dto);

        List<ParamAt> params = paramAtRepo.findAll();
        for (ParamAt param: params) {
            PointAtParam pointAtParam = new PointAtParam(newDto.getCode(), param.getCode());
            pointAtParamRepo.save(pointAtParam);
        }

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(newDto);
    }

    @PutMapping(value = "/api/v1/{lang}/emcos/point-at/{code}", produces = "application/json")
    public ResponseEntity<PointAtDto> update(
        @PathVariable(value = "lang") String lang,
        @PathVariable(value = "code") String code,
        @RequestBody PointAtDto dto
    ) {
        PointAt entity = first(pointAtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .apply(code);

        mapper.map(dto, entity);

        PointAtDto newDto = first(beforeSave)
            .andThen(pointAtRepo::save)
            .andThen(toPointAtDto)
            .apply(entity);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(newDto);
    }

    @DeleteMapping(value = "/api/v1/{lang}/emcos/point-at/{code}", produces = "application/json")
    public ResponseEntity<Void> remove(
        @PathVariable(value = "lang") String lang,
        @PathVariable(value = "code") String code
    ) {
        PointAt entity = first(pointAtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .apply(code);

        pointAtRepo.delete(entity);

        return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
    }

    @GetMapping(value = "/api/v1/{lang}/emcos/point-at/{code}/param-at", produces = "application/json")
    public ResponseEntity<List<PointPtParamDto>> getParams(
        @PathVariable(value = "lang")  String lang,
        @PathVariable(value = "code") String code
    ) {
        PointAt point = first(pointAtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .apply(code);

        List<PointPtParamDto> list = pointAtParamRepo.findAllByPointCode(point.getCode())
            .stream()
            .map(t -> mapper.map(t, PointPtParamDto.class))
            .collect(toList());

        return ResponseEntity.ok(list);
    }


    @PostMapping(value = "/api/v1/{lang}/emcos/point-at/{code}/add-param-at", produces = "application/json")
    public ResponseEntity<Void> addParam(
        @PathVariable(value = "lang") String lang,
        @PathVariable(value = "code") String code,
        @RequestBody PointAtParamDto dto
    ) {
        PointAt point = first(pointAtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .apply(code);

        ParamAt paramPt = first(paramAtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .apply(dto.getParamCode());

        Optional<PointAtParam> optPointPtParam = pointAtParamRepo.findAllByPointCode(point.getCode())
            .stream()
            .filter(t -> t.getParamCode().equals(paramPt.getCode()))
            .findFirst();

        PointAtParam pointAtParam = optPointPtParam.isPresent()
            ? optPointPtParam.get()
            : new PointAtParam(point.getCode(), paramPt.getCode());

        pointAtParam.setLastMeteringDate(dto.getLastMeteringDate());
        pointAtParamRepo.save(pointAtParam);

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build();
    }

    @PostMapping(value = "/api/v1/{lang}/emcos/point-at/{code}/remove-param-at", produces = "application/json")
    public ResponseEntity<Void> removeParam(
            @PathVariable(value = "lang") String lang,
            @PathVariable(value = "code") String code,
            @RequestBody PointAtParamDto dto
    ) {
        PointAt point = first(pointAtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .apply(code);

        ParamAt paramPt = first(paramAtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .apply(dto.getParamCode());

        Optional<PointAtParam> pointPtParam = pointAtParamRepo.findAllByPointCode(point.getCode())
            .stream()
            .filter(t -> t.getParamCode().equals(paramPt.getCode()))
            .findFirst();

        if (pointPtParam.isPresent())
            pointAtParamRepo.delete(pointPtParam.get());

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build();
    }

    private Function<PointAt, PointAtDto> toPointAtDto;
    private Function<PointAtDto, PointAt> toPointAt;
    private Function<PointAt, PointAt> beforeSave;
}
