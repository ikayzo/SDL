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

import java.io.Serializable;

/**
 * This class represents a period of time (duration) as apposed to a particular
 * moment in time (which would be represented using a Date or Calendar instance)
 * 
 * @author Daniel Leuck
 */
public class SDLTimeSpan implements Serializable {

	private static final long serialVersionUID = -2348381600242718399L;
	
	private static final long MILLISECONDS_IN_DAY = 24*60*60*1000;
	private static final long MILLISECONDS_IN_HOUR = 60*60*1000;
	private static final long MILLISECONDS_IN_MINUTE = 60*1000;
	private static final long MILLISECONDS_IN_SECOND = 1000;
	
	private long milliseconds;
	
	/**
	 * Create an sdl time span.  Note: if the timespan is negative all
	 * components should be negative.
	 * 
	 * @param days
	 * @param hours
	 * @param minutes
	 * @param seconds
	 * @param milliseconds
	 */
	public SDLTimeSpan(int days, int hours, int minutes, int seconds,
			int milliseconds) {
		
		this.milliseconds=
			((long)days)*MILLISECONDS_IN_DAY +
			hours*MILLISECONDS_IN_HOUR +
			minutes*MILLISECONDS_IN_MINUTE +
			seconds*MILLISECONDS_IN_SECOND +
			milliseconds;
	}
	
	/**
	 * Create an sdl time span using the total number of milliseconds in the
	 * span.
	 * 
	 * @param totalMilliseconds
	 */
	public SDLTimeSpan(long totalMilliseconds) {
		milliseconds=totalMilliseconds;
	}	
	
	/**
	 * The days component.
	 * 
	 * @return The days component
	 */
	public int getDays() {
		return (int)(((long)milliseconds)/MILLISECONDS_IN_DAY);
	}
	
	/**
	 * The hours component.
	 * 
	 * @return The hours component
	 */
	public int getHours() {	
		long millis = milliseconds-(((long)getDays())*MILLISECONDS_IN_DAY);
		return (int)(millis/MILLISECONDS_IN_HOUR);
	}	
	
	/**
	 * The minutes component.
	 * 
	 * @return The hours component
	 */
	public int getMinutes() {
		long millis = milliseconds-(((long)getDays())*MILLISECONDS_IN_DAY);
		millis=millis-(getHours()*MILLISECONDS_IN_HOUR);
		
		return (int)(millis/MILLISECONDS_IN_MINUTE);
	}
	
	/**
	 * The seconds component.
	 * 
	 * @return The seconds component
	 */
	public int getSeconds() {
		long millis = milliseconds-(((long)getDays())*MILLISECONDS_IN_DAY);
		millis-=(getHours()*MILLISECONDS_IN_HOUR);
		millis-=(getMinutes()*MILLISECONDS_IN_MINUTE);
		
		return (int)(millis/MILLISECONDS_IN_SECOND);
	}
	
	/**
	 * The milliseconds component.
	 * 
	 * @return The milliseconds component
	 */
	public int getMilliseconds() {
		long millis = milliseconds-(((long)getDays())*MILLISECONDS_IN_DAY);
		millis-=(getHours()*MILLISECONDS_IN_HOUR);
		millis-=(getMinutes()*MILLISECONDS_IN_MINUTE);		
		millis-=(getSeconds()*MILLISECONDS_IN_SECOND);	
		
		return (int)millis;
	}

	/**
	 * Get the total number of hours in this time span.  For example, if
	 * this time span represents two days, this method will return 48.
	 * 
	 * @return This timespan in hours
	 */
	public long getTotalHours() {
		return milliseconds/MILLISECONDS_IN_HOUR;
	}	
	
	/**
	 * Get the total number of minutes in this time span.  For example, if
	 * this time span represents two hours, this method will return 120.
	 * 
	 * @return This timespan in minutes
	 */
	public long getTotalMinutes() {
		return milliseconds/MILLISECONDS_IN_MINUTE;
	}	
	
	/**
	 * Get the total number of seconds in this time span.  For example, if
	 * this time span represents three minutes, this method will return 180.
	 * 
	 * @return This timespan in seconds
	 */
	public long getTotalSeconds() {
		return milliseconds/MILLISECONDS_IN_SECOND;
	}	
	
	/**
	 * Get the total number of milliseconds in this time span.  For example, if
	 * this time span represents 4 seconds, this method will return 4000.
	 * 
	 * @return This timespan in milliseconds
	 */
	public long getTotalMilliseconds() {
		return milliseconds;
	}	
	
	/**
	 * Returns an new SDLTimeSpane instance that is the inverse of this
	 * instance
	 * 
	 * @return An instance representing the inverse of this instance
	 */
	public SDLTimeSpan negate() {
		return new SDLTimeSpan(milliseconds*-1L);
	}
	
	/**
	 * Return a new instance with the days adjusted by the given amount.
	 * Positive numbers add days.  Negative numbers remove days.
	 * 
	 * @param days The adjustment (days to add or subtract)
	 * @return A new instance with the given adjustment.
	 */
	public SDLTimeSpan rollDays(long days) {
		return new SDLTimeSpan(milliseconds +(days*MILLISECONDS_IN_DAY));
	}
	
	/**
	 * Return a new instance with the hours adjusted by the given amount.
	 * Positive numbers add hours.  Negative numbers remove hours.
	 * 
	 * @param hours The adjustment (hours to add or subtract)
	 * @return A new instance with the given adjustment.
	 */
	public SDLTimeSpan rollHours(long hours) {
		return new SDLTimeSpan(milliseconds +(hours*MILLISECONDS_IN_HOUR));
	}
	
	/**
	 * Return a new instance with the minutes adjusted by the given amount.
	 * Positive numbers add minutes.  Negative numbers remove minutes.
	 * 
	 * @param minutes The adjustment (minutes to add or subtract)
	 * @return A new instance with the given adjustment.
	 */
	public SDLTimeSpan rollMinutes(long minutes) {
		return new SDLTimeSpan(milliseconds +(minutes*MILLISECONDS_IN_MINUTE));
	}
	
	/**
	 * Return a new instance with the seconds adjusted by the given amount.
	 * Positive numbers add seconds.  Negative numbers remove seconds.
	 * 
	 * @param seconds The adjustment (seconds to add or subtract)
	 * @return A new instance with the given adjustment.
	 */
	public SDLTimeSpan rollSeconds(long seconds) {
		return new SDLTimeSpan(milliseconds + (seconds*MILLISECONDS_IN_SECOND));
	}
	
	/**
	 * Return a new instance with the milliseconds adjusted by the given amount.
	 * Positive numbers add milliseconds.  Negative numbers remove milliseconds.
	 * 
	 * @param milliseconds The adjustment (milliseconds to add or subtract)
	 * @return A new instance with the given adjustment.
	 */
	public SDLTimeSpan rollMilliseconds(long milliseconds) {
		return new SDLTimeSpan(this.milliseconds + milliseconds);
	}
	
	/**
	 * A hashcode based on the canonical string representation.
	 * 
	 * @return A hashcode for this time span
	 */
	public int hashCode() {
		return toString().hashCode();
	}
	
	/**
	 * Tests for equivalence.
	 * 
	 * @return true If the given object is equivalent
	 */
	public boolean equals(Object other) {
		return other instanceof SDLTimeSpan && ((SDLTimeSpan)other)
			.getTotalMilliseconds()==milliseconds;
	}
	
	/**
	 * <p>Returns an SDL representation of this time span using the format:<p>
	 * 
	 * <pre>
	 * (days:)hours:minutes:seconds(.milliseconds)
	 * </pre>
	 * 
	 * <p>(parenthesis indicate optional components)
	 * 
	 * <p>The days and milliseconds components will not be included if they 
	 * are set to 0.  Days must be suffixed with "d" for clarity.</p>
	 * 
	 * <p>Hours, minutes, and seconds will be zero paded to two characters.</p>
	 * 
	 * <p>Examples:</p>
	 * <pre>
	 *     23:13:00 (12 hours and 13 seconds)
	 *     24d:12:13:09.234 (24 days, 12 hours, 13 minutes, 9 seconds,
	 *         234 milliseconds)
	 * </pre>
	 * 
	 * @return an SDL representation of this time span
	 */
	public String toString() {
		StringBuilder sb=new StringBuilder();
		
		int days = getDays();
		int hours = getHours();
		int minutes = getMinutes();
		int seconds = getSeconds();
		int milliseconds = getMilliseconds();

		if(days!=0) {
			sb.append(days);
			sb.append("d");
			sb.append(":");
			
			sb.append(padTo2((int)Math.abs(hours)));
		} else {		
			sb.append(padTo2(hours));
		}
		
		sb.append(":");
		
		sb.append(padTo2((int)Math.abs(minutes)));
		sb.append(":");
		
		sb.append(padTo2((int)Math.abs(seconds)));
		
		if(milliseconds!=0) {
			sb.append(".");	
			
			String millis = "" + (int)Math.abs(milliseconds);
			if(millis.length()==1)
				millis="00"+millis;
			else if(millis.length()==2)
				millis="0"+millis;
			
			sb.append(millis);
		}
		
		return sb.toString();
	}
	
	private String padTo2(int val) {
		if(val>-10 && val<0) {
			return "-0" + (int)Math.abs(val);
		} else if(val>-1 && val<10) {
			return "0" + val;
		}
		
		return "" + val;
	}
}
