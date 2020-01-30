package energy.emcos.web.controllers;

import energy.emcos.model.entity.ParamPt;
import energy.emcos.model.entity.PointAtParam;
import energy.emcos.model.entity.PointPt;
import energy.emcos.model.entity.PointPtParam;
import energy.emcos.model.repo.ParamPtRepo;
import energy.emcos.model.repo.PointPtParamRepo;
import energy.emcos.model.repo.PointPtRepo;
import energy.emcos.web.dto.PointPtParamDto;
import energy.emcos.web.dto.PointPtDto;
import lombok.RequiredArgsConstructor;
import org.dozer.DozerBeanMapper;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.annotation.PostConstruct;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

import static energy.emcos.util.Util.first;
import static java.util.stream.Collectors.toList;

@RestController
@RequiredArgsConstructor
public class PointPtController {
    private final PointPtRepo pointPtRepo;
    private final ParamPtRepo paramPtRepo;
    private final PointPtParamRepo pointPtParamRepo;
    private final DozerBeanMapper mapper;

    @PostConstruct
    private void init() {
        toPointPtDto = t -> mapper.map(t, PointPtDto.class);
        toPointPt = t -> mapper.map(t, PointPt.class);
        beforeSave = t -> t;
    }

    @GetMapping(value = "/api/v1/{lang}/emcos/point-pt", produces = "application/json")
    public ResponseEntity<List<PointPtDto>> getAll(@PathVariable(value = "lang")  String lang) {
        List<PointPtDto> list = pointPtRepo.findAll()
            .stream()
            .map(toPointPtDto::apply)
            .collect(toList());

        return ResponseEntity.ok(list);
    }

    @GetMapping(value = "/api/v1/{lang}/emcos/point-pt/{code}", produces = "application/json")
    public ResponseEntity<PointPtDto> getByCode(
        @PathVariable(value = "lang") String lang,
        @PathVariable(value = "code") String code
    ) {
        PointPtDto newDto = first(pointPtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .andThen(toPointPtDto)
            .apply(code);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(newDto);
    }

    @PostMapping(value = "/api/v1/{lang}/emcos/point-pt", produces = "application/json")
    public ResponseEntity<PointPtDto> create(
        @PathVariable(value = "lang") String lang,
        @RequestBody PointPtDto dto
    ) {
        PointPtDto newDto = first(toPointPt)
            .andThen(beforeSave)
            .andThen(pointPtRepo::save)
            .andThen(toPointPtDto)
            .apply(dto);

        List<ParamPt> params = paramPtRepo.findAll();
        for (ParamPt param: params) {
            PointPtParam pointPtParam = new PointPtParam(newDto.getCode(), param.getCode());
            pointPtParamRepo.save(pointPtParam);
        }
        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(newDto);
    }


    @PutMapping(value = "/api/v1/{lang}/emcos/point-pt/{code}", produces = "application/json")
    public ResponseEntity<PointPtDto> update(
        @PathVariable(value = "lang") String lang,
        @PathVariable(value = "code") String code,
        @RequestBody PointPtDto dto
    ) {
        PointPt entity = first(pointPtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .apply(code);

        mapper.map(dto, entity);

        PointPtDto newDto = first(beforeSave)
            .andThen(pointPtRepo::save)
            .andThen(toPointPtDto)
            .apply(entity);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(newDto);
    }

    @DeleteMapping(value = "/api/v1/{lang}/emcos/point-pt/{code}", produces = "application/json")
    public ResponseEntity<Void> remove(
        @PathVariable(value = "lang") String lang,
        @PathVariable(value = "code") String code
    ) {
        PointPt entity = first(pointPtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .apply(code);

        pointPtRepo.delete(entity);

        return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
    }


    @GetMapping(value = "/api/v1/{lang}/emcos/point-pt/{code}/param-pt", produces = "application/json")
    public ResponseEntity<List<PointPtParamDto>> getParams(
        @PathVariable(value = "lang")  String lang,
        @PathVariable(value = "code") String code
    ) {
        PointPt point = first(pointPtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .apply(code);

        List<PointPtParamDto> list = pointPtParamRepo.findAllByPointCode(point.getCode())
            .stream()
            .map(t -> mapper.map(t, PointPtParamDto.class))
            .collect(toList());

        return ResponseEntity.ok(list);
    }


    @PostMapping(value = "/api/v1/{lang}/emcos/point-pt/{code}/add-param-pt", produces = "application/json")
    public ResponseEntity<Void> addParam(
        @PathVariable(value = "lang") String lang,
        @PathVariable(value = "code") String code,
        @RequestBody PointPtParamDto dto
    ) {
        PointPt point = first(pointPtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .apply(code);

        ParamPt paramPt = first(paramPtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .apply(dto.getParamCode());

        Optional<PointPtParam> optPointPtParam = pointPtParamRepo.findAllByPointCode(point.getCode())
            .stream()
            .filter(t -> t.getParamCode().equals(paramPt.getCode()))
            .findFirst();

        PointPtParam pointPtParam = optPointPtParam.isPresent()
            ? optPointPtParam.get()
            : new PointPtParam(point.getCode(), paramPt.getCode());

        pointPtParam.setLastMeteringDate(dto.getLastMeteringDate());
        pointPtParamRepo.save(pointPtParam);

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build();
    }

    @PostMapping(value = "/api/v1/{lang}/emcos/point-pt/{code}/remove-param-pt", produces = "application/json")
    public ResponseEntity<Void> removeParam(
        @PathVariable(value = "lang") String lang,
        @PathVariable(value = "code") String code,
        @RequestBody PointPtParamDto dto
    ) {
        PointPt point = first(pointPtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .apply(code);

        ParamPt paramPt = first(paramPtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                    return t;
            })
            .andThen(Optional::get)
            .apply(dto.getParamCode());

        Optional<PointPtParam> pointPtParam = pointPtParamRepo.findAllByPointCode(point.getCode())
            .stream()
            .filter(t -> t.getParamCode().equals(paramPt.getCode()))
            .findFirst();

        if (pointPtParam.isPresent())
            pointPtParamRepo.delete(pointPtParam.get());

        return ResponseEntity
            .status(HttpStatus.NO_CONTENT)
            .build();
    }

    private Function<PointPt, PointPtDto> toPointPtDto;
    private Function<PointPtDto, PointPt> toPointPt;
    private Function<PointPt, PointPt> beforeSave;
}
