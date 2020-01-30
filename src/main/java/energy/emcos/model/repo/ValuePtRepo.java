package energy.emcos.model.repo;

import energy.emcos.model.entity.ValuePt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@Transactional
public interface ValuePtRepo extends JpaRepository<ValuePt, String> {

    @Query("select t from ValuePt t where t.pointCode = ?1 and t.paramCode = ?2 and t.meteringDate = ?3")
    Optional<ValuePt> findOne(String point, String param, LocalDateTime date);
}
