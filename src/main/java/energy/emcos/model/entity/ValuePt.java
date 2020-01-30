package energy.emcos.model.entity;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import javax.persistence.*;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(of= {"id"})
@NoArgsConstructor
@Entity
@Table(name = "emcos_pt_values")
public class ValuePt {

    @Id
    @SequenceGenerator(name="emcos_pt_values_s", sequenceName = "emcos_pt_values_s", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "emcos_pt_values_s")
    private Long id;

    @NotNull
    @Size(max = 18)
    @Column(name = "point_code")
    private String pointCode;

    @NotNull
    @Size(max = 4)
    @Column(name = "param_code")
    private String paramCode;

    @Column(name="metering_date")
    private LocalDateTime meteringDate;

    @NotNull
    @Column
    private Double val;

    @Column(name="created_date")
    private LocalDateTime createdDate;

    @Column(name="last_updated_date")
    private LocalDateTime lastUpdatedDate;
}
