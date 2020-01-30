package energy.emcos.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PointAtParamId implements Serializable {
    private String pointCode;
    private String paramCode;
}
