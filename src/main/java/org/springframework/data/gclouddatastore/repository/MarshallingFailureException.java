package org.springframework.data.gclouddatastore.repository;

import org.springframework.dao.DataAccessException;
import org.springframework.lang.Nullable;

public class MarshallingFailureException extends DataAccessException {
    public MarshallingFailureException(String msg) {
        super(msg);
    }

    public MarshallingFailureException(@Nullable String msg, @Nullable Throwable cause) {
        super(msg, cause);
    }
}
