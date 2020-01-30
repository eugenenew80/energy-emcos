package energy.emcos.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.DynamicUpdate;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of= {"pointCode, paramCode"})
@NoArgsConstructor
@Entity
@Table(name = "emcos_pt_point_params")
@IdClass(PointPtParamId.class)
@DynamicUpdate
public class PointPtParam implements Serializable {
    public PointPtParam(@NotNull String pointCode, @NotNull String paramCode) {
        this.pointCode = pointCode;
        this.paramCode = paramCode;
    }

    @NotNull
    @Id
    @Column(name = "point_code")
    private String pointCode;

    @NotNull
    @Id
    @Column(name = "param_code")
    private String paramCode;

    @Column(name="last_load_date")
    private LocalDateTime lastLoadDate;

    @Column(name="last_metering_date")
    private LocalDateTime lastMeteringDate;
}
