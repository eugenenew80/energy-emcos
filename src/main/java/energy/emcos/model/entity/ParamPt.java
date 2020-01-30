package energy.emcos.model.entity;

import lombok.*;
import javax.persistence.*;
import javax.validation.constraints.*;

@Data
@EqualsAndHashCode(of= {"code"})
@NoArgsConstructor
@Entity
@Table(name = "emcos_pt_params")
public class ParamPt {
    @NotNull
    @Size(max = 4)
    @Id
    private String code;
}
