package energy.emcos.model.repo;

import energy.emcos.model.entity.PointPt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import javax.transaction.Transactional;

@Repository
@Transactional
public interface PointPtRepo extends JpaRepository<PointPt, String> { }
