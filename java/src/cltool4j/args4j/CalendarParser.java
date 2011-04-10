package cltool4j.args4j;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * {@link ArgumentParser} for {@link Calendar}s. Parses a number of standard (and not-so-standard) date and
 * time formats, listed below in {@link SimpleDateFormat} syntax:<br/>
 * <br/>
 * 
 * <b>Dot-separated</b>
 * <ul>
 * <li>yyyy.MM.dd.HH.mm.ss.SSS.ZZZ</li>
 * <li>yyyy.MM.dd.HH.mm.ss.SSS</li>
 * <li>yyyy.MM.dd.HH.mm.ss</li>
 * <li>yyyy.MM.dd.HH.mm</li>
 * <li>yyyy.MM.dd.HH.mm</li>
 * </ul>
 * 
 * <b>Dot- and colon-separated, with time</b>
 * <ul>
 * <li>yyyy.MM.dd HH:mm:ss.SSS ZZZ</li>
 * <li>yyyy.MM.dd HH:mm:ss.SSS</li>
 * <li>yyyy.MM.dd HH:mm:ss</li>
 * <li>yyyy.MM.dd HH:mm</li>
 * <li>MM.dd.yyyy HH:mm:ss</li>
 * <li>MM.dd.yy HH:mm:ss</li>
 * <li>MM.dd HH:mm:ss</li>
 * <li>MM.dd.yyyy HH:mm</li>
 * <li>MM.dd.yy HH:mm</li>
 * <li>MM.dd HH:mm</li>
 * </ul>
 * 
 * <b>Dot-separated, without time</b>
 * <ul>
 * <li>yyyy.MM.dd</li>
 * <li>MM.dd.yyyy</li>
 * <li>MM.dd.yy</li>
 * <li>MM.dd</li>
 * </ul>
 * 
 * <b>Slash-separated, with time</b>
 * <ul>
 * <li>MM/dd/yyyy HH:mm:ss</li>
 * <li>MM/dd/yy HH:mm:ss</li>
 * <li>MM/dd HH:mm:ss</li>
 * <li>MM/dd/yyyy HH:mm</li>
 * <li>MM/dd/yy HH:mm</li>
 * <li>MM/dd HH:mm</li>
 * </ul>
 * 
 * <b>Slash-separated, without time</b>
 * <ul>
 * <li>yyyy/MM/dd</li>
 * <li>MM/dd/yyyy</li>
 * <li>MM/dd/yy</li>
 * <li>MM/dd</li>
 * </ul>
 * 
 * @author Aaron Dunlop
 * 
 */
public class CalendarParser extends ArgumentParser<Calendar> {

    protected final static SimpleDateFormat COMMANDLINE_DATE_FORMATS[] = new SimpleDateFormat[] {
            // Dot-separated, with time
            new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS.ZZZ"),
            new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss.SSS"),
            new SimpleDateFormat("yyyy.MM.dd.HH.mm.ss"),
            new SimpleDateFormat("yyyy.MM.dd.HH.mm"),
            new SimpleDateFormat("yyyy.MM.dd.HH.mm"),

            // Dot- and colon-separated, with time
            new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS ZZZ"),
            new SimpleDateFormat("yyyy.MM.dd HH:mm:ss.SSS"), new SimpleDateFormat("yyyy.MM.dd HH:mm:ss"),
            new SimpleDateFormat("yyyy.MM.dd HH:mm"),

            new SimpleDateFormat("MM.dd.yyyy HH:mm:ss"),
            new SimpleDateFormat("MM.dd.yy HH:mm:ss"),
            new SimpleDateFormat("MM.dd HH:mm:ss"),
            new SimpleDateFormat("MM.dd.yyyy HH:mm"),
            new SimpleDateFormat("MM.dd.yy HH:mm"),
            new SimpleDateFormat("MM.dd HH:mm"),

            // Dot-separated, without time
            new SimpleDateFormat("yyyy.MM.dd"),
            new SimpleDateFormat("MM.dd.yyyy"),
            new SimpleDateFormat("MM.dd.yy"),
            new SimpleDateFormat("MM.dd"),

            // Slash-separated, with time
            new SimpleDateFormat("MM/dd/yyyy HH:mm:ss"), new SimpleDateFormat("MM/dd/yy HH:mm:ss"),
            new SimpleDateFormat("MM/dd HH:mm:ss"), new SimpleDateFormat("MM/dd/yyyy HH:mm"),
            new SimpleDateFormat("MM/dd/yy HH:mm"), new SimpleDateFormat("MM/dd HH:mm"),

            // Slash-separated, without time
            new SimpleDateFormat("yyyy/MM/dd"), new SimpleDateFormat("MM/dd/yyyy"),
            new SimpleDateFormat("MM/dd/yy"), new SimpleDateFormat("MM/dd") };

    static {
        for (final SimpleDateFormat formatter : COMMANDLINE_DATE_FORMATS) {
            formatter.setLenient(false);
        }
    }

    public static Calendar parseDate(final String arg) {
        final Calendar c = Calendar.getInstance();
        final int year = c.get(Calendar.YEAR);

        for (final SimpleDateFormat dateFormat : COMMANDLINE_DATE_FORMATS) {
            try {
                final Date d = dateFormat.parse(arg);
                c.setTime(d);
                if (c.get(Calendar.YEAR) == 1970) {
                    c.set(Calendar.YEAR, year);
                }
                return c;
            } catch (final java.text.ParseException ignore) {
            }
        }

        throw new IllegalArgumentException();
    }

    @Override
    public Calendar parse(final String arg) throws IllegalArgumentException {
        return parseDate(arg);
    }

}
