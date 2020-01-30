package energy.emcos.services.emcos;

import energy.emcos.model.entity.ServerConfig;

public interface ValueRequestService {
    void load(ServerConfig config) throws Exception;
}
