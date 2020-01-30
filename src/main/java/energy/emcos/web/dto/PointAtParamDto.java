package energy.emcos.web.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PointAtParamDto {
    private String paramCode;
    private LocalDateTime lastMeteringDate;
}
