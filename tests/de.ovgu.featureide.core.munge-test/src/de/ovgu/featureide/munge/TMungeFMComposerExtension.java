/* FeatureIDE - An IDE to support feature-oriented software development
 * Copyright (C) 2005-2011  FeatureIDE Team, University of Magdeburg
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 * See http://www.fosd.de/featureide/ for further information.
 */
package de.ovgu.featureide.munge;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

/**
 * TODO description
 * 
 * @author Jens Meinicke
 */
public class TMungeFMComposerExtension {
	private MungeFMComposerExtension fmComposerExtension = new MungeFMComposerExtension();
	
	private static final String OLD_NAME = "Hello"; 
	private static final String NEW_NAME = "NewFeature";
	
	private static final String OLD_FILE_CONTENT = 
		"public class Main {\r\n" +
		"\r\n" +
		"public static void main(String[] args){\r\n" +
		"	/*if[Hello]*/\r\n" +
		"	System.out.print(\"Hello\");\r\n" +
		"	/*end[Hello]*/\r\n" +
		"	/*if[Beautiful]*/	\r\n" +
		"	System.out.print(\" beautiful\");\r\n" +
		"	/*end[Beautiful]*/\r\n" +
		"	/*if[Wonderful]*/	\r\n" +
		"	System.out.print(\" wonderful\");\r\n" +	
		"	/*end[Wonderful]*/\r\n" +
		"	/*if[World]*/		\r\n" +
		"	System.out.print(\" world!\");\r\n" +
		"	/*end[World]*/\r\n" +
		"	}\r\n" +
		"}";
	
	private static final String NEW_FILE_CONTENT = 
		"public class Main {\r\n" +
		"\r\n" +
		"public static void main(String[] args){\r\n" +
		"	/*if[NewFeature]*/\r\n" +
		"	System.out.print(\"Hello\");\r\n" +
		"	/*end[NewFeature]*/\r\n" +
		"	/*if[Beautiful]*/	\r\n" +
		"	System.out.print(\" beautiful\");\r\n" +
		"	/*end[Beautiful]*/\r\n" +
		"	/*if[Wonderful]*/	\r\n" +
		"	System.out.print(\" wonderful\");\r\n" +	
		"	/*end[Wonderful]*/\r\n" +
		"	/*if[World]*/		\r\n" +
		"	System.out.print(\" world!\");\r\n" +
		"	/*end[World]*/\r\n" +
		"	}\r\n" +
		"}";
	
	@Test
	public void performRenamingsTest_1() {
		assertEquals(fmComposerExtension.performRenamings(OLD_NAME, NEW_NAME, OLD_FILE_CONTENT), NEW_FILE_CONTENT);
	}

}