package cltool4j.args4j;

/**
 * Parses a date into a long (seconds since the epoch) using the patterns supported by {@link CalendarParser}.
 * This handler is <em>not</em> registered by default, since it conflicts with the normal parser for
 * {@link java.lang.Long} registered in {@link ArgumentParser}. To parse a timestamp, include
 * <code>parser = cltool4j.args4j.TimestampParser</code> in the {@link Option} / {@link Argument} annotation.
 */
public class TimestampParser extends ArgumentParser<Long> {

    @Override
    public Long parse(final String s) {
        return CalendarParser.parseDate(s.toLowerCase()).getTime().getTime();
    }

    @Override
    public String defaultMetaVar() {
        return "date";
    }
}