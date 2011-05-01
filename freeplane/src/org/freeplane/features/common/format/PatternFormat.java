package org.freeplane.features.common.format;

import java.util.Formatter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.lang.time.FastDateFormat;
import org.freeplane.core.util.LogUtils;
import org.freeplane.core.util.TypeReference;
import org.freeplane.n3.nanoxml.XMLElement;

/** a thin wrapper around {@link FastDateFormat} and {@link Formatter}.
 * <p>
 * Parsing is not supported! */
public abstract class PatternFormat /*extends Format*/ {
	private static final String SERIALIZATION_SEPARATOR = ":";
	private static final long serialVersionUID = 1L;

	private static class IdentityPatternFormat extends PatternFormat {

		public IdentityPatternFormat() {
			super("", TYPE_ANY);
		}

		@Override
		public String getStyle() {
			return STYLE_FORMATTER;
		}

		@Override
		public Object formatObject(Object toFormat) {
			return toFormat;
		}
	};

	private static final PatternFormat IDENTITY = new IdentityPatternFormat();
	static final String STYLE_FORMATTER = "formatter";
	static final String STYLE_DATE = "date";
	static final String STYLE_DECIMAL = "decimal";
	static final String TYPE_NUMBER = "number";
	static final String TYPE_DATE = "date";
	static final String TYPE_STRING = "string";
	private static final String TYPE_ANY = "any";
	private static final String ELEMENT_NAME = "format";
	private final String type;
	private final String pattern;
	private String name;

	public PatternFormat(String pattern, String type) {
		this.type = type;
		this.pattern = pattern;
	}

	/** the formal format description. */
	public String getPattern() {
		return pattern;
	}

	/** selects the kind of data the formatter is intended to format. */
	public String getType() {
		return type;
	}

	public String getName() {
		return name;
    }
	
	public void setName(final String name) {
		this.name = name;
	}

	/** selects the formatter implementation, e.g. "formatter" or "date" */
	public abstract String getStyle();
	
	public static PatternFormat createPatternFormat(final String pattern, final String style, final String type) {
		if (style.equals(STYLE_DATE))
			return new DatePatternFormat(pattern);
		else if (style.equals(STYLE_FORMATTER))
			return new FormatterPatternFormat(pattern, type);
		else if (style.equals(STYLE_DECIMAL))
			return new DecimalPatternFormat(pattern);
		else
			throw new IllegalArgumentException("unknown format style");
	}
	
	public static PatternFormat createPatternFormat(final String pattern, final String style, final String type, final String name) {
		final PatternFormat format = createPatternFormat(pattern, style, type);
		format.setName(name);
		return format;
	}

	// yyyy-MM-dd HH:mm:ss
	final static Pattern datePattern = Pattern.compile("yyyy");

	// %[argument_index$] [flags] [width] conversion
	// == conversions
	// ignore boolean: bB
	// ignore hash: hH
	// sS
	// ignore char: cC
	// number: doxXeEfgGaA
	// ignore literals: %n
	// time prefix: tT
	final static Pattern formatterPattern = Pattern.compile("%" //
		// + "(?:[\\d<]+\\$)?" // Freeplane: no support for argument index$!
		+ "(?:[-#+ 0,(]+)?" // flags
		+ "(?:[\\d.]+)?" // width
		+ "([sSdoxXeEfgGaA]|[tT][HIklMSLNpzZsQBbhAaCYyjmdeRTrDFc])"); // conversion

	public static PatternFormat guessPatternFormat(final String pattern) {
		try {
			final Matcher matcher = formatterPattern.matcher(pattern);
			if (matcher.find()) {
				// System.err.println("pattern='" + pattern + "' match='" + matcher.group() + "'");
				final char conversion = matcher.group(1).charAt(0);
				if (matcher.find()) {
					LogUtils.warn("found multiple formats in this formatter pattern: '" + pattern + "'");
					return null;
				}
				switch (conversion) {
					case 's':
					case 'S':
						return new FormatterPatternFormat(pattern, TYPE_STRING);
					case 'd':
					case 'o':
					case 'x':
					case 'X':
					case 'e':
					case 'E':
					case 'f':
					case 'g':
					case 'G':
					case 'a':
					case 'A':
						return new FormatterPatternFormat(pattern, TYPE_NUMBER);
					case 't':
					case 'T':
						return new FormatterPatternFormat(pattern, TYPE_DATE);
				}
			}
			if (datePattern.matcher(pattern).find()) {
				return new DatePatternFormat(pattern);
			}
			if (pattern.indexOf('#') != -1 || pattern.indexOf('0') != -1) {
				return new DecimalPatternFormat(pattern);
			}
			LogUtils.warn("not a pattern format: '" + pattern + "'");
			return null;
		}
		catch (Exception e) {
			LogUtils.warn("can't build a formatter for this pattern '" + pattern + "'", e);
			return null;
		}
	}

	public static PatternFormat getIdentityPatternFormat() {
	    return IDENTITY;
    }

	public void toXml(XMLElement element) {
		final XMLElement child = new XMLElement();
		child.setName(ELEMENT_NAME);
		child.setAttribute("type", getType());
		child.setAttribute("style", getStyle());
		if (getName() != null)
			child.setAttribute("name", getName());
		child.setContent(getPattern());
		element.addChild(child);
	}

	public String serialize() {
		return getType() + SERIALIZATION_SEPARATOR +  getStyle() + SERIALIZATION_SEPARATOR + TypeReference.encode(getPattern()); 
    }

	public static PatternFormat deserialize(String string) {
		final String[] tokens = string.split(SERIALIZATION_SEPARATOR, 3);
	    return createPatternFormat(TypeReference.decode(tokens[2]), tokens[1], tokens[0]);
    }

	public boolean acceptsDate() {
	    return getType().equals(TYPE_DATE);
    }
	
	public boolean acceptsNumber() {
		return getType().equals(TYPE_NUMBER);
	}
	
	public boolean acceptsString() {
		return getType().equals(TYPE_STRING);
	}

	abstract public Object formatObject(Object toFormat);

	@Override
    public int hashCode() {
		final String style = getStyle();
	    final int prime = 31;
	    int result = 1;
	    result = prime * result + ((pattern == null) ? 0 : pattern.hashCode());
	    result = prime * result + ((style == null) ? 0 : style.hashCode());
	    result = prime * result + ((type == null) ? 0 : type.hashCode());
	    return result;
    }

	@Override
    public boolean equals(Object obj) {
	    if (this == obj)
		    return true;
	    if (obj == null)
		    return false;
	    if (getClass() != obj.getClass())
		    return false;
	    PatternFormat other = (PatternFormat) obj;
	    if (pattern == null) {
		    if (other.pattern != null)
			    return false;
	    }
	    else if (!pattern.equals(other.pattern))
		    return false;
		final String style = getStyle();
	    if (style == null) {
		    if (other.getStyle() != null)
			    return false;
	    }
	    else if (!style.equals(other.getStyle()))
		    return false;
	    if (type == null) {
		    if (other.type != null)
			    return false;
	    }
	    else if (!type.equals(other.type))
		    return false;
	    return true;
    }
}
