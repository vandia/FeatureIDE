/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2015  FeatureIDE team, University of Magdeburg, Germany
 *
 * This file is part of FeatureIDE.
 * 
 * FeatureIDE is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * FeatureIDE is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with FeatureIDE.  If not, see <http://www.gnu.org/licenses/>.
 *
 * See http://featureide.cs.ovgu.de/ for further information.
 */
package org.prop4j;

import static de.ovgu.featureide.fm.core.localization.StringTable.IFF;
import static de.ovgu.featureide.fm.core.localization.StringTable.IMPLIES;
import static de.ovgu.featureide.fm.core.localization.StringTable.INVALID_NUMBER_OF_QUOTATION_MARKS;
import static de.ovgu.featureide.fm.core.localization.StringTable.INVALID_POSITIONING_OF_PARENTHESES;
import static de.ovgu.featureide.fm.core.localization.StringTable.NO_LEFT_SYMBOL_FOUND;
import static de.ovgu.featureide.fm.core.localization.StringTable.NO_OPERATOR_FOUND;
import static de.ovgu.featureide.fm.core.localization.StringTable.NO_RIGHT_SYMBOL_FOUND;
import static de.ovgu.featureide.fm.core.localization.StringTable.NO_SYMBOLS_FOUND_;
import static de.ovgu.featureide.fm.core.localization.StringTable.NO_SYMBOL_FOUND;
import static de.ovgu.featureide.fm.core.localization.StringTable.WHITESPACE_BEFORE_AND_AFTER_QUOTED_FEATURENAME_REQUIRED;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

/**
 * This class can be used to parse propositional formulas.
 * 
 * @author Dariusz Krolikowski
 * @author David Broneske
 * @author Fabian Benduhn
 * @author Thomas Thuem
 * @author Florian Proksch
 * @author Stefan Krueger
 */
public class NodeReader {

	public final static String[] textualSymbols = new String[] { IFF,
			IMPLIES, "or", "and", "not" };

	public final static String[] shortSymbols = new String[] { "<=>", "=>",
			"|", "&", "-" };

	public final static String[] logicalSymbols = new String[] { "\u21D4",
			"\u21D2", "\u2228", "\u2227", "\u00AC" };

	private Collection<String> featureNames;
	
	private boolean ignoreMissingFeatures = false;

	private String errorMessage = "";

	private String[] symbols = textualSymbols;

	private boolean error = false;

	public void activateShortSymbols() {
		symbols = shortSymbols;
	}

	public void activateTextualSymbols() {
		symbols = textualSymbols;
	}

	public void activateLogicalSymbols() {
		symbols = logicalSymbols;
	}

	public Node stringToNode(String constraint) {
		return stringToNode(constraint, null);
	}

	public Node stringToNode(String constraint, Collection<String> featureNames) {
		this.featureNames = featureNames;
		errorMessage = "";
		constraint = constraint.trim();

		if (!isWellFormed(constraint, featureNames))
			return null;
		return getNode(constraint);
	}

	/**
	 * if stringToNode or isWellFormed were called with not well-formed
	 * constraint this method returns the error message otherwise empty String
	 * 
	 * @return
	 */
	public String getErrorMessage() {
		return errorMessage;
	}

	/**
	 * returns true if constraint is well formed
	 * 
	 * @param constraint
	 * @return
	 */
	public boolean isWellFormed(String constraint) {
		return isWellFormed(constraint, null);
	}

	/**
	 * Checking expression on correct syntax
	 * 
	 * @param string
	 *            constraint (without brackets) to convert
	 * @param symbols
	 *            array containing strings for: iff, implies, or, and, not
	 * @param list
	 *            list of substituted bracket expressions
	 * @return
	 */
	private Node checkExpression(String string, LinkedList<String> list) {
		if (!error) {
			errorMessage = " ";
			string = " " + string.trim() + " ";
			if ("  ".equals(string)) {
				errorMessage = NO_SYMBOLS_FOUND_;
				error = true;
				return getInvalidLiteral();
			}
			// traverse all symbols
			for (int i = 0; i < symbols.length; i++) {
				while (string.contains(" " + symbols[i] + " ")) {
					int index = string.indexOf(" " + symbols[i] + " ") + 1; // 1st
																			// symbol
																			// occurrence

					if (index == -1) {
						errorMessage = NO_OPERATOR_FOUND;
						error = true;
						return getInvalidLiteral();
					}

					// recursion for children nodes
					Node node1 = null;
					if (i != 4)
						node1 = checkExpression(string.substring(0, index),
								list);
					Node node2 = checkExpression(string.substring(index
							+ symbols[i].length(), string.length()), list);

					if (i < 4) {
						if (index == 0) {
							errorMessage = NO_LEFT_SYMBOL_FOUND;
							error = true;
							return getInvalidLiteral();
						}

						if (string.length() - (index + symbols[i].length()) == 0) {
							errorMessage = NO_RIGHT_SYMBOL_FOUND;
							error = true;
							return getInvalidLiteral();
						}

					} else if (i == 4) {
						if (string.length() - (index + symbols[i].length()) == 0) {
							errorMessage = NO_SYMBOL_FOUND;
							error = true;
							return getInvalidLiteral();
						}
					}

					switch (i) {
						case 0: {
							return new Equals(node1, node2);
						}
						case 1: {
							return new Implies(node1, node2);
						}
						case 2: {
							return new Or(node1, node2);
						}
						case 3: {
							return new And(node1, node2);
						}
						case 4: {
							return new Not(node2); // Not - only one argument
	
						}
					}
				}
			}
			string = string.trim();
			if (isIntNumber(string)) {
				if (Integer.parseInt(string) >= 100000) {
					if (featureNames != null) {

						if (featureNames.contains(list.get(Integer
								.parseInt(string) - 100000))) {
							return new Literal(list.get(Integer.parseInt(string) - 100000));
						} else {
							errorMessage = list.get(Integer.parseInt(string) - 100000)
									+ " is no valid Feature Name";
							error = true;
							return getInvalidLiteral();
						}
					} else {
						return new Literal(list.get(Integer.parseInt(string) - 100000));
					}
				} else {
					return checkExpression(list.get(Integer.parseInt(string)), list);
				}
			}

			if (featureNames != null) {
				if (!featureNames.contains(string)) {
					errorMessage = string + " is no valid Feature Name";
					error = true;
					return getInvalidLiteral();
				}
			}
			if (string.contains(" ")) {
				errorMessage = string + " is no valid Feature Name";
				error = true;
				return getInvalidLiteral();
			}

			return new Literal(string);
		}
		return getInvalidLiteral();
	}
	
	private Literal getInvalidLiteral() {
		if (ignoreMissingFeatures) {
			error = false;
			return new Literal("FALSE");
		} else {
			return new Literal("");
		}
	}

	/**
	 * returns true if constraint is well formed
	 * 
	 * @param constraint
	 *            constraint supposed to be checked
	 * @param featureNames
	 *            list of feature names
	 * @return true if constraint is well formed
	 */
	public boolean isWellFormed(String constraint, final Collection<String> featureNames) {
		this.featureNames = featureNames;

		if (!constraint.trim().isEmpty()) {
			// Check constraint if brackets set correctly
			int bracketCounter = 0;
			for (int i = 0; i < constraint.length(); i++) {
				if (constraint.charAt(i) == '(') {
					bracketCounter++;
				} else if (constraint.charAt(i) == ')' && bracketCounter == 0) {
					errorMessage = INVALID_POSITIONING_OF_PARENTHESES;
					return false;
				} else if (constraint.charAt(i) == ')') {
					bracketCounter--;
				} else {

				}
			}
			if (bracketCounter < 0) {
				errorMessage = "invalid number of parentheses: to many ')'";
				return false;
			}
			if (bracketCounter > 0) {
				errorMessage = "invalid number of parentheses: to many '('";
				return false;
			}

			LinkedList<String> list = new LinkedList<String>();

			int counter = 0;

			// replacing all quotation-marked Featurenames with numbers
			// Same procedure as with brackets -> see below
			while (constraint.contains("\"")) {
				int indStart = constraint.indexOf("\"");
				int indEnd = constraint.indexOf("\"", indStart + 1);

				if (indEnd == -1) {
					errorMessage = INVALID_NUMBER_OF_QUOTATION_MARKS;
					return false;
				}
				if (indStart - 1 > 0 && constraint.charAt(indStart - 1) != ' '
						&& constraint.charAt(indStart - 1) != '('
						|| indEnd + 1 < constraint.length()
						&& constraint.charAt(indEnd + 1) != ' '
						&& constraint.charAt(indEnd + 1) != ')') {
					errorMessage = WHITESPACE_BEFORE_AND_AFTER_QUOTED_FEATURENAME_REQUIRED;
					return false;
				}

				// inner bracket found -> substitution to list
				// Names are added inclusive quotation marks
				list.add(constraint.substring(indStart + 1, indEnd));

				constraint = constraint.substring(0, indStart)
						+ (counter + 100000)
						+ constraint.substring(indEnd + 1, constraint.length());
				counter++;
			}

			// finding all bracket expressions
			while (constraint.contains(")")) {
				int indEnd = constraint.indexOf(")");
				int indStart = constraint.substring(0, indEnd).lastIndexOf("(");

				if (indStart - 1 > 0 && constraint.charAt(indStart - 1) != ' '
						&& constraint.charAt(indStart - 1) != '('
						|| indEnd + 1 < constraint.length()
						&& constraint.charAt(indEnd + 1) != ' '
						&& constraint.charAt(indEnd + 1) != ')') {
					errorMessage = WHITESPACE_BEFORE_AND_AFTER_QUOTED_FEATURENAME_REQUIRED;
					return false;
				}

				// inner bracket found -> substitution to list
				list.add(constraint.substring(indStart + 1, indEnd));
				constraint = constraint.substring(0, indStart) + counter
						+ constraint.substring(indEnd + 1, constraint.length());
				counter++;
			}

			error = false;
			checkExpression(constraint, list);
			return !error;
		}
		return true;
	}

	private Node getNode(String string) {

		LinkedList<String> list = new LinkedList<String>();
		int counter = 0;

		// replacing all quotation-marked Featurenames with numbers
		// Same procedure as with brackets -> see below
		while (string.contains("\"")) {

			int indStart = string.indexOf('\"');
			int indEnd = string.indexOf('\"', indStart + 1);

			// inner bracket found -> substitution to list
			// Names are added inclusive quotation marks
			list.add(string.substring(indStart + 1, indEnd));

			string = string.substring(0, indStart) + (counter + 100000)
					+ string.substring(indEnd + 1, string.length());
			counter++;
		}

		// finding all bracket expressions
		while (string.contains(")")) {
			int indEnd = string.indexOf(")");
			int indStart = string.substring(0, indEnd).lastIndexOf("(");

			// inner bracket found -> substitution to list
			list.add(string.substring(indStart + 1, indEnd));
			string = string.substring(0, indStart) + counter
					+ string.substring(indEnd + 1, string.length());
			counter++;
		}
		return stringToNodeRec(string, list);
	}

	/**
	 * Creating nodes out of constraints
	 * 
	 * @param string
	 *            constraint (without brackets) to convert
	 * @param symbols
	 *            array containing strings for: iff, implies, or, and, not
	 * @param list
	 *            list of substituted bracket expressions
	 * @return
	 */
	private Node stringToNodeRec(String string, List<String> list) {
		string = " " + string.trim() + " ";
		// traverse all symbols
		for (int i = 0; i < symbols.length; i++) {
			while (string.contains(" " + symbols[i] + " ")) {
				int index = string.indexOf(" " + symbols[i] + " ") + 1; // 1st
																		// symbol
																		// occurrence

				// recursion for children nodes
				Node node1 = stringToNodeRec(string.substring(0, index), list);
				Node node2 = stringToNodeRec(
						string.substring(index + symbols[i].length(),
								string.length()), list);

				switch (i) {
				case 0: {
					return new Equals(node1, node2);
				}
				case 1: {
					return new Implies(node1, node2);
				}
				case 2: {
					return new Or(node1, node2);
				}
				case 3: {
					return new And(node1, node2);
				}
				case 4: {
					return new Not(node2); // Not - only one argument
				}
				}
			}
		}

		string = string.trim();
		if (isIntNumber(string)) {
			if (Integer.parseInt(string) >= 100000) {

				return new Literal(list.get(Integer.parseInt(string) - 100000)
						.replace("\"", ""));

			} else {
				return stringToNodeRec(list.get(Integer.parseInt(string)), list);
			}
		}

		return new Literal(string.replace("\"", ""));
	}

	/**
	 * Checks, if num is an integer number
	 * 
	 * @param num
	 *            number to check
	 * @return true, if num is an integer number, false otherwise
	 */
	private static boolean isIntNumber(String num) {
		try {
			Integer.parseInt(num);
		} catch (NumberFormatException nfe) {
			return false;
		}
		return true;
	}
	
	public boolean ignoresMissingFeatures() {
		return ignoreMissingFeatures;
	}
	
	public void setIgnoreMissingFeatures(boolean ignoreMissingFeatures) {
		this.ignoreMissingFeatures = ignoreMissingFeatures;
	}

}
