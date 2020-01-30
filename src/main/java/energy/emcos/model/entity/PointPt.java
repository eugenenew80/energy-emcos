package energy.emcos.model.entity;

import lombok.*;
import javax.persistence.*;
import javax.validation.constraints.*;

@Data
@EqualsAndHashCode(of= {"code"})
@NoArgsConstructor
@Entity
@Table(name = "emcos_pt_points")
public class PointPt {
    @NotNull
    @Size(max = 18)
    @Id
    private String code;
}
