package energy.emcos.web.controllers;

import energy.emcos.model.entity.ParamAt;
import energy.emcos.model.repo.ParamAtRepo;
import energy.emcos.web.dto.ParamAtDto;
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
public class ParamAtController {
    private final ParamAtRepo paramAtRepo;
    private final DozerBeanMapper mapper;

    @PostConstruct
    private void init() {
        toParamAtDto = t -> mapper.map(t, ParamAtDto.class);
        toParamAt = t -> mapper.map(t, ParamAt.class);
        beforeSave = t -> t;
    }

    @GetMapping(value = "/api/v1/{lang}/emcos/param-at", produces = "application/json")
    public ResponseEntity<List<ParamAtDto>> getAll(@PathVariable(value = "lang")  String lang) {
        List<ParamAtDto> list = paramAtRepo.findAll()
            .stream()
            .map(toParamAtDto::apply)
            .collect(toList());

        return ResponseEntity.ok(list);
    }

    @GetMapping(value = "/api/v1/{lang}/emcos/param-at/{code}", produces = "application/json")
    public ResponseEntity<ParamAtDto> getByCode(
        @PathVariable(value = "lang") String lang,
        @PathVariable(value = "code") String code
    ) {
        ParamAtDto newDto = first(paramAtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .andThen(toParamAtDto)
            .apply(code);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(newDto);
    }

    @PostMapping(value = "/api/v1/{lang}/emcos/param-at", produces = "application/json")
    public ResponseEntity<ParamAtDto> create(
        @PathVariable(value = "lang") String lang,
        @RequestBody ParamAtDto dto
    ) {
        ParamAtDto newDto = first(toParamAt)
            .andThen(beforeSave)
            .andThen(paramAtRepo::save)
            .andThen(toParamAtDto)
            .apply(dto);

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(newDto);
    }


    @PutMapping(value = "/api/v1/{lang}/emcos/param-at/{code}", produces = "application/json")
    public ResponseEntity<ParamAtDto> update(
        @PathVariable(value = "lang") String lang,
        @PathVariable(value = "code") String code,
        @RequestBody ParamAtDto dto
    ) {
        ParamAt entity = first(paramAtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .apply(code);

        mapper.map(dto, entity);

        ParamAtDto newDto = first(beforeSave)
            .andThen(paramAtRepo::save)
            .andThen(toParamAtDto)
            .apply(entity);

        return ResponseEntity
            .status(HttpStatus.OK)
            .body(newDto);
    }

    @DeleteMapping(value = "/api/v1/{lang}/emcos/param-at/{code}", produces = "application/json")
    public ResponseEntity<Void> remove(
        @PathVariable(value = "lang") String lang,
        @PathVariable(value = "code") String code
    ) {
        ParamAt entity = first(paramAtRepo::findById)
            .andThen(t -> {
                if (!t.isPresent()) throw new RuntimeException("entity not found");
                return t;
            })
            .andThen(Optional::get)
            .apply(code);

        paramAtRepo.delete(entity);

        return ResponseEntity
        .status(HttpStatus.NO_CONTENT)
        .build();
    }

    private Function<ParamAt, ParamAtDto> toParamAtDto;
    private Function<ParamAtDto, ParamAt> toParamAt;
    private Function<ParamAt, ParamAt> beforeSave;
}
