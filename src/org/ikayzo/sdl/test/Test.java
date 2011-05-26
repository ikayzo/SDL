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
package org.ikayzo.sdl.test;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;
import java.util.TreeMap;

import org.ikayzo.codec.Base64;
import org.ikayzo.sdl.*;

import static org.ikayzo.sdl.SDL.format;

/**
 * SDL unit tests.  Why aren't these JUnit tests?  Mostly because being self
 * contained makes them easy to port to other languages.
 * 
 * @author Daniel Leuck
 */
@SuppressWarnings("unchecked")
public class Test {

	private static PrintWriter out = new PrintWriter(System.out, true);
	private static int assertCount=0;
	private static int failures=0;
	
	// Tag datastructure tests
	private static final String TAG = "Tag";
	private static final String TAG_WRITE_PARSE = "Tag Write Parse";
	
	// Basic Types Tests
	private static final String STRING_DECLARATIONS = "String Declarations";	
	private static final String CHARACTER_DECLARATIONS = "Character Declarations";
	private static final String NUMBER_DECLARATIONS = "Number Declarations";	
	private static final String BOOLEAN_DECLARATIONS = "Boolean Declarations";
	private static final String NULL_DECLARATION = "Null Declaration";	
	private static final String DATE_DECLARATIONS = "Date Declarations";		
	private static final String TIME_SPAN_DECLARATIONS = "Time Span Declarations";	
	private static final String DATE_TIME_DECLARATIONS = "Date Time Declarations";	
	private static final String BINARY_DECLARATIONS = "Binary Declarations";		

	// Structure Tests
	private static final String EMPTY_TAG = "Empty Tag";
	private static final String VALUES = "Values";		
	private static final String ATTRIBUTES = "Attributes";	
	private static final String VALUES_AND_ATTRIBUTES = "Values and Attributes";		
	private static final String CHILDREN = "Children";	
	private static final String NAMESPACES = "Namespaces";	
	
	////////////////////////////////////////////////////////////////////////////
	// Tag Tests
	////////////////////////////////////////////////////////////////////////////
	private static void testTag() {	
		out.println("Doing basic Tag tests...");
		
		// Test to make sure Tag ignores the order in which attributes are
		// added.
		out.println("    Making sure attributes are consistently ordered...");
		Tag t1 = new Tag("test");
		t1.setAttribute("foo", "bar");
		t1.setAttribute("john", "doe");
		
		Tag t2 = new Tag("test");
		t2.setAttribute("john", "doe");	
		t2.setAttribute("foo", "bar");
		
		assertEquals(TAG, t1, t2);
		
		out.println("    Making sure tags with different structures return " +
				"false from .equals...");
		
		t2.setValue("item");
		assertNotEquals(TAG, t1, t2);
		
		t2.removeValue("item");
		t2.setAttribute("another", "attribute");
		assertNotEquals(TAG, t1, t2);
		
		out.println("    Checking attributes namespaces...");
		
		t2.setAttribute("name", "bill");
		t2.setAttribute("private", "smoker", true);
		t2.setAttribute("public", "hobby", "hiking");
		t2.setAttribute("private", "nickname", "tubby");
		
		assertEquals(TAG, t2.getAttributesForNamespace("private"),
				new TreeMap<String,Object>(
						map("smoker",true,"nickname","tubby")
						));
	}	
	
	private static void testTagWriteParse(String fileName, Tag root)
		throws SDLParseException {
		
		out.println("Doing Tag write/parse tests for file " + fileName + "...");
		
		// Write out the contents of a tag, read the output back in and
		// test for equality.  This is a very rigorous test for any non-trivial
		// file.  It tests the parsing, output, and .equals implementation.
		out.println("    Write out the tag and read it back in...");
		
		assertEquals(TAG_WRITE_PARSE, root, new Tag("test")
				.read(root.toString()).getChild("root"));
		
		
	}
	
	////////////////////////////////////////////////////////////////////////////
	// Basic Types Tests
	////////////////////////////////////////////////////////////////////////////
	
	private static void testStrings(Tag root) {
		out.println("Doing String tests...");
		out.println("    Doing basic tests including new line handling...");
		assertEquals(STRING_DECLARATIONS, root.getChild("string1").getValue(), "hello");
		assertEquals(STRING_DECLARATIONS, root.getChild("string2").getValue(), "hi");
		assertEquals(STRING_DECLARATIONS, root.getChild("string3").getValue(), "aloha");
		assertEquals(STRING_DECLARATIONS, root.getChild("string4").getValue(), "hi there");		
		assertEquals(STRING_DECLARATIONS, root.getChild("string5").getValue(), "hi there joe");		
		assertEquals(STRING_DECLARATIONS, root.getChild("string6").getValue(), "line1\nline2");
		assertEquals(STRING_DECLARATIONS, root.getChild("string7").getValue(), "line1\nline2");
		assertEquals(STRING_DECLARATIONS, root.getChild("string8").getValue(), "line1\nline2\nline3");			
		assertEquals(STRING_DECLARATIONS, root.getChild("string9").getValue(),
				"Anything should go in this line without escapes \\ \\\\ \\n " +
				"\\t \" \"\" ' ''");
		assertEquals(STRING_DECLARATIONS, root.getChild("string10").getValue(), "escapes \"\\\n\t");
		
		out.println("    Checking unicode strings...");
		assertEquals(STRING_DECLARATIONS, root.getChild("japanese").getValue(), "\u65e5\u672c\u8a9e");
		assertEquals(STRING_DECLARATIONS, root.getChild("korean").getValue(), "\uc5ec\ubcf4\uc138\uc694");
		assertEquals(STRING_DECLARATIONS, root.getChild("russian").getValue(),
				"\u0437\u0434\u0440\u0430\u0432\u0441\u0442\u0432\u0443\u043b\u0442\u0435");
		
		out.println("    More new line tests...");
		assertContains(STRING_DECLARATIONS, (String)root.getChild("xml").getValue(),
				"<text>Hi there!</text>");
		assertEquals(STRING_DECLARATIONS, root.getChild("line_test").getValue(),
				"\nnew line above and below\n");
	}

	private static void testCharacters(Tag root) {		
		out.println("Doing character tests...");
		assertEquals(CHARACTER_DECLARATIONS, root.getChild("char1").getValue(), 'a');
		assertEquals(CHARACTER_DECLARATIONS, root.getChild("char2").getValue(), 'A');
		assertEquals(CHARACTER_DECLARATIONS, root.getChild("char3").getValue(), '\\');
		assertEquals(CHARACTER_DECLARATIONS, root.getChild("char4").getValue(), '\n');
		assertEquals(CHARACTER_DECLARATIONS, root.getChild("char5").getValue(), '\t');
		assertEquals(CHARACTER_DECLARATIONS, root.getChild("char6").getValue(), '\'');
		assertEquals(CHARACTER_DECLARATIONS, root.getChild("char7").getValue(), '"');
		
		out.println("    Doing unicode character tests...");
		assertEquals(CHARACTER_DECLARATIONS, root.getChild("char8").getValue(), '\u65e5');
		assertEquals(CHARACTER_DECLARATIONS, root.getChild("char9").getValue(), '\uc5ec');
		assertEquals(CHARACTER_DECLARATIONS, root.getChild("char10").getValue(), '\u0437');
	}

	private static void testNumbers(Tag root) {		
		out.println("Doing number tests...");
		
		out.println("    Testing ints...");
		assertEquals(NUMBER_DECLARATIONS, root.getChild("int1").getValue(), 0);
		assertEquals(NUMBER_DECLARATIONS, root.getChild("int2").getValue(), 5);
		assertEquals(NUMBER_DECLARATIONS, root.getChild("int3").getValue(), -100);
		assertEquals(NUMBER_DECLARATIONS, root.getChild("int4").getValue(), 234253532);
		
		out.println("    Testing longs...");
		assertEquals(NUMBER_DECLARATIONS, root.getChild("long1").getValue(), 0L);
		assertEquals(NUMBER_DECLARATIONS, root.getChild("long2").getValue(), 5L);
		assertEquals(NUMBER_DECLARATIONS, root.getChild("long3").getValue(), 5L);
		assertEquals(NUMBER_DECLARATIONS, root.getChild("long4").getValue(), 3904857398753453453L);		
		
		out.println("    Testing floats...");
		assertEquals(NUMBER_DECLARATIONS, root.getChild("float1").getValue(), 1F);
		assertEquals(NUMBER_DECLARATIONS, root.getChild("float2").getValue(), .23F);
		assertEquals(NUMBER_DECLARATIONS, root.getChild("float3").getValue(), -.34F);

		out.println("    Testing doubles...");
		assertEquals(NUMBER_DECLARATIONS, root.getChild("double1").getValue(), 2D);
		assertEquals(NUMBER_DECLARATIONS, root.getChild("double2").getValue(), -.234D);
		assertEquals(NUMBER_DECLARATIONS, root.getChild("double3").getValue(), 2.34D);
		
		out.println("    Testing decimals (BigDouble in Java)...");
		assertEquals(NUMBER_DECLARATIONS, root.getChild("decimal1").getValue(),
				new BigDecimal("0"));
		assertEquals(NUMBER_DECLARATIONS, root.getChild("decimal2").getValue(),
				new BigDecimal("11.111111"));
		assertEquals(NUMBER_DECLARATIONS, root.getChild("decimal3").getValue(),
				new BigDecimal("234535.3453453453454345345341242343"));		
	}
	
	private static void testBooleans(Tag root) {		
		out.println("Doing boolean tests...");

		assertEquals(BOOLEAN_DECLARATIONS, root.getChild("light-on").getValue(), true);
		assertEquals(BOOLEAN_DECLARATIONS, root.getChild("light-off").getValue(), false);
		assertEquals(BOOLEAN_DECLARATIONS, root.getChild("light1").getValue(), true);
		assertEquals(BOOLEAN_DECLARATIONS, root.getChild("light2").getValue(), false);
	}
	
	private static void testNull(Tag root) {		
		out.println("Doing null test...");

		assertEquals(NULL_DECLARATION, root.getChild("nothing").getValue(), null);
	}	
	
	private static void testDates(Tag root) {
		out.println("Doing date tests...");

		assertEquals(DATE_DECLARATIONS, root.getChild("date1").getValue(),
				getDate(2005,12,31));
		assertEquals(DATE_DECLARATIONS, root.getChild("date2").getValue(),
				getDate(1882,5,2));
		assertEquals(DATE_DECLARATIONS, root.getChild("date3").getValue(),
				getDate(1882,5,2));		
		assertEquals(DATE_DECLARATIONS, root.getChild("_way_back").getValue(),
				getDate(582,9,16));			
	}

	private static void testTimeSpans(Tag root) {
		out.println("Doing time span tests...");

		assertEquals(TIME_SPAN_DECLARATIONS, root.getChild("time1").getValue(),
				new SDLTimeSpan(0,12,30,0,0));
		assertEquals(TIME_SPAN_DECLARATIONS, root.getChild("time2").getValue(),
				new SDLTimeSpan(0,24,0,0,0));
		assertEquals(TIME_SPAN_DECLARATIONS, root.getChild("time3").getValue(),
				new SDLTimeSpan(0,1,0,0,0));	
		assertEquals(TIME_SPAN_DECLARATIONS, root.getChild("time4").getValue(),
				new SDLTimeSpan(0,1,0,0,0));	
		assertEquals(TIME_SPAN_DECLARATIONS, root.getChild("time5").getValue(),
				new SDLTimeSpan(0,12,30,2,0));	
		assertEquals(TIME_SPAN_DECLARATIONS, root.getChild("time6").getValue(),
				new SDLTimeSpan(0,12,30,23,0));	
		assertEquals(TIME_SPAN_DECLARATIONS, root.getChild("time7").getValue(),
				new SDLTimeSpan(0,12,30,23,100));	
		assertEquals(TIME_SPAN_DECLARATIONS, root.getChild("time8").getValue(),
				new SDLTimeSpan(0,12,30,23,120));	
		assertEquals(TIME_SPAN_DECLARATIONS, root.getChild("time9").getValue(),
				new SDLTimeSpan(0,12,30,23,123));
		
		out.println("    Checking time spans with days...");
		assertEquals(TIME_SPAN_DECLARATIONS, root.getChild("time10").getValue(),
				new SDLTimeSpan(34,12,30,23,100));	
		assertEquals(TIME_SPAN_DECLARATIONS, root.getChild("time11").getValue(),
				new SDLTimeSpan(1,12,30,0,0));	
		assertEquals(TIME_SPAN_DECLARATIONS, root.getChild("time12").getValue(),
				new SDLTimeSpan(5,12,30,23,123));
		
		out.println("    Checking negative time spans...");
		assertEquals(TIME_SPAN_DECLARATIONS, root.getChild("time13").getValue(),
				new SDLTimeSpan(0,-12,-30,-23,-123));	
		assertEquals(TIME_SPAN_DECLARATIONS, root.getChild("time14").getValue(),
				new SDLTimeSpan(-5,-12,-30,-23,-123));
	}
	
	private static void testDateTimes(Tag root) {
		out.println("Doing date time tests...");

		assertEquals(DATE_TIME_DECLARATIONS, root.getChild("date_time1").getValue(),
				getDateTime(2005,12,31,12,30,0,0,null));	
		assertEquals(DATE_TIME_DECLARATIONS, root.getChild("date_time2").getValue(),
				getDateTime(1882,5,2,12,30,0,0,null));	
		assertEquals(DATE_TIME_DECLARATIONS, root.getChild("date_time3").getValue(),
				getDateTime(2005,12,31,1,0,0,0,null));	
		assertEquals(DATE_TIME_DECLARATIONS, root.getChild("date_time4").getValue(),
				getDateTime(1882,5,2,1,0,0,0,null));	
		assertEquals(DATE_TIME_DECLARATIONS, root.getChild("date_time5").getValue(),
				getDateTime(2005,12,31,12,30,23,120,null));	
		assertEquals(DATE_TIME_DECLARATIONS, root.getChild("date_time6").getValue(),
				getDateTime(1882,5,2,12,30,23,123,null));	
		
		out.println("    Checking timezones...");
		assertEquals(DATE_TIME_DECLARATIONS, root.getChild("date_time7").getValue(),
				getDateTime(1882,5,2,12,30,23,123,"JST"));	
		assertEquals(DATE_TIME_DECLARATIONS, root.getChild("date_time8").getValue(),
				getDateTime(985,04,11,12,30,23,123,"PST"));	
	}
	
	private static void testBinaries(Tag root) throws Exception {
		out.println("Doing binary tests...");
		assertEquals(BINARY_DECLARATIONS, root.getChild("hi").getValue(),
			"hi".getBytes("UTF8"));	
		assertEquals(BINARY_DECLARATIONS, root.getChild("png").getValue(),
			Base64.decode(
				"iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAYAAAAf8/9hAAAAKnRFWHRDcmVhdGlvbiBUaW1l" +
				"AERpIDQgTXJ6IDIwMDMgMDA6MjQ6MDQgKzAxMDDdSQ6OAAAAB3RJTUUH0wMEAAcllPlrJgAA" +
				"AAlwSFlzAAAK8AAACvABQqw0mAAAAARnQU1BAACxjwv8YQUAAADQSURBVHjaY2CgEDCCyZn/" +
				"3YHkDhL1ejCkM+5kgXJ2zDQmXueShwwMh9+ALWSEGcCQfhZIvHlDnAk8PAwMHBxgJtyAa7bX" +
				"UdT8/cvA8Ps3hP7zB4FBYn/+vGbweqyJaoCmpiaKASDFv35BNMBoZMzwGKKOidJYoNgAuBdm" +
				"naXQgHRKDfgagxD89w8S+iAaFICwGIHFAgjrHUczAByySAaAMEgDLBphhv7/D8EYLgDZhAxA" +
				"mkAKYYbAMMwwDAOQXYDuDXRXgDC6AR7SW8jITNQAACjZgdj4VjlqAAAAAElFTkSuQmCC"						
			));		
	}
	
	////////////////////////////////////////////////////////////////////////////
	// Structure Tests (values, attributes, children)
	////////////////////////////////////////////////////////////////////////////
	
	public static void testEmptyTag(Tag root) {
		out.println("Doing empty tag test...");
		
		assertEquals(EMPTY_TAG, root.getChild("empty_tag"), new Tag("empty_tag"));
	}
	
	public static void testValues(Tag root) {
		out.println("Doing values tests...");

		assertEquals(VALUES, root.getChild("values1").getValues(), list("hi"));
		assertEquals(VALUES, root.getChild("values2").getValues(), list("hi","ho"));
		assertEquals(VALUES, root.getChild("values3").getValues(), list(1, "ho"));	
		assertEquals(VALUES, root.getChild("values4").getValues(), list("hi",5));
		assertEquals(VALUES, root.getChild("values5").getValues(), list(1,2));	
		assertEquals(VALUES, root.getChild("values6").getValues(), list(1,2,3));	
		assertEquals(VALUES, root.getChild("values7").getValues(),
				list(null,"foo",false,getDate(1980,12,5)));		
		assertEquals(VALUES, root.getChild("values8").getValues(),
				list(null, "foo", false, getDateTime(1980,12,5,12,30,0,0,null),
						"there", new SDLTimeSpan(0,15,23,12,234)));
		assertEquals(VALUES, root.getChild("values9").getValues(),
				list(null, "foo", false, getDateTime(1980,12,5,12,30,0,0,null),
						"there", getDateTime(1989,8,12,15,23,12,234,"JST")));
		assertEquals(VALUES, root.getChild("values10").getValues(),
				list(null, "foo", false, getDateTime(1980,12,5,12,30,0,0,null),
						"there", new SDLTimeSpan(0,15,23,12,234), "more stuff"));
		assertEquals(VALUES, root.getChild("values11").getValues(),
				list(null, "foo", false, getDateTime(1980,12,5,12,30,0,0,null),
						"there", new SDLTimeSpan(123,15,23,12,234),
						"more stuff here"));		
		assertEquals(VALUES, root.getChild("values12").getValues(), list(1,3));
		assertEquals(VALUES, root.getChild("values13").getValues(), list(1,3));
		assertEquals(VALUES, root.getChild("values14").getValues(), list(1,3));	
		assertEquals(VALUES, root.getChild("values15").getValues(), list(1,2,4,5,6));
		assertEquals(VALUES, root.getChild("values16").getValues(), list(1,2,5));	
		assertEquals(VALUES, root.getChild("values17").getValues(), list(1,2,5));		
		assertEquals(VALUES, root.getChild("values18").getValues(), list(1,2,7));	
		assertEquals(VALUES, root.getChild("values19").getValues(),
				list(1,3,5,7));	
		assertEquals(VALUES, root.getChild("values20").getValues(),
				list(1,3,5));		
		assertEquals(VALUES, root.getChild("values21").getValues(),
				list(1,3,5));			 
		assertEquals(VALUES, root.getChild("values22").getValues(),
				list("hi","ho","ho",5,"hi"));			
	}
	
	public static void testAttributes(Tag root) throws Exception {
		out.println("Doing attribute tests...");

		assertEquals(ATTRIBUTES, root.getChild("atts1").getAttributes(),
				map("name","joe"));
		assertEquals(ATTRIBUTES, root.getChild("atts2").getAttributes(),
				map("size",5));	
		assertEquals(ATTRIBUTES, root.getChild("atts3").getAttributes(),
				map("name","joe","size",5));	
		assertEquals(ATTRIBUTES, root.getChild("atts4").getAttributes(),
				map("name","joe","size",5,"smoker",false));
		assertEquals(ATTRIBUTES, root.getChild("atts5").getAttributes(),
				map("name","joe","smoker",false));
		assertEquals(ATTRIBUTES, root.getChild("atts6").getAttributes(),
				map("name","joe","smoker",false));	
		assertEquals(ATTRIBUTES, root.getChild("atts7").getAttributes(),
				map("name","joe"));
		assertEquals(ATTRIBUTES, root.getChild("atts8").getAttributes(),
				map("name","joe","size",5,"smoker",false,"text","hi","birthday",
						getDate(1972,5,23)));
		assertEquals(ATTRIBUTES, root.getChild("atts9").getAttribute("key"),
				"mykey".getBytes("utf8"));
	}	
	
	public static void testValuesAndAttributes(Tag root) {
		out.println("Doing values and attributes tests...");

		assertEquals(VALUES_AND_ATTRIBUTES, root.getChild("valatts1")
				.getValues(), list("joe"));
		assertEquals(VALUES_AND_ATTRIBUTES, root.getChild("valatts1")
				.getAttributes(), map("size", 5));		
		
		assertEquals(VALUES_AND_ATTRIBUTES, root.getChild("valatts2")
				.getValues(), list("joe"));
		assertEquals(VALUES_AND_ATTRIBUTES, root.getChild("valatts2")
				.getAttributes(), map("size", 5));			
		
		assertEquals(VALUES_AND_ATTRIBUTES, root.getChild("valatts3")
				.getValues(), list("joe"));
		assertEquals(VALUES_AND_ATTRIBUTES, root.getChild("valatts3")
				.getAttributes(), map("size", 5));

		assertEquals(VALUES_AND_ATTRIBUTES, root.getChild("valatts4")
				.getValues(), list("joe"));
		assertEquals(VALUES_AND_ATTRIBUTES, root.getChild("valatts4")
				.getAttributes(), map("size", 5, "weight", 160, "hat", "big"));
		
		assertEquals(VALUES_AND_ATTRIBUTES, root.getChild("valatts5")
				.getValues(), list("joe", "is a\n nice guy"));
		assertEquals(VALUES_AND_ATTRIBUTES, root.getChild("valatts5")
				.getAttributes(), map("size", 5, "smoker", false));		

		assertEquals(VALUES_AND_ATTRIBUTES, root.getChild("valatts6")
				.getValues(), list("joe", "is a\n nice guy"));
		assertEquals(VALUES_AND_ATTRIBUTES, root.getChild("valatts6")
				.getAttributes(), map("size", 5, "house", "big and\n blue"));
		
		//////////
		
		assertEquals(VALUES_AND_ATTRIBUTES, root.getChild("valatts7")
				.getValues(), list("joe", "is a\n nice guy"));
		assertEquals(VALUES_AND_ATTRIBUTES, root.getChild("valatts7")
				.getAttributes(), map("size", 5, "smoker", false));
		
		assertEquals(VALUES_AND_ATTRIBUTES, root.getChild("valatts8")
				.getValues(), list("joe", "is a\n nice guy"));
		assertEquals(VALUES_AND_ATTRIBUTES, root.getChild("valatts8")
				.getAttributes(), map("size", 5, "smoker", false));
		
		assertEquals(VALUES_AND_ATTRIBUTES, root.getChild("valatts9")
				.getValues(),list("joe", "is a\n nice guy"));
			assertEquals(VALUES_AND_ATTRIBUTES, root.getChild("valatts9")
					.getAttributes(), map("size", 5, "smoker", false));			
	}	
	
	public static void testChildren(Tag root) {
		out.println("Doing children tests...");

		Tag parent = root.getChild("parent");
		
		assertEquals(CHILDREN, parent.getChildren().size(), 2);
		assertEquals(CHILDREN, parent.getChildren().get(1).getName(),
				"daughter");
		
		
		Tag grandparent = root.getChild("grandparent");
		
		assertEquals(CHILDREN, grandparent.getChildren().size(), 2);
		// recursive fetch of children
		assertEquals(CHILDREN, grandparent.getChildren(true).size(), 6);		
		assertEquals(CHILDREN, grandparent.getChildren("son", true).size(), 2);	
		
		Tag grandparent2 = root.getChild("grandparent2");
		assertEquals(CHILDREN, grandparent2.getChildren("child", true)
				.size(), 5);
		assertEquals(CHILDREN, grandparent2.getChild("daughter", true)
				.getAttribute("birthday"),getDate(1976,04,18));
		
		Tag files = root.getChild("files");
		
		assertEquals(CHILDREN, files.getChildrenValues("content"),
				list("c:/file1.txt", "c:/file2.txt", "c:/folder"));
		
		Tag matrix = root.getChild("matrix");
		
		assertEquals(CHILDREN, matrix.getChildrenValues("content"),
				list(list(1,2,3),list(4,5,6)));		
	}
	
	public static void testNamespaces(Tag root) {
		out.println("Doing namespaces tests...");
		
		assertEquals(NAMESPACES, root.getChildrenForNamespace("person", true)
				.size(), 8);
		
		Tag grandparent2 = root.getChild("grandparent3");
		
		// get only the attributes for Akiko in the public namespace
		assertEquals(NAMESPACES, grandparent2.getChild("daughter", true)
				.getAttributesForNamespace("public"), map("name", "Akiko",
						"birthday", getDate(1976,04,18)));
	}
	
	public static void main(String[] args) {
		go();
	}
	
	public static void go() {

		out.println("Begin tests");

		try { testTag(); } catch(Throwable e) {
			reportException(TAG, e);
		}		
		
		testBasicTypes();
		testStructures();
		
		out.println();
		
		out.println("Checked " + assertCount + " assertions");
		
		if(failures==0)
			out.println(":-) All tests succeeded!");
		else
			out.println("Summary: " + failures + (failures==1 ? " failure" :
				" failures."));
	}

	private static void testBasicTypes() {
		
		out.println("Reading test_basic_types.sdl");
		
		Tag root = null;
		
		try {
			root=new Tag("root").read(new InputStreamReader(
					Test.class.getResourceAsStream("test_basic_types.sdl"),
					"UTF8"));
		} catch(IOException ioe) {
			reportException("Problem reading test_basic_types.sdl", ioe);
		} catch(SDLParseException spe) {
			reportException("Problem parsing test_basic_types.sdl", spe);
		}
		
		out.println("Successfully read and parsed test_basic_types.sdl");	
		
		try { testTagWriteParse("test_basic_types.sdl", root); } catch(Throwable e) {
			reportException(TAG_WRITE_PARSE, e);
		}
		
		try { testStrings(root); } catch(Throwable e) {
			reportException(STRING_DECLARATIONS, e);
		}
		
		try { testCharacters(root); } catch(Throwable e) {
			reportException(CHARACTER_DECLARATIONS, e);
		}
		
		try { testNumbers(root); } catch(Throwable e) {
			reportException(NUMBER_DECLARATIONS, e);
		}		

		try { testBooleans(root); } catch(Throwable e) {
			reportException(BOOLEAN_DECLARATIONS, e);
		}
		
		try { testNull(root); } catch(Throwable e) {
			reportException(NULL_DECLARATION, e);
		}
		
		try { testDates(root); } catch(Throwable e) {
			reportException(DATE_DECLARATIONS, e);
		}
		
		try { testTimeSpans(root); } catch(Throwable e) {
			reportException(TIME_SPAN_DECLARATIONS, e);
		}
		
		try { testDateTimes(root); } catch(Throwable e) {
			reportException(DATE_TIME_DECLARATIONS, e);
		}
		
		try { testBinaries(root); } catch(Throwable e) {
			reportException(BINARY_DECLARATIONS, e);
		}
	}
	
	private static void testStructures() {
		
		out.println("Reading test_structures.sdl");
		
		Tag root = null;
		
		try {
			root=new Tag("root").read(new InputStreamReader(
					Test.class.getResourceAsStream("test_structures.sdl"),
					"UTF8"));
		} catch(IOException ioe) {
			reportException("Problem reading test_structures.sdl", ioe);
		} catch(SDLParseException spe) {
			reportException("Problem parsing test_structures.sdl", spe);
		}
		
		out.println("Successfully read and parsed test_structures.sdl");

		try { testTagWriteParse("test_structures.sdl", root); } catch(Throwable e) {
			reportException(TAG_WRITE_PARSE, e);
		}		
		
		try { testEmptyTag(root); } catch(Throwable e) {
			reportException(EMPTY_TAG, e);
		}		
		
		try { testValues(root); } catch(Throwable e) {
			reportException(VALUES, e);
		}
		
		try { testAttributes(root); } catch(Throwable e) {
			reportException(ATTRIBUTES, e);
		}
		
		try { testValuesAndAttributes(root); } catch(Throwable e) {
			reportException(VALUES_AND_ATTRIBUTES, e);
		}	
		
		try { testChildren(root); } catch(Throwable e) {
			reportException(CHILDREN, e);
		}	
		
		try { testNamespaces(root); } catch(Throwable e) {
			reportException(NAMESPACES, e);
		}
	}
	
	private static void assertEquals(String testName, Object o1, Object o2) {
		assertCount++;
		if(!equals(o1, o2)) {
			failures++;
			out.println("!! Failure: " + testName + " - " + format(o1) +
					" does not equal " + format(o2));
		}
	}
	
	private static void assertNotEquals(String testName, Object o1, Object o2) {
		assertCount++;
		if(equals(o1, o2)) {
			failures++;
			out.println("!! Failure: " + testName + " - " + format(o1) +
					" equals " + format(o2));
		}	
	}
	
	private static void assertContains(String testName, String o1, String o2) {
		assertCount++;
		if(o1==null) {
			failures++;
			out.println("!! Failure: " + testName +
					" (contains type assertion) - first string argument " +
					"is null");
		} else if(o2==null) {
			failures++;
			out.println("!! Failure: " + testName + " (contains type " +
					"assertion) - second string argument is null");
		} else {
			if(!o1.contains(o2)) {
				failures++;
				out.println("!! Failure: " + testName + " - " + o1 +
						" does not contain " + o2);
			}
		}
	}
	
	private static void assertTrue(String testName, String evalString,
			boolean value) {
		assertCount++;
		if(!value) {
			failures++;
			out.println("!! Failure: " + testName + " - " + evalString +
					" is false");			
		}
	}
	
	private static void assertFalse(String testName, String evalString,
			boolean value) {
		assertCount++;
		if(value) {
			failures++;
			out.println("!! Failure: " + testName + " - " + evalString +
					" is true");			
		}
	}
	
	private static void reportException(String testName, Throwable e) {
		failures++;
		out.println("!! Failure: " + testName + " - " + e.getMessage());
	}
	
	////////////////////////////////////////////////////////////////////////////
	// Utility methods
	////////////////////////////////////////////////////////////////////////////

	private static Calendar getDate(int year, int month, int day) {	
		GregorianCalendar gc = new GregorianCalendar(year, month-1, day);
		
		// force calculations
		gc.get(Calendar.YEAR);
		gc.get(Calendar.MONTH);
		gc.get(Calendar.DAY_OF_MONTH);
		
		gc.clear(Calendar.HOUR_OF_DAY);
		gc.clear(Calendar.MINUTE);
		gc.clear(Calendar.SECOND);
		gc.clear(Calendar.MILLISECOND);
	
		return gc;
	}
	
	private static Calendar getTime(int hour, int minute, int second,
			int millisecond, String timeZone) {
		
		GregorianCalendar gc = new GregorianCalendar(1970,1,1);
		
		if(timeZone!=null)
			gc.setTimeZone(TimeZone.getTimeZone(timeZone));
		else
			gc.setTimeZone(TimeZone.getDefault());
		
		gc.clear(Calendar.YEAR);
		gc.clear(Calendar.MONTH);
		gc.clear(Calendar.DAY_OF_MONTH);			
		
		gc.set(Calendar.HOUR_OF_DAY, hour);
		gc.set(Calendar.MINUTE, minute);
		gc.set(Calendar.SECOND, second);
		gc.set(Calendar.MILLISECOND, millisecond);
		
		// force calculations
		gc.get(Calendar.HOUR_OF_DAY);
		gc.get(Calendar.MINUTE);
		gc.get(Calendar.SECOND);
		gc.get(Calendar.MILLISECOND);
		
		gc.clear(Calendar.YEAR);
		gc.clear(Calendar.MONTH);
		gc.clear(Calendar.DAY_OF_YEAR);				
		
		return gc;
	}
	
	private static Calendar getDateTime(int year, int month, int day, int hour,
			int minute, int second, int millisecond, String timeZone) {
	
		TimeZone tz = (timeZone==null) ? TimeZone.getDefault() :
			TimeZone.getTimeZone(timeZone);
		
		GregorianCalendar gc = new GregorianCalendar(tz);
		
		gc.set(Calendar.YEAR, year);
		gc.set(Calendar.MONTH, month-1);
		gc.set(Calendar.DAY_OF_MONTH, day);
		
		gc.set(Calendar.HOUR_OF_DAY, hour);
		gc.set(Calendar.MINUTE, minute);
		gc.set(Calendar.SECOND, second);
		gc.set(Calendar.MILLISECOND, millisecond);
		
		// force calculations
		gc.get(Calendar.YEAR);
		gc.get(Calendar.MONTH);
		gc.get(Calendar.DAY_OF_MONTH);
		
		gc.get(Calendar.HOUR_OF_DAY);
		gc.get(Calendar.MINUTE);
		gc.get(Calendar.SECOND);
		gc.get(Calendar.MILLISECOND);
	
		return gc;
	}
	
	private static List list(Object... obs) {
		ArrayList list = new ArrayList();
		for(Object o:obs)
			list.add(o);
		return list;
	}
	
	/**
	 * Make a map from alternating key/value pairs
	 */
	private static Map map(Object... obs) {
		TreeMap map = new TreeMap();
		for(int i=0; i<obs.length;)
			map.put(obs[i++], obs[i++]);		
		return map;
	}
	
	private static boolean equals(Object o1, Object o2) {
		if (o1==null)
			return o2==null;
		else if(o2==null)
			return false;
		
		if(o1.getClass().isArray() && o2.getClass().isArray() &&
				o1.getClass().getComponentType()==byte.class)
			return Arrays.equals((byte[])o1,(byte[])o2);
		else
			return o1.equals(o2);
	}	
}
