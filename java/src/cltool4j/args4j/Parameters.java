package cltool4j.args4j;

import java.util.NoSuchElementException;

/**
 * A bidirectional iterator over a {@link String} array. Allows lookahead and forward or backward iteration.
 */
class Parameters {

    private final String[] args;
    private int pos;

    Parameters(final String[] args) {
        this.args = args;
        pos = 0;
    }

    protected boolean hasNext() {
        return pos < args.length;
    }

    public String peek() {
        if (pos >= args.length) {
            throw new NoSuchElementException();
        }
        return args[pos];
    }

    public String next() {
        if (pos >= args.length) {
            throw new NoSuchElementException();
        }
        return args[pos++];
    }

    public String previous() {
        if (pos == 0) {
            throw new NoSuchElementException();
        }
        return args[--pos];
    }

    public String current() {
        if (pos == 0) {
            throw new NoSuchElementException();
        }
        return args[pos - 1];
    }
}