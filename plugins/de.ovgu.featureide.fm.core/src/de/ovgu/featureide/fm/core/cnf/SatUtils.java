/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2016  FeatureIDE team, University of Magdeburg, Germany
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
package de.ovgu.featureide.fm.core.cnf;

import java.util.Arrays;

/**
 * 
 * @author Sebastian Krieter
 */
public class SatUtils {

	public static LiteralSet negateSolution(LiteralSet solution) {
		final int[] literals = solution.getLiterals();
		int[] negSolution = Arrays.copyOf(literals, literals.length);
		for (int i = 0; i < negSolution.length; i++) {
			negSolution[i] = -negSolution[i];
		}
		return new LiteralSet(negSolution);
	}

	public static int[] negateSolution(int[] solution) {
		int[] negSolution = Arrays.copyOf(solution, solution.length);
		for (int i = 0; i < negSolution.length; i++) {
			negSolution[i] = -negSolution[i];
		}
		return negSolution;
	}

	public static void updateSolution(final int[] model1, int[] model2) {
		for (int i = 0; i < model1.length; i++) {
			final int x = model1[i];
			final int y = model2[i];
			if (x != y) {
				model1[i] = 0;
			}
		}
	}

	public static void updateSolution(final int[] model1, Iterable<int[]> models) {
		for (int i = 0; i < model1.length; i++) {
			final int x = model1[i];
			for (int[] model2 : models) {
				final int y = model2[i];
				if (x != y) {
					model1[i] = 0;
					break;
				}
			}
		}
	}
	
	public static int countNegative(int[] model) {
		int count = 0;
		for (int i = 0; i < model.length; i++) {
			count += model[i] >>> (Integer.SIZE - 1);
		}
		return count;
	}

}
