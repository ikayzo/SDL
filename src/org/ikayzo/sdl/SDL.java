/*
 * Simple Declarative Language (SDL) for Java
 * Copyright 2005 Ikayzo, inc.
 *
 * This program is free software. You can distribute or modify it under the 
 * terms of the GNU Lesser General Public License version 2.1 as published by  
 * the Free Software Foundation.
 *
 * This program is distributed AS IS and WITHOUT WARRANTY. OF ANY KIND,
 * INCLUDING MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, contact the Free Software Foundation, Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 */
package org.ikayzo.sdl;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.SortedMap;
import org.ikayzo.codec.Base64;

/**
 * Various SDL related utility methods
 * 
 * @author Daniel Leuck
 */
public class SDL {
	
	/**
	 * <p>The SDL standard time format HH:mm:ss.SSS-z</p>
	 * 
	 * <p>Note: SDL uses a 24 hour clock (0-23)</p>
	 * <p>Note: This is not the same as a time span.   This format is used
	 *          for the time component of a date_time instance</p>
	 */
	public static final String TIME_FORMAT = "HH:mm:ss.SSS-z";

	/**
	 * <p>The SDL standard date format yyyy/MM/dd</p>
	 * 
	 * <p>Note: SDL uses the Gregorian calendar</p>
	 */
	public static final String DATE_FORMAT = "yyyy/MM/dd";
	
	/**
	 * The SDL standard DATE_TIME format yyyy/MM/dd HH:mm:ss.SSS-z
	 * 
	 * <p>Note: SDL uses a 24 hour clock (0-23) and the Gregorian calendar
	 */
	public static final String DATE_TIME_FORMAT = DATE_FORMAT + " " +
		TIME_FORMAT;	
	
	/**
	 * Create an SDL string representation for an object (note: Strings and 
	 * Characters will be surrounded by quotes)
	 * 
	 * @param object The object to format
	 * @return an SDL string representation for an object
	 */
	public static String format(Object object) {
		return format(object, true);
	}
	
	/**
	 * Create an SDL string representation for an object
	 * 
	 * @param object The object to format
	 * @param addQuotes Quotes will be added to Strings and Characters if true
	 * @return an SDL string representation for an object
	 */
	public static String format(Object object, boolean addQuotes) {
		
		if(object instanceof String) {
			if(addQuotes)
				return "\"" + escape(String.valueOf(object)) + "\"";
			else
				return escape(String.valueOf(object));
		} else if(object instanceof Character) {
			if(addQuotes) {
				return "'" + escape((Character)object) + "'";
			} else {
				return escape((Character)object);
			}
		} else if(object instanceof BigDecimal) {
			return object.toString() + "BD";
		} else if(object instanceof Float) {
			return object.toString() + "F";
		} else if(object instanceof Long) {
			return object.toString() + "L";
		} else if(object instanceof byte[]) {
			return "[" + Base64.encode((byte[])object) + "]";
		} else if(object instanceof Calendar) {
			Calendar c = (Calendar)object;

			if(c.isSet(Calendar.HOUR_OF_DAY)) {
				
				// if no time components are set, consider this a date only
				// instance
				if(
					c.get(Calendar.HOUR_OF_DAY)==0 &&
					c.get(Calendar.MINUTE)==0 &&
					c.get(Calendar.SECOND)==0 &&
					c.get(Calendar.MILLISECOND)==0
					) {
					SimpleDateFormat sdf = new SimpleDateFormat(
							DATE_FORMAT);	
					return sdf.format(c.getTime());		
				} else {				
					SimpleDateFormat sdf = new SimpleDateFormat(
							DATE_TIME_FORMAT);	
					sdf.setTimeZone(c.getTimeZone());	
					return sdf.format(c.getTime());
				}
			} else {
				SimpleDateFormat sdf = new SimpleDateFormat(
						DATE_FORMAT);	
				return sdf.format(c.getTime());					
			}
		}
		
		// We don't need to worry about time span because SDLTimeSpan outputs
		// proper SDL from its toString() method
		return String.valueOf(object);
	}
	
	private static String escape(String s) {
		StringBuilder sb = new StringBuilder();
		
		int size = s.length();
		for(int i=0; i<size; i++) {
			char c = s.charAt(i);
			if(c=='\\')
				sb.append("\\\\");
			else if(c=='"')
				sb.append("\\\"");
			else if(c=='\t')
				sb.append("\\t");
			else if(c=='\r')
				sb.append("\\r");	
			else if(c=='\n')
				sb.append("\\n");
			else
				sb.append(c);
		}
		
		return sb.toString();
	}
	
	private static String escape(Character ch) {

		char c = ch.charValue();
		switch(c) {
			case '\\': return "\\\\";
			case '\'': return "\\'";
			case '\t': return "\\t";
			case '\r': return "\\r";
			case '\n': return "\\n";
			default: return ""+c;
		}
	}
	
	/**
	 * <p>Coerce the type to a standard SDL type or throw an illegal argument
	 * exception if no coercion is possible.<p>
	 * 
	 * <p>Coercion table</p>
	 * <pre>
	 *     String, Character, Integer, Long, Float, Double, BigDecimal,
	 *         Boolean, Calendar, SDLTimeSpan -> No change
	 *     Byte[] -> byte[]
	 *     Byte, Short -> Integer
	 *     Date -> Calendar
	 * </pre>
	 * 
	 * @param obj An object to coerce
	 * @throws IllegalArgumentException if the type is coercible to a legal SDL
	 *     type
	 */
	@SuppressWarnings("unchecked")
	public static Object coerceOrFail(Object obj) {
		
		if(obj==null)
			return null;
		
		if(obj instanceof String || obj instanceof Double ||
		   obj instanceof Integer || obj instanceof Boolean ||
		   obj instanceof BigDecimal || obj instanceof Long ||
		   obj instanceof Character || obj instanceof Float ||
		   obj instanceof Calendar || obj instanceof SDLTimeSpan) {
			
			return obj;
		}
		
		Class c = obj.getClass();
		if(c.isArray()) {
			Class compType = c.getComponentType();
			
			if(compType==byte.class)
				return obj;
			
			if(compType==Byte.class) {
				Byte[] objBytes = (Byte[])obj;
				byte[] bytes = new byte[objBytes.length];
				for(int i=0;i<objBytes.length;i++)
					bytes[i]=objBytes[i];
				
				return bytes;
			}
		}
		
		if(obj instanceof Date) {
			Calendar cal = new GregorianCalendar();
			cal.setTime((Date)obj);
			return cal;
		}
			
		if(obj instanceof Byte || obj instanceof Short) {
			return ((Number)obj).intValue();
		}
		
		throw new IllegalArgumentException(obj.getClass().getName() + " is " +
				"not coercible to an SDL type");
	}
	
	/**
	 * Validates an SDL identifier String.  SDL Identifiers must start with a
	 * Unicode letter or underscore (_) and contain only unicode letters,
	 * digits, underscores (_), and dashes(-).
	 * 
	 * @param identifier The identifier to validate
	 * @throws IllegalArgumentException if the identifier is not legal
	 */
	public static void validateIdentifier(String identifier) {
		if(identifier==null || identifier.length()==0)
			throw new IllegalArgumentException("SDL identifiers cannot be " +
					"null or empty.");
		
		if(!Character.isJavaIdentifierStart(identifier.charAt(0)))
			throw new IllegalArgumentException("'" + identifier.charAt(0) + 
					"' is not a legal first character for an SDL identifier. " +
					"SDL Identifiers must start with a unicode letter or " +
					"an underscore (_).");	
		
		int identifierSize=identifier.length();
		for(int i=1; i<identifierSize; i++)
			if(!Character.isJavaIdentifierPart(identifier.charAt(i)) && 
					identifier.charAt(i)!='-')
				throw new IllegalArgumentException("'" + identifier.charAt(i) + 
						"' is not a legal character for an SDL identifier. " +
						"SDL Identifiers must start with a unicode letter or " +
						"underscore (_) followed by 0 or more unicode " +
						"letters, digits, underscores (_), or dashes (-)");					
	}

	/**
	 * Get the value represented by a string containing an SDL literal.
	 * 
	 * @param literal The literal string to parse
	 * @return A value for an SDL literal
	 * @throws IllegalArgumentException If the text is null or the text does not
	 *         represent a valid SDL literal
	 * @throws NumberFormatException If the text represents a malformed number.
	 */
	public static Object value(String literal) {
		if(literal==null)
			throw new IllegalArgumentException("literal argument to " +
					"SDL.value(String) cannot be null");
		
		if(literal.startsWith("\"") || literal.startsWith("`"))
			return Parser.parseString(literal);
		if(literal.startsWith("'"))
			return Parser.parseCharacter(literal);
		if(literal.equals("null"))
			return null;
		if(literal.equals("true") || literal.equals("on"))
			return Boolean.TRUE;
		if(literal.equals("false") || literal.equals("off"))
			return Boolean.FALSE;
		if(literal.startsWith("["))
			return Parser.parseBinary(literal);	
		if(literal.charAt(0)!='/' && literal.indexOf('/')!=-1)
			return Parser.parseDateTime(literal);
		if(literal.charAt(0)!=':' && literal.indexOf(':')!=-1)
			return Parser.parseTimeSpan(literal);
		if("01234567890-.".indexOf(literal.charAt(0))!=-1)
			return Parser.parseNumber(literal);	
		
		throw new IllegalArgumentException("String " + literal + " does not " +
				"represent an SDL type.");
	}

	/**
	 * <p>Parse the string of values and return a list.  The string is handled
	 * as if it is the values portion of an SDL tag.</p>
	 * 
	 * <p>Example
	 * <pre>
	 *     List list = SDL.list("1 true 12:24:01");
	 * </pre>
	 * </p>
	 * 
	 * <p>Will return an int, a boolean, and a time span.</p>
	 * 
	 * <p>Note: If you want the more descriptive SDLParseException to be thrown
	 * use:
	 * <pre>
	 *     List list = new Tag("root").read("1 true 12:24:01")
	 *         .getChild("content").getValues(); 
	 * </pre>
	 * </p>
	 * 
	 * @param valueList A string of space separated SDL literals
	 * @return A list of values
	 * @throws IllegalArgumentException If the string is null or contains 
	 *     literals that cannot be parsed
	 */
	public static List list(String valueList) {
		if(valueList==null)
			throw new IllegalArgumentException("valueList argument to " +
					"SDL.list(String) cannot be null");
		
		try {	
			return new Tag("root").read(valueList).getChild("content")
				.getValues();
		} catch(SDLParseException spe) {
			throw new IllegalArgumentException(spe.getMessage());
		}
	}
	
	/**
	 * <p>Parse a string representing the attributes portion of an SDL tag
	 * and return the results as a map.</p>
	 * 
	 * <p>Example
	 * <pre>
	 *     Map<String,Object> m = SDL.map("value=1 debugging=on time=12:24:01");
	 * </pre>
	 * </p>
	 * 
	 * <p>Will return a map containing value=1, debugging=true, and
	 * time=12:24:01</p>
	 * 
	 * <p>Note: If you want the more descriptive SDLParseException to be thrown
	 * use:
	 * <pre>
	 *     Map<String,Object> m = new Tag("root")
	 *         .read("atts " + attributeString).getChild("atts")
	 *         .getAttributes();
	 * </pre>
	 * </p>
	 * 
	 * @param attributeString A string of space separated key=value pairs
	 * @return A map created from the attribute string
	 * @throws IllegalArgumentException If the string is null or contains 
	 *     literals that cannot be parsed or the map is malformed
	 */
	public static SortedMap<String,Object> map(String attributeString) {
		if(attributeString==null)
			throw new IllegalArgumentException("attributeString argument to " +
					"SDL.map(String) cannot be null");
		
		try {	
			return new Tag("root").read("atts " + attributeString)
				.getChild("atts").getAttributes();
		} catch(SDLParseException spe) {
			throw new IllegalArgumentException(spe.getMessage());
		}
	}	
}
