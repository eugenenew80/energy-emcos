package energy.emcos.model.util;

import javax.persistence.PreUpdate;

public class PreventUpdate {
    @PreUpdate
    void onPreUpdate(Object o) {
        throw new UnsupportedOperationException("Unsupported operation");
    }
}
