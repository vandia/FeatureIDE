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
package de.ovgu.featureide.fm.core.cnf.properties;

import java.util.ArrayList;
import java.util.List;

import de.ovgu.featureide.fm.core.cnf.CNF;

/**
 * 
 * @author Sebastian Krieter
 */
public class SatInstanceProperties {

	protected final CNF satInstance;

	protected final SatProperty satProperty = new SatProperty();
	protected final List<VariableProperty> varProperties;
	protected final List<ClauseProperty> properties = new ArrayList<>();

	public SatInstanceProperties(CNF satInstance) {
		this.satInstance = satInstance;
		this.varProperties = new ArrayList<>(satInstance.getVariables().size());
	}

}
