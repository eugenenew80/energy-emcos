package energy.emcos.model.repo;

import energy.emcos.model.entity.PointAtParam;
import energy.emcos.model.entity.PointAtParamId;
import energy.emcos.model.entity.ValueAt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
@Transactional
public interface PointAtParamRepo extends JpaRepository<PointAtParam, PointAtParamId> {
    List<PointAtParam> findAllByPointCode(String pointCode);
}
