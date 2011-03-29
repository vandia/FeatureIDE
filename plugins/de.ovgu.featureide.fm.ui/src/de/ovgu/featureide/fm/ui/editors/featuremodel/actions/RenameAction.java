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
package de.ovgu.featureide.fm.ui.editors.featuremodel.actions;

import org.eclipse.gef.ui.parts.GraphicalViewerImpl;
import org.eclipse.swt.SWT;

import de.ovgu.featureide.fm.core.FeatureModel;
import de.ovgu.featureide.fm.ui.editors.featuremodel.editparts.FeatureEditPart;

/**
 * Renames a particular feature at the feature diagram.
 * 
 * @author Tom Brosch
 */
public class RenameAction extends SingleSelectionAction {

	public static String ID = "de.ovgu.featureide.rename";

	public RenameAction(GraphicalViewerImpl viewer, FeatureModel featureModel) {
		super("Rename (F2)", viewer);
		this.setAccelerator(SWT.F2);
	}

	@Override
	public void run() {
		FeatureEditPart part = getSelectedFeatureEditPart();
		part.showRenameManager();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * de.ovgu.featureide.fm.ui.editors.featuremodel.actions.SingleSelectionAction
	 * #updateProperties()
	 */
	@Override
	protected void updateProperties() {
		setEnabled(true);
	}

}
