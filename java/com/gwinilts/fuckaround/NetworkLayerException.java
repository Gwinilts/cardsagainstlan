package com.gwinilts.fuckaround;

import java.lang.Exception;

public class NetworkLayerException extends Exception {
    public enum Kind {
        BCAST,
        SPEAK,
        LISTEN,
        VERB
    }

    private Kind kind;

    public NetworkLayerException(Kind k) {
        kind = k;
    }

    public Kind getKind() {
        return this.kind;
    }
}
