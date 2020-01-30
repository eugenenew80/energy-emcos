package energy.emcos.model.repo;

import energy.emcos.model.entity.ValueAt;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import javax.transaction.Transactional;
import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@Transactional
public interface ValueAtRepo extends JpaRepository<ValueAt, Long> {

    @Query("select t from ValueAt t where t.pointCode = ?1 and t.paramCode = ?2 and t.meteringDate = ?3")
    Optional<ValueAt> findOne(String point, String param, LocalDateTime date);
}
