package energy.emcos.model.util;

import javax.persistence.PrePersist;
import javax.persistence.PreRemove;
import javax.persistence.PreUpdate;

public class PreventAnyUpdate {
    @PrePersist
    void onPrePersist(Object o) {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @PreUpdate
    void onPreUpdate(Object o) {
        throw new UnsupportedOperationException("Unsupported operation");
    }

    @PreRemove
    void onPreRemove(Object o) {
        throw new UnsupportedOperationException("Unsupported operation");
    }
}
