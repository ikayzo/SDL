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

/**
 * An exception describing a problem with an SDL document's structure
 * 
 * @author Daniel Leuck
 */
@SuppressWarnings("serial")
public class SDLParseException extends Exception {

	private int line;
	private int position;
	
	/**
	 * Note: Line and positioning numbering start with 1 rather than 0 to be
	 * consistent with most editors.
	 * 
	 * @param description A description of the problem.
	 * @param line The line on which the error occured or -1 for unknown
	 * @param position The position (within the line) where the error occured or
	 *        -1 for unknown
	 */
	public SDLParseException(String description, int line, int position) {
		super(description + " Line " + ((line==-1) ? "unknown" : (""+line)) +
				", Position " + ((position==-1) ? "unknown" : (""+position)));
		this.line = line;
		this.position = position;
	}	
	
	/**
	 * @return Returns the line on which the error occured
	 */
	public int getLine() {
		return line;
	}

	/**
	 * @return Returns the character position within the line where the error
	 *         first occured.
	 */
	public int getPosition() {
		return position;
	}
	
	/**
	 * @return getMessage()
	 */
	public String toString() {
		return getMessage();
	}
}
