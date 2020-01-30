package energy.emcos.schedule;

import energy.emcos.services.emcos.ValueAtRequestService;
import energy.emcos.services.emcos.ValuePtRequestService;
import energy.emcos.model.entity.*;
import energy.emcos.model.repo.ServerConfigRepo;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AutoLoader {
    private static final Logger logger = LoggerFactory.getLogger(AutoLoader.class);
    private final ValueAtRequestService valueAtRequestService;
    private final ValuePtRequestService valuePtRequestService;
    private final ServerConfigRepo configRepo;

    @Scheduled(cron = "0 */1 * * * *")
    public void run() {
        List<ServerConfig> configs = configRepo.findAll();
        for (ServerConfig config : configs) {
            try {
                valueAtRequestService.load(config);
            }
            catch (Exception e) {
                e.printStackTrace();
            }

            try {
                valuePtRequestService.load(config);
            }
            catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
