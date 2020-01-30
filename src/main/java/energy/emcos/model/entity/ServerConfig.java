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
@Table(name = "emcos_servers")
public class ServerConfig {

    @Id
    @SequenceGenerator(name="emcos_servers_s", sequenceName = "emcos_servers_s", allocationSize = 1)
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "emcos_servers_s")
    private Long id;

    @NotNull
    @Size(max = 100)
    @Column
    private String name;

    @NotNull
    @Size(max = 300)
    @Column
    private String url;

    @Size(max = 30)
    @Column(name = "user_name")
    private String userName;

    @Size(max = 30)
    @Column
    private String pwd;

    @NotNull
    @Size(max = 30)
    @Column(name = "time_zone")
    private String timeZone;

    @NotNull
    @Column(name = "arc_depth")
    private Long arcDepth;

    @Column(name="created_date")
    private LocalDateTime createdDate;

    @Column(name="last_updated_date")
    private LocalDateTime lastUpdatedDate;

    @Column(name="created_by")
    private Long createdBy;

    @Column(name="last_updated_by")
    private Long lastUpdatedBy;
}
