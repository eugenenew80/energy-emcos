package energy.emcos.web.controllers;

import energy.emcos.model.entity.ParamPt;
import energy.emcos.model.repo.ParamPtRepo;
import energy.emcos.web.dto.ParamPtDto;
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
public class ParamPtController {
    private final ParamPtRepo paramPtRepo;
    private final DozerBeanMapper mapper;

    @PostConstruct
    private void init() {
        toParamPtDto = t -> mapper.map(t, ParamPtDto.class);
        toParamPt = t -> mapper.map(t, ParamPt.class);
        beforeSave = t -> t;
    }

    @GetMapping(value = "/api/v1/{lang}/emcos/param-pt", produces = "application/json")
    public ResponseEntity<List<ParamPtDto>> getAll(@PathVariable(value = "lang")  String lang) {
        List<ParamPtDto> list = paramPtRepo.findAll()
            .stream()
            .map(toParamPtDto::apply)
            .collect(toList());

        return ResponseEntity.ok(list);
    }

    @GetMapping(value = "/api/v1/{lang}/emcos/param-pt/{code}", produces = "application/json")
    public ResponseEntity<ParamPtDto> getByCode(
        @PathVariable(value = "lang") String lang,
        @PathVariable(value = "code") String code
    ) {
        ParamPtDto newDto = first(paramPtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .andThen(toParamPtDto)
            .apply(code);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(newDto);
    }

    @PostMapping(value = "/api/v1/{lang}/emcos/param-pt", produces = "application/json")
    public ResponseEntity<ParamPtDto> create(
        @PathVariable(value = "lang") String lang,
        @RequestBody ParamPtDto dto
    ) {
        ParamPtDto newDto = first(toParamPt)
            .andThen(beforeSave)
            .andThen(paramPtRepo::save)
            .andThen(toParamPtDto)
            .apply(dto);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(newDto);
    }

    @PutMapping(value = "/api/v1/{lang}/emcos/param-pt/{code}", produces = "application/json")
    public ResponseEntity<ParamPtDto> update(
        @PathVariable(value = "lang") String lang,
        @PathVariable(value = "code") String code,
        @RequestBody ParamPtDto dto
    ) {
        ParamPt entity = first(paramPtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .apply(code);

        mapper.map(dto, entity);

        ParamPtDto newDto = first(beforeSave)
            .andThen(paramPtRepo::save)
            .andThen(toParamPtDto)
            .apply(entity);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(newDto);
    }

    @DeleteMapping(value = "/api/v1/{lang}/emcos/param-pt/{code}", produces = "application/json")
    public ResponseEntity<Void> remove(
            @PathVariable(value = "lang") String lang,
            @PathVariable(value = "code") String code
    ) {
        ParamPt entity = first(paramPtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .apply(code);

        paramPtRepo.delete(entity);

        return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
    }

    private Function<ParamPt, ParamPtDto> toParamPtDto;
    private Function<ParamPtDto, ParamPt> toParamPt;
    private Function<ParamPt, ParamPt> beforeSave;
}
