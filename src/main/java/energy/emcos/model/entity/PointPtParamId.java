package energy.emcos.model.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class PointPtParamId implements Serializable {
    private String pointCode;
    private String paramCode;
}
