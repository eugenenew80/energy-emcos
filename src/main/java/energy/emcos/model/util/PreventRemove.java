package energy.emcos.model.util;

import javax.persistence.PreRemove;

public class PreventRemove {
    @PreRemove
    void onPreRemove(Object o) {
        throw new UnsupportedOperationException("Unsupported operation");
    }
}
