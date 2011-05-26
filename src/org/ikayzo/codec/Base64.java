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
package org.ikayzo.codec;

/**
 * Base64 Utilities
 * 
 * @author Daniel Leuck
 */
public class Base64 {

	/**
	 * 6 bit grouping -> Base64
	 */
	private static char[] toBase64 = makeToBase64();
	private static char[] makeToBase64() {			
		char chars[] = new char[64];
		int i = 0;
		for (char c = 'A'; c <= 'Z'; c++)
			chars[i++] = c;
		for (char c = 'a'; c <= 'z'; c++)
			chars[i++] = c;
		for (char c = '0'; c <= '9'; c++)
			chars[i++] = c;
		chars[i++] = '+';
		chars[i++] = '/';
		return chars;
	}

	/**
	 * Base64 -> 6 bit grouping
	 */	
	private static byte[] fromBase64 = makeFromBase64();
	private static byte[] makeFromBase64() {	
		byte[] bytes = new byte[128];
		for (int i = 0; i < bytes.length; i++)
			bytes[i] = -1;
		for (int i = 0; i < 64; i++)
			bytes[toBase64[i]] = (byte) i;
		return bytes;
	}

	/**
	 * Encode bytes in a base64 string.
	 * 
	 * @param bytes  the bytes to be encoded
	 * @return a String containing base64 chars
	 */
	public static String encode(byte[] bytes) {
		int bytesSize = bytes.length;
		
		// without padding
		int outSize = (bytesSize * 4 + 2) / 3; 

		// include padding
		char[] chars = new char[((bytesSize + 2) / 3) * 4];
		int ip = 0, op = 0;
		
		while (ip < bytesSize) {
			int i0 = bytes[ip++] & 0xff;
			int i1 = ip < bytesSize ? bytes[ip++] & 0xff : 0;
			int i2 = ip < bytesSize ? bytes[ip++] & 0xff : 0;
			int o0 = i0 >>> 2;
			int o1 = ((i0 & 3) << 4) | (i1 >>> 4);
			int o2 = ((i1 & 0xf) << 2) | (i2 >>> 6);
			int o3 = i2 & 0x3F;
			
			chars[op++] = toBase64[o0];
			chars[op++] = toBase64[o1];
			
			chars[op] = op < outSize ? toBase64[o2] : '=';
			op++;
			
			chars[op] = op < outSize ? toBase64[o3] : '=';
			op++;
		}
		
		return new String(chars);
	}

	/**
	 * Decodes a base64 String
	 * 
	 * @param base64String The string to be decoded
	 * @return The bytes represented by the string
	 * @throws Base64.EncodingException If the string is malformed (contains
	 *         illegal characters)
	 */
	public static byte[] decode(String base64String)
		throws EncodingException {
		
		char[] chars = base64String.toCharArray();
		
		int charsSize = chars.length;
		if (charsSize % 4 != 0)
			throw new IllegalArgumentException("Input string length is not " +
					"a multiple of 4.");
		while (charsSize > 0 && chars[charsSize - 1] == '=')
			charsSize--;
		int bytesSize = (charsSize * 3) / 4;
		byte[] bytes = new byte[bytesSize];
		int ip = 0, op = 0;
		
		while (ip < charsSize) {
			int i0 = chars[ip++];
			int i1 = chars[ip++];
			int i2 = ip < charsSize ? chars[ip++] : 'A';
			int i3 = ip < charsSize ? chars[ip++] : 'A';
			
			if (i0 > 127 || i1 > 127 || i2 > 127 || i3 > 127)
				throw new EncodingException("Illegal character in " +
						"base64 string.");
			int b0 = fromBase64[i0], b1 = fromBase64[i1], b2 = fromBase64[i2],
					 b3 = fromBase64[i3];
			
			if (b0 < 0 || b1 < 0 || b2 < 0 || b3 < 0)
				throw new EncodingException("Illegal character in " +
					"base64 string.");
			
			int o0 = (b0 << 2) | (b1 >>> 4);
			int o1 = ((b1 & 0xf) << 4) | (b2 >>> 2);
			int o2 = ((b2 & 3) << 6) | b3;
			bytes[op++] = (byte) o0;
			if (op < bytesSize)
				bytes[op++] = (byte) o1;
			if (op < bytesSize)
				bytes[op++] = (byte) o2;
		}
		return bytes;
	}
	
	/**
	 * An exception for improperly formed base64 Strings. 
	 * 
	 * @author Daniel Leuck
	 */
	@SuppressWarnings("serial")
	public static class EncodingException extends RuntimeException {
		public EncodingException(String message) {
			super(message);
		}
	}
}
