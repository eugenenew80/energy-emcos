package energy.emcos.services.emcos;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class PointConfig {
    private String pointCode;
    private String paramCode;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
