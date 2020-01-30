package energy.emcos.model.repo;

import energy.emcos.model.entity.PointPtParam;
import energy.emcos.model.entity.PointPtParamId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import javax.transaction.Transactional;
import java.util.List;

@Repository
@Transactional
public interface PointPtParamRepo extends JpaRepository<PointPtParam, PointPtParamId> {
    List<PointPtParam> findAllByPointCode(String pointCode);
}
