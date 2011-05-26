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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.math.BigDecimal;
import java.util.*;

import org.ikayzo.codec.Base64;

/**
 * The SDL parser.
 * 
 * @author Daniel Leuck
 */
class Parser {
	
	private BufferedReader reader;
	private String line;
	private List<Token> toks;
	private StringBuilder sb;
	private boolean startEscapedQuoteLine;
	private int lineNumber=-1, pos=0, lineLength=0, tokenStart=0;
	
	/**
	 * Create an SDL parser
	 */
	Parser(Reader reader) {
		this.reader = (reader instanceof BufferedReader)
			? ((BufferedReader)reader)
			: new BufferedReader(reader);
	}	

	/**
	 * @return A list of tags described by the input
	 * @throws IOException If a problem is encountered with the reader
	 * @throws SDLParseException If the document is malformed
	 */
	List<Tag> parse() throws IOException,
		SDLParseException {
		
		List<Tag> tags = new ArrayList<Tag>();
		List<Token> toks;
		
		while((toks=getLineTokens())!=null) {
			int size = toks.size();
			
			if(toks.get(size-1).type==Type.START_BLOCK) {
				Tag t = constructTag(toks.subList(0, size-1));
				addChildren(t);
				tags.add(t);
			} else if(toks.get(0).type==Type.END_BLOCK){
				parseException("No opening block ({) for close block (}).",
						toks.get(0).line, toks.get(0).position);
			} else {
				tags.add(constructTag(toks));
			}
		}
		
		reader.close();
		
		return tags;
	}
	
	private void addChildren(Tag parent) throws SDLParseException, IOException {
		List<Token> toks;
		while((toks=getLineTokens())!=null) {
			int size = toks.size();
			
			if(toks.get(0).type==Type.END_BLOCK) {
				return;
			} else if(toks.get(size-1).type==Type.START_BLOCK) {
				Tag tag = constructTag(toks.subList(0, size-1));
				addChildren(tag);
				parent.addChild(tag);
			} else {
				parent.addChild(constructTag(toks));
			}
		}		
		
		// we have to use -2 for position rather than -1 for unknown because
		// the parseException method adds 1 to line and position
		parseException("No close block (}).", lineNumber, -2);
	}
	
	/**
	 * Construct a tag (but not its children) from a string of tokens
	 * 
	 * @throws SDLParseException 
	 */
	Tag constructTag(List<Token> toks) throws SDLParseException {
		if(toks.isEmpty())
			// we have to use -2 for position rather than -1 for unknown because
			// the parseException method adds 1 to line and position
			parseException("Internal Error: Empty token list", lineNumber, -2);
			
		Token t0 = toks.get(0);
		
		
		if(t0.literal) {
			toks.add(0, t0=new Token("content", -1, -1));
		} else if(t0.type!=Type.IDENTIFIER) {
			expectingButGot("IDENTIFIER", "" + t0.type + " (" + t0.text + ")",
					t0.line, t0.position);	
		}
		
		int size = toks.size();
		
		Tag tag = null;
		
		if(size==1) {
			tag = new Tag(t0.text);
		} else {
			int valuesStartIndex = 1;
			
			Token t1 = toks.get(1);
			
			if(t1.type==Type.COLON) {
				if(size==2 || toks.get(2).type!=Type.IDENTIFIER)
					parseException("Colon (:) encountered in unexpected " +
							"location.", t1.line, t1.position);
				
				Token t2 = toks.get(2);
				tag = new Tag(t0.text,t2.text);
				
				valuesStartIndex = 3;
			} else {
				tag = new Tag(t0.text);
			}
				
			// read values
			int i =addTagValues(tag, toks, valuesStartIndex);
			
			// read attributes
			if(i<size)
				addTagAttributes(tag, toks, i);
		}

		return tag;
	}
	
	/**
	 * @return The position at the end of the value list
	 */
	private int addTagValues(Tag tag, List<Token> toks, int tpos)
		throws SDLParseException {
		
		int size=toks.size(), i=tpos;
		
		for(;i<size;i++) {
			Token t = toks.get(i);
			if(t.literal) {
				
				// if a DATE token is followed by a TIME token combine them
				if(t.type==Type.DATE && (i+1)<size &&
						toks.get(i+1).type==Type.TIME) {

					Calendar dc = (Calendar)t.getObjectForLiteral();
					TimeSpanWithZone tswz = (TimeSpanWithZone)
						toks.get(i+1).getObjectForLiteral();
					
					if(tswz.getDays()!=0) {
						tag.addValue(dc);
						tag.addValue(new SDLTimeSpan(
								tswz.getDays(), tswz.getHours(),
								tswz.getMinutes(), tswz.getSeconds(),
								tswz.getMilliseconds()
							));
						
						if(tswz.getTimeZone()!=null)
							parseException("TimeSpan cannot have a timezone",
								t.line, t.position);
					} else {
						tag.addValue(combine(dc,tswz));
					}
					
					i++;
				} else {
					Object v = t.getObjectForLiteral();
					if(v instanceof TimeSpanWithZone) {
						TimeSpanWithZone tswz = (TimeSpanWithZone)v;
						
						if(tswz.getTimeZone()!=null)
							expectingButGot("TIME SPAN",
								"TIME (component of date/time)", t.line,
								    t.position);
						
						tag.addValue(new SDLTimeSpan(
							tswz.getDays(), tswz.getHours(),
							tswz.getMinutes(), tswz.getSeconds(),
							tswz.getMilliseconds()
						));
					} else {
						tag.addValue(v);
					}
				}
			} else if(t.type==Type.IDENTIFIER) {
				break;
			} else {
				expectingButGot("LITERAL or IDENTIFIER", t.type, t.line,
						t.position);
			}
		}	
		
		return i;
	}
	
	/**
	 * Add attributes to the given tag
	 */
	private void addTagAttributes(Tag tag, List<Token> toks, int tpos)
		throws SDLParseException {
		
		int i=tpos, size=toks.size();
		
		while(i<size) {
			Token t = toks.get(i);
			if(t.type!=Type.IDENTIFIER)
				expectingButGot("IDENTIFIER", t.type, t.line, t.position);
			String nameOrNamespace = t.text;
			
			if(i==size-1)
				expectingButGot("\":\" or \"=\" \"LITERAL\"", "END OF LINE.",
						t.line, t.position);
			
			t = toks.get(++i);
			if(t.type==Type.COLON) {
				if(i==size-1)
					expectingButGot("IDENTIFIER", "END OF LINE", t.line,
							t.position);
				
				t = toks.get(++i);
				if(t.type!=Type.IDENTIFIER)
					expectingButGot("IDENTIFIER", t.type, t.line,
							t.position);
				String name = t.text;
				
				if(i==size-1)
					expectingButGot("\"=\"", "END OF LINE", t.line,
							t.position);
				t = toks.get(++i);
				if(t.type!=Type.EQUALS)
					expectingButGot("\"=\"", t.type, t.line,
							t.position);
				
				if(i==size-1)
					expectingButGot("LITERAL", "END OF LINE", t.line,
							t.position);
				t = toks.get(++i);
				if(!t.literal)
					expectingButGot("LITERAL", t.type, t.line, t.position);
				
				if(t.type==Type.DATE && (i+1)<size &&
						toks.get(i+1).type==Type.TIME) {
					
					Calendar dc = (Calendar)t.getObjectForLiteral();
					TimeSpanWithZone tswz = (TimeSpanWithZone)
						toks.get(i+1).getObjectForLiteral();
					
					if(tswz.getDays()!=0) {
						expectingButGot("TIME (component of date/time) " +
							"in attribute value", "TIME SPAN", t.line,
							t.position);
						
					} else {
						tag.setAttribute(nameOrNamespace, name,
								combine(dc,tswz));	
					}
					
					i++;
				} else {
					Object v = t.getObjectForLiteral();
					if(v instanceof TimeSpanWithZone) {
						TimeSpanWithZone tswz = (TimeSpanWithZone)v;
						
						if(tswz.getTimeZone()!=null)
							expectingButGot("TIME SPAN",
								"TIME (component of date/time)", t.line,
								    t.position);
						
						SDLTimeSpan ts = new SDLTimeSpan(
							tswz.getDays(), tswz.getHours(),
							tswz.getMinutes(), tswz.getSeconds(),
							tswz.getMilliseconds());
						
						tag.setAttribute(nameOrNamespace, name,
								ts);							
					} else {
						tag.setAttribute(nameOrNamespace, name,
								v);
					}
				}
			} else if(t.type==Type.EQUALS){
				if(i==size-1)
					expectingButGot("LITERAL", "END OF LINE", t.line,
							t.position);	
				t = toks.get(++i);
				if(!t.literal)
					expectingButGot("LITERAL", t.type, t.line, t.position);	
				
				
				if(t.type==Type.DATE && (i+1)<size &&
						toks.get(i+1).type==Type.TIME) {
				
					Calendar dc = (Calendar)t.getObjectForLiteral();
					TimeSpanWithZone tswz = (TimeSpanWithZone)
						toks.get(i+1).getObjectForLiteral();
					
					if(tswz.getDays()!=0)
						expectingButGot("TIME (component of date/time) " +
							"in attribute value", "TIME SPAN", t.line,
							t.position);
					tag.setAttribute(nameOrNamespace, combine(dc,tswz));	
					
					i++;
				} else {
					Object v = t.getObjectForLiteral();
					if(v instanceof TimeSpanWithZone) {
						TimeSpanWithZone tswz = (TimeSpanWithZone)v;
						
						if(tswz.getTimeZone()!=null)
							expectingButGot("TIME SPAN",
								"TIME (component of date/time)", t.line,
								    t.position);
						
						SDLTimeSpan ts = new SDLTimeSpan(
							tswz.getDays(), tswz.getHours(),
							tswz.getMinutes(), tswz.getSeconds(),
							tswz.getMilliseconds());
						
						tag.setAttribute(nameOrNamespace, ts);							
					} else {
						tag.setAttribute(nameOrNamespace, v);
					}			
				}
			} else {
				expectingButGot("\":\" or \"=\"", t.type, t.line,
						t.position);	
			}
			
			i++;
		}
	}

	/**
	 * Get a line as tokens.  This method handles line continuations both
	 * within and outside String literals.
	 * 
	 * @return A logical line as a list of Tokens
	 * @throws SDLParseException
	 * @throws IOException
	 */
	List<Token> getLineTokens() throws SDLParseException, IOException {
		line = readLine();
		if(line==null)
			return null;
		toks = new ArrayList<Token>();
		lineLength = line.length(); 
		sb = null;
		tokenStart=0;	
		
		for(;pos<lineLength; pos++) {
			char c=line.charAt(pos);

			if(sb!=null) {
				toks.add(new Token(sb.toString(), lineNumber, tokenStart));
				sb=null;
			}
			
			if(c=='"') {	
				// handle "" style strings including line continuations
				handleDoubleQuoteString();
			} else if(c=='\'') {	
				// handle character literals				
				handleCharacterLiteral();
			} else if("{}=:".indexOf(c)!=-1) {
				// handle punctuation
				toks.add(new Token(""+c, lineNumber, pos));
				sb=null;
			} else if(c=='#') {	
				// handle hash comments
				break;
			} else if(c=='/') {	
				// handle /**/ and // style comments
				
				if((pos+1)<lineLength &&
						line.charAt(pos+1)=='/')
					break;
				else
					handleSlashComment();	
			} else if(c=='`') {	
				// handle multiline `` style strings				
				handleBackQuoteString();
			} else if(c=='[') {	
				// handle binary literals
				
				handleBinaryLiteral();
			
			} else if(c==' ' || c=='\t') {
				// eat whitespace
				while((pos+1)<lineLength &&
						" \t".indexOf(line.charAt(pos+1))!=-1)
					pos++;	
			} else if(c=='\\') {
				// line continuations (outside a string literal)
				
				// backslash line continuation outside of a String literal
				// can only occur at the end of a line
				handleLineContinuation();
			} else if("0123456789-.".indexOf(c)!=-1) {
				if(c=='-' && (pos+1)<lineLength &&
						line.charAt(pos+1)=='-')
					break;
				
				// handle numbers, dates, and time spans
				handleNumberDateOrTimeSpan();
			} else if(Character.isJavaIdentifierStart(c)) {
				// handle identifiers
				handleIdentifier();
			} else {
				parseException("Unexpected character \"" + c + "\".)",
						lineNumber, pos);
			}
		}
		
		if(sb!=null) {
			toks.add(new Token(sb.toString(), lineNumber, tokenStart));
		}
		
		// if toks are empty, try another line
		// this seems a bit dangerous, but eventually we should get a null line
		// which serves as a termination condition for the recursion
		while(toks!=null && toks.isEmpty())
			toks=getLineTokens();
		
		return toks;
	}
	
	private void addEscapedCharInString(char c)
		throws SDLParseException {
		
		switch(c) {
			case '\\':
			case '"':
				sb.append(c);
				break;
			case 'n':
				sb.append('\n');
				break;
			case 'r':
				sb.append('\r');
				break;	
			case 't':
				sb.append('\t');
				break;								
			default:
				parseException("Ellegal escape character in " +
						"string literal: \"" + c + "\".",
						lineNumber, pos);
		}
	}
	
	private void handleDoubleQuoteString() throws SDLParseException,
		IOException {

		boolean escaped=false;
		startEscapedQuoteLine=false;	
		
		sb = new StringBuilder("\"");
		pos++;
		
		for(;pos<lineLength; pos++) {
			char c=line.charAt(pos);

			if(" \t".indexOf(c)!=-1 && startEscapedQuoteLine)
				continue;
			else
				startEscapedQuoteLine=false;
			
			if(escaped) {
				addEscapedCharInString(c);
				escaped=false;
			} else if(c=='\\') {
				// check for String broken across lines
				if(pos==lineLength-1 || (pos+1<lineLength &&
						" \t".indexOf(line.charAt(pos+1))!=-1)) {
					handleEscapedDoubleQuotedString();
				} else {
					escaped=true;
				}
			} else {
				sb.append(c);
				if(c=='"') {
					toks.add(new Token(sb.toString(), lineNumber,
							tokenStart));
					sb=null;
					return;
				}
			}
		}
		
		if(sb!=null) {
			String tokString = sb.toString();
			if(tokString.length()>0 && tokString.charAt(0)=='"' &&
					tokString.charAt(tokString.length()-1)!='"') {
				parseException("String literal \"" + tokString +
						"\" not terminated by end quote.", lineNumber,
						line.length());	
			} else if(tokString.length()==1 && tokString.charAt(0)=='"') {
				parseException("Orphan quote (unterminated " +
						"string)", lineNumber, line.length());	
			}
		}	
	}
	
	private void handleEscapedDoubleQuotedString()
		throws SDLParseException, IOException {
		
		if(pos==lineLength-1) {
			line = readLine();
			if(line==null) {
				parseException("Escape at end of file.", lineNumber,
						pos);
			}
			
			lineLength = line.length(); 
			pos=-1;
			startEscapedQuoteLine=true;
		} else {
			// consume whitespace
			int j=pos+1;
			while(j<lineLength &&
					" \t".indexOf(line.charAt(j))!=-1) j++;
			
			if(j==lineLength) {
				line = readLine();
				if(line==null) {
					parseException("Escape at end of file.",
							lineNumber, pos);
				}
					
				lineLength = line.length(); 
				pos=-1;
				startEscapedQuoteLine=true;
	
			} else {
				parseException("Malformed string literal - " +
						"escape followed by whitespace " +
						"followed by non-whitespace.", lineNumber,
						pos);
			}		
		}
	}	
	
	private void handleCharacterLiteral() throws SDLParseException {
		if(pos==lineLength-1)
			parseException("Got ' at end of line", lineNumber, pos);
		
		pos++;
		
		char c2 = line.charAt(pos);
		if(c2=='\\') {
			
			if(pos==lineLength-1)
				parseException("Got '\\ at end of line", lineNumber,
						pos);
			pos++;
			char c3 = line.charAt(pos);
			
			if(pos==lineLength-1)
				parseException("Got '\\" + c3 + " at end of " + 
						"line", lineNumber, pos);
			
			if(c3=='\\') {
				toks.add(new Token("'\\'", lineNumber,
						pos));
			} else if(c3=='\'') {
				toks.add(new Token("'''", lineNumber,
						pos));
			} else if(c3=='n') {
				toks.add(new Token("'\n'", lineNumber,
						pos));
			} else if(c3=='r') {
				toks.add(new Token("'\r'", lineNumber,
						pos));
			}  else if(c3=='t') {
				toks.add(new Token("'\t'", lineNumber,
						pos));
			} else {
				parseException("Illegal escape character " +
						line.charAt(pos), lineNumber, pos);	
			}
			
			pos++;
			if(line.charAt(pos)!='\'')
				expectingButGot("single quote (')", "\"" + line.charAt(
						pos) + "\"", lineNumber, pos);		
		} else {
			toks.add(new Token("'" +  c2 + "'", lineNumber,
					pos));
			if(pos==lineLength-1)
				parseException("Got '" + c2 + " at end of " + 
						"line", lineNumber, pos);
			pos++;
			if(line.charAt(pos)!='\'')
				expectingButGot("quote (')", "\"" + line.charAt(pos) +
						"\"", lineNumber, pos);		
		}
	}
	
	private void handleSlashComment() throws SDLParseException,
		IOException{
		
		if(pos==lineLength-1)
			parseException("Got slash (/) at end of line.", lineNumber,
					pos);
		
		if(line.charAt(pos+1)=='*') {
	
			int endIndex = line.indexOf("*/", pos+1);
			if(endIndex!=-1) {
				// handle comment on same line
				pos=endIndex+1;
			} else {
				// handle multiline comments
				inner: while(true) {
					line = readRawLine();							
					if(line==null) {
						parseException("/* comment not terminated.",
							lineNumber, -2);
					}
					
					endIndex = line.indexOf("*/");
	
					if(endIndex!=-1) {
						lineLength = line.length(); 
						pos=endIndex+1;
						break inner;
					}
				}	
			}
		} else if(line.charAt(pos+1)=='/') {
			parseException("Got slash (/) in unexpected location.", 
					lineNumber, pos);
		}
	}
	
	private void handleBackQuoteString() throws SDLParseException,
		IOException {
		
		int endIndex = line.indexOf("`", pos+1);
		
		if(endIndex!=-1) {
			// handle end quote on same line
			toks.add(new Token(line.substring(pos, endIndex+1),
					lineNumber, pos));
			sb=null;					
			
			pos=endIndex;
		} else {
			
			sb = new StringBuilder(line.substring(pos) +
					"\n");
			int start = pos;
			// handle multiline quotes
			inner: while(true) {
				line = readRawLine();
				if(line==null) {
					parseException("` quote not terminated.",
						lineNumber, -2);
				}
				
				endIndex = line.indexOf("`");
				if(endIndex!=-1) {
					sb.append(line.substring(0, endIndex+1));
					
					line=line.trim();
					lineLength = line.length(); 
					
					pos=endIndex;
					break inner;
				} else {
					sb.append(line + "\n");
				}
			}	
			
			toks.add(new Token(sb.toString(), lineNumber, start));
			sb=null;
		}
	}

	private void handleBinaryLiteral() throws SDLParseException,
		IOException {
		
		int endIndex = line.indexOf("]", pos+1);
		
		if(endIndex!=-1) {
			// handle end quote on same line
			toks.add(new Token(line.substring(pos, endIndex+1),
					lineNumber, pos));
			sb=null;					
			
			pos=endIndex;
		} else {					
			sb = new StringBuilder(line.substring(pos) +
					"\n");
			int start = pos;
			// handle multiline quotes
			inner: while(true) {
				line = readRawLine();
				if(line==null) {
					parseException("[base64] binary literal not " +
							"terminated.", lineNumber, -2);
				}
				
				endIndex = line.indexOf("]");
				if(endIndex!=-1) {
					sb.append(line.substring(0, endIndex+1));
					
					line=line.trim();
					lineLength = line.length(); 
					
					pos=endIndex;
					break inner;
				} else {
					sb.append(line + "\n");
				}
			}	
			
			toks.add(new Token(sb.toString(), lineNumber, start));
			sb=null;
		}
	}
	
	// handle a line continuation (not inside a string)
	private void handleLineContinuation() throws SDLParseException,
		IOException {
		
		if(line.substring(pos+1).trim().length()!=0) {
			parseException("Line continuation (\\) before end of line",
					lineNumber, pos);
		} else {
			line = readLine();
			if(line==null) {
				parseException("Line continuation at end of file.",
					lineNumber, pos);
			}
				
			lineLength = line.length(); 
			pos=-1;				
		}
	}
	
	private void handleNumberDateOrTimeSpan() throws SDLParseException {
		tokenStart=pos;
		sb=new StringBuilder();
		char c;
	
		for(;pos<lineLength; ++pos) {
			c=line.charAt(pos);
			
			if("0123456789.-+:abcdefghijklmnopqrstuvwxyz".indexOf(
					Character.toLowerCase(c))!=-1) {
				sb.append(c);
			} else if(c=='/' && !((pos+1)<lineLength &&
					line.charAt(pos+1)=='*')) {
				
				sb.append(c);
			} else {
				pos--;
				break;
			}
		}
		
		toks.add(new Token(sb.toString(), lineNumber, tokenStart));
		sb=null;
	}
	
	private void handleIdentifier() throws SDLParseException {
		tokenStart=pos;
		sb=new StringBuilder();
		char c;
		
		for(;pos<lineLength; ++pos) {
			c=line.charAt(pos);
			
			if(Character.isJavaIdentifierPart(c) || c=='-') {
				sb.append(c);
			} else {
				pos--;
				break;
			}
		}
		
		toks.add(new Token(sb.toString(), lineNumber, tokenStart));
		sb=null;
	}
	
	/**
	 * Close the reader and throw a SDLParseException
	 */
	private void parseException(String description, int line, int position)
		throws SDLParseException {
		try {
			reader.close();
		} catch(IOException ioe) { /* no recourse */ }
		
		// We add one because editors typically start with line 1 and position 1
		// rather than 0...
		throw new SDLParseException(description, line+1, position+1);			
	}
	
	/**
	 * Close the reader and throw a SDLParseException using the format
	 * Was expecting X but got Y.
	 */
	private void expectingButGot(String expecting, Object got, int line,
			int position)
		throws SDLParseException {
		
		parseException("Was expecting " + expecting + " but got " +
				String.valueOf(got), line, position);	
	}	
	
	/**
	 * Skips comment lines and blank lines.
	 * 
	 * @return the next line or null at the end of the file.
	 */
	String readLine() throws IOException {
		String line = reader.readLine();
		pos=0;

		if(line==null)
			return null;
		lineNumber++;
		
		String tLine = line.trim();	

		while(tLine.startsWith("#") || tLine.length()==0) {
			line = reader.readLine();
			if(line==null)
				return null;
			
			lineNumber++;
			tLine = line.trim();	
		}
		
		return line;
	}
	
	/**
	 * Reads a "raw" line including lines with comments and blank lines
	 * 
	 * @return the next line or null at the end of the file.
	 */
	String readRawLine() throws IOException {
		String line = reader.readLine();
		pos=0;

		if(line==null)
			return null;
		lineNumber++;
		
		return line;
	}	
	
	enum Type {
		IDENTIFIER,
		
		// punctuation
		COLON, EQUALS, START_BLOCK, END_BLOCK,
		
		// literals
		STRING, CHARACTER, BOOLEAN, NUMBER, DATE, TIME, BINARY, NULL
	}
	
	/**
	 * Combine a date only calendar with a TimeSpanWithZone to create a 
	 * date-time calendar 
	 */
	private static Calendar combine(Calendar dc, TimeSpanWithZone tswz) {		
		TimeZone tz = tswz.getTimeZone();
		if(tz==null)
			tz=TimeZone.getDefault();
		
		Calendar cc = new GregorianCalendar(tz);
		
		cc.set(Calendar.YEAR, dc.get(Calendar.YEAR));
		cc.set(Calendar.MONTH, dc.get(Calendar.MONTH));
		cc.set(Calendar.DAY_OF_MONTH, dc.get(Calendar.DAY_OF_MONTH));
		
		cc.set(Calendar.HOUR_OF_DAY, tswz.getHours());					
		cc.set(Calendar.MINUTE, tswz.getMinutes());
		cc.set(Calendar.SECOND, tswz.getSeconds());					
		cc.set(Calendar.MILLISECOND, tswz.getMilliseconds());
		
		// deal with calendar bug where fields are not
		// marked as set unless you fetch that particular
		// field
		cc.get(Calendar.YEAR);
		cc.get(Calendar.MONTH);
		cc.get(Calendar.DAY_OF_MONTH);
		
		cc.get(Calendar.HOUR_OF_DAY);
		cc.get(Calendar.MINUTE);
		cc.get(Calendar.SECOND);
		cc.get(Calendar.MILLISECOND);
				
		if(tz!=null)
			cc.setTimeZone(tz);
		
		return cc;		
	}	
	
	/**
	 * An SDL token.  
	 * 
	 * @author Daniel Leuck
	 */
	class Token {		
		Type type; 
		String text;
		int line;
		int position;
		int size;
		Object object;
		
		boolean punctuation;
		boolean literal;
		
		Token(String text, int line, int position) throws SDLParseException {
			this.text=text;
			
			// type=determineType();
			
			this.line=line;
			this.position=position;
			size=text.length();
			
			try {
				if(text.startsWith("\"") || text.startsWith("`")) {
					type=Type.STRING;
					object=parseString(text);
				} else if(text.startsWith("'")) {
					type=Type.CHARACTER;
					object=text.charAt(1);
				} else if(text.equals("null")) {
					type=Type.NULL;
					object=null;
				} else if(text.equals("true") || text.equals("on")) {
					type=Type.BOOLEAN;
					object=true;
				} else if(text.equals("false") || text.equals("off")) {
					type=Type.BOOLEAN;
					object=false;
				} else if(text.startsWith("[")) {
					type=Type.BINARY;
					object=parseBinary(text);
				} else if(text.charAt(0)!='/' && text.indexOf('/')!=-1 &&
						text.indexOf(':')==-1) {
					type=Type.DATE;
					object=Parser.parseDateTime(text);
				} else if(text.charAt(0)!=':' && text.indexOf(':')!=-1) {
					type=Type.TIME;
					object=parseTimeSpanWithZone(text);
				} else if("01234567890-.".indexOf(text.charAt(0))!=-1) {
					type=Type.NUMBER;
					object=Parser.parseNumber(text);				
				} else {
					char c = text.charAt(0);
					switch(c) {
						case '{': type=Type.START_BLOCK; break;
						case '}': type=Type.END_BLOCK; break;
						case '=': type=Type.EQUALS; break;
						case ':': type=Type.COLON; break;
					}	
				}
			} catch(IllegalArgumentException iae) {
				throw new SDLParseException(iae.getMessage(), line,
						position);
			}
			
			if(type==null)
				type=Type.IDENTIFIER;
			
			punctuation = type==Type.COLON || type==Type.EQUALS ||
				type==Type.START_BLOCK || type==Type.END_BLOCK;
			literal =  type!=Type.IDENTIFIER && !punctuation;
		}
		
		Object getObjectForLiteral() {
			return object;
		}
		
		public String toString() {
			return type + " " + text + " pos:" + position;
		}
		
		// This special parse method is used only by the Token class for
		// tokens which are ambiguously either a TimeSpan or the time component
		// of a date/time type
		TimeSpanWithZone parseTimeSpanWithZone(String text)
			throws SDLParseException {
			
			int day=0; // optional (not allowed for date_time)
			int hour=0; // mandatory
			int minute=0; // mandatory
			int second=0; // optional for date_time, mandatory for time span
			int millisecond=0; // optional
			
			String timeZone = null;				
			String dateText = text;
			
			int dashIndex = dateText.indexOf("-",1);
			if(dashIndex!=-1) {
				timeZone=dateText.substring(dashIndex+1);
				dateText=text.substring(0,dashIndex);
			}
			
			String[] segments = dateText.split(":");
			
			// we know this is the time component of a date time type
			// because the time zone has been set
			if(timeZone!=null) {			
				if(segments.length<2 || segments.length>3)
					parseException("date/time format exception.  Must " +
							"use hh:mm(:ss)(.xxx)(-z)", line, position);
			} else {
				if(segments.length<2 || segments.length>4)
					parseException("Time format exception.  For time " +
							"spans use (d:)hh:mm:ss(.xxx) and for the " + 
							"time component of a date/time type use " +
							"hh:mm(:ss)(.xxx)(-z)  If you use the day " +
							"component of a time span make sure to " +
							"prefix it with a lower case d", line,
							position);	
			}
			
			try {
				if(segments.length==4) {
					String dayString = segments[0];
					if(!dayString.endsWith("d"))
						parseException("The day component of a time " +
						    "span must end with a lower case d", line,
						    position);
					
					day = Integer.parseInt(dayString.substring(0,
							dayString.length()-1));
					
					hour=Integer.parseInt(segments[1]);
					minute=Integer.parseInt(segments[2]);
					
					if(segments.length==4) {
						String last = segments[3];
						int dotIndex = last.indexOf(".");
						
						if(dotIndex==-1) {
							second = Integer.parseInt(last);
						} else {
							second =
								Integer.parseInt(
										last.substring(0, dotIndex));
							
							String millis = last.substring(dotIndex+1);
							if(millis.length()==1)
								millis=millis+"00";
							else if(millis.length()==2)
								millis=millis+"0";
							
							millisecond =
								Integer.parseInt(millis);
						}
					}
					
					if(day<0) {
						hour=reverseIfPositive(hour);
						minute=reverseIfPositive(minute);
						second=reverseIfPositive(second);
						millisecond=reverseIfPositive(millisecond);
					}
				} else {
					hour=Integer.parseInt(segments[0]);
					minute=Integer.parseInt(segments[1]);
					
					if(segments.length==3) {
						String last = segments[2];
						int dotIndex = last.indexOf(".");
						
						if(dotIndex==-1) {
							second = Integer.parseInt(last);
						} else {
							second = Integer.parseInt(last.substring(0, dotIndex));
							
							String millis = last.substring(dotIndex+1);
							if(millis.length()==1)
								millis=millis+"00";
							else if(millis.length()==2)
								millis=millis+"0";
							
							millisecond = Integer.parseInt(millis);
						}
					}
					
					if(hour<0) {
						minute=reverseIfPositive(minute);
						second=reverseIfPositive(second);
						millisecond=reverseIfPositive(millisecond);
					}
				}
			} catch(NumberFormatException nfe) {
				parseException("Time format: " + nfe.getMessage(), line,
						position);
			}
			
			TimeSpanWithZone tswz = new TimeSpanWithZone(
					day, hour, minute, second, millisecond, timeZone
			);

			return tswz;
		}		
	}
	
	static int reverseIfPositive(int val) {
		if(val<1)
			return val;
		return 0-val;
	}

	// An intermediate object used to store a time span or the time
	// component of a date/time instance.  The types are disambiguated at
	// a later stage.
	static class TimeSpanWithZone {

		private TimeZone timeZone;
		int days, hours, minutes, seconds, milliseconds;
		
		private TimeSpanWithZone(int days, int hours, int minutes,
				int seconds, int milliseconds, String timeZone) {
			
			this.days=days;
			this.hours=hours;
			this.minutes=minutes;
			this.seconds=seconds;
			this.milliseconds=milliseconds;
			
			if(timeZone!=null)
				this.timeZone=TimeZone.getTimeZone(timeZone);
		}			
		
		int getDays() { return days; }
		int getHours() { return hours; }
		int getMinutes() { return minutes; }
		int getSeconds() { return seconds; }
		int getMilliseconds() { return milliseconds; }
		
		TimeZone getTimeZone() {
			return timeZone;
		}
	}
	
	////////////////////////////////////////////////////////////////////////////
	// Parsers for types
	////////////////////////////////////////////////////////////////////////////
	static String parseString(String literal) {
		if(literal.charAt(0)!=literal.charAt(literal.length()-1))
			throw new IllegalArgumentException("Malformed string <" +
					literal + ">.  Strings must start and end with \" or `");
		
		return literal.substring(1, literal.length()-1);
	}
	
	static Character parseCharacter(String literal) {
		if(literal.charAt(0)!='\'' ||
				literal.charAt(literal.length()-1)!='\'')
			throw new IllegalArgumentException("Malformed character <" +
					literal + ">.  Character literals must start and end " +
					"with single quotes.");		
		
		return new Character(literal.charAt(1));
	}
	
	static Number parseNumber(String literal) {
		int textLength = literal.length();
		boolean hasDot=false;
		int tailStart=0;
		
		for(int i=0;i<textLength;i++) {
			char c=literal.charAt(i);
			if("-0123456789".indexOf(c)==-1) {
				if(c=='.') {
					if(hasDot) {
						new NumberFormatException(
						    "Encountered second decimal point.");
					} else if(i==textLength-1) {
						new NumberFormatException(
								"Encountered decimal point at the " +
								"end of the number.");	
					} else {
						hasDot=true;
					}
				} else {
					tailStart=i;
					break;
				}
			} else {
				tailStart=i+1;
			}
		}
		
		String number = literal.substring(0, tailStart);
		String tail = literal.substring(tailStart);
		

		if(tail.length()==0) {
			if(hasDot)
				return new Double(number);
			else
				return new Integer(number);
		}
		
		if(tail.equalsIgnoreCase("BD")) {
			return new BigDecimal(number);
		} else if(tail.equalsIgnoreCase("L")) {
			if(hasDot)
				new NumberFormatException("Long literal with decimal " +
						"point");
			return new Long(number);
		} else if(tail.equalsIgnoreCase("F")) {
			return new Float(number);
		} else if(tail.equalsIgnoreCase("D")) {
			return new Double(number);
		}
		
		throw new NumberFormatException("Could not parse number <" + literal +
				">");
	}	
	
	static Calendar parseDateTime(String literal) {
		int spaceIndex = literal.indexOf(' ');
		if(spaceIndex==-1) {
			return parseDate(literal);
		} else {
			Calendar dc = parseDate(literal.substring(0,spaceIndex));
			String timeString = literal.substring(spaceIndex+1);
			
			int dashIndex = timeString.indexOf('-');
			String tzString = null;
			if(dashIndex!=-1) {
				tzString=timeString.substring(dashIndex+1);
				timeString=timeString.substring(0, dashIndex);
			}
			
			String[] timeComps = timeString.split(":");
			if(timeComps.length<2 || timeComps.length>3)
				throw new IllegalArgumentException("Malformed time " +
						"component in date/time literal.  Must use " +
						"hh:mm(:ss)(.xxx)");
			
			int hour = 0;
			int minute = 0;
			int second = 0;
			int millisecond = 0;

			// TODO - parse the time string, concatenate and return date/time
			try {
				hour=Integer.parseInt(timeComps[0]);
				minute=Integer.parseInt(timeComps[1]);
				
				if(timeComps.length==3) {
					String last = timeComps[2];
					
					int dotIndex = last.indexOf('.');
					if(dotIndex==-1) {
						second=Integer.parseInt(last);
					} else {
						second=Integer.parseInt(last.substring(0,dotIndex));
						
						String millis = last.substring(dotIndex+1);
						if(millis.length()==1)
							millis=millis+"00";
						else if(millis.length()==2)
							millis=millis+"0";
						millisecond=Integer.parseInt(millis);
					}
				}
			} catch(NumberFormatException nfe) {
				throw new IllegalArgumentException("Number format exception " +
						"in time portion of date/time literal \"" +
							nfe.getMessage() +"\"");
			}
			
			TimeZone tz = (tzString==null) ? TimeZone.getDefault() :
				TimeZone.getTimeZone(tzString);
			
			GregorianCalendar gc = new GregorianCalendar(tz);
			gc.set(dc.get(Calendar.YEAR), dc.get(Calendar.MONTH),
					dc.get(Calendar.DAY_OF_MONTH), hour, minute, second);
			gc.set(Calendar.MILLISECOND, millisecond);
			gc.getTime();
			
			return gc;
		}
	}
	
	static Calendar parseDate(String literal) {
		String[] comps = literal.split("/");
		if(comps.length!=3)
			throw new IllegalArgumentException("Malformed Date <" +
				literal + ">");
		
		try {
			return new GregorianCalendar(
					Integer.parseInt(comps[0]),
					Integer.parseInt(comps[1])-1,
					Integer.parseInt(comps[2])
			);
		} catch(NumberFormatException nfe) {
			throw new IllegalArgumentException("Number format exception in " +
					"date literal \"" + nfe.getMessage() +"\"");
			
		}
	}
	
	static byte[] parseBinary(String literal) {
		String stripped = literal.substring(1, literal.length()-1);
		StringBuilder sb = new StringBuilder();
		int btLength = stripped.length();
		for(int i=0; i<btLength; i++) {
			char c = stripped.charAt(i);
			if("\n\r\t ".indexOf(c)==-1)
				sb.append(c);
		}
				
		byte[] bytes = null;
		try {
			bytes=Base64.decode(sb.toString());
		} catch(Base64.EncodingException bee) {
			new IllegalArgumentException(bee.getMessage());
		}
		
		return bytes;
	}
	
	static SDLTimeSpan parseTimeSpan(String literal) {
		int days=0; // optional 
		int hours=0; // mandatory
		int minutes=0; // mandatory
		int seconds=0; // mandatory
		int milliseconds=0; // optional
				
		String[] segments = literal.split(":");
		
		if(segments.length<3 || segments.length>4)
			throw new IllegalArgumentException("Malformed time span <" +
					literal + ">.  Time spans must use the format " +
					"(d:)hh:mm:ss(.xxx) Note: if the day component is " +
					"included it must be suffixed with lower case \"d\"");
		
		try {
			if(segments.length==4) {
				String dayString = segments[0];
				if(!dayString.endsWith("d"))
					new IllegalArgumentException("The day component of a time " +
					    "span must end with a lower case d");
				
				days = Integer.parseInt(dayString.substring(0,
						dayString.length()-1));
				
				hours=Integer.parseInt(segments[1]);
				minutes=Integer.parseInt(segments[2]);
				
				if(segments.length==4) {
					String last = segments[3];
					int dotIndex = last.indexOf(".");
					
					if(dotIndex==-1) {
						seconds = Integer.parseInt(last);
					} else {
						seconds =
							Integer.parseInt(
									last.substring(0, dotIndex));
						
						String millis = last.substring(dotIndex+1);
						if(millis.length()==1)
							millis=millis+"00";
						else if(millis.length()==2)
							millis=millis+"0";
						
						milliseconds =
							Integer.parseInt(millis);
					}
				}
				
				if(days<0) {
					hours=reverseIfPositive(hours);
					minutes=reverseIfPositive(minutes);
					seconds=reverseIfPositive(seconds);
					milliseconds=reverseIfPositive(milliseconds);
				}
			} else {
				hours=Integer.parseInt(segments[0]);
				minutes=Integer.parseInt(segments[1]);
				
				String last = segments[2];
				int dotIndex = last.indexOf(".");
				
				if(dotIndex==-1) {
					seconds = Integer.parseInt(last);
				} else {
					seconds = Integer.parseInt(last.substring(0, dotIndex));
					
					String millis = last.substring(dotIndex+1);
					if(millis.length()==1)
						millis=millis+"00";
					else if(millis.length()==2)
						millis=millis+"0";
					milliseconds = Integer.parseInt(millis);
				}

				if(hours<0) {
					minutes=reverseIfPositive(minutes);
					seconds=reverseIfPositive(seconds);
					milliseconds=reverseIfPositive(milliseconds);
				}
			}
		} catch(NumberFormatException nfe) {
			throw new IllegalArgumentException("Number format in time span " +
					"exception: \"" + nfe.getMessage() + "\" for literal <" +
					literal + ">");
		}
		
		return new SDLTimeSpan(days, hours, minutes, seconds, milliseconds);
	}	
}