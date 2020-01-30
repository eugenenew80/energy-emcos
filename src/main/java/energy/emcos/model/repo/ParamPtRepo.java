package energy.emcos.model.repo;

import energy.emcos.model.entity.ParamPt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import javax.transaction.Transactional;

@Repository
@Transactional
public interface ParamPtRepo extends JpaRepository<ParamPt, String> { }
