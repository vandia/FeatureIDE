/* FeatureIDE - A Framework for Feature-Oriented Software Development
 * Copyright (C) 2005-2017  FeatureIDE team, University of Magdeburg, Germany
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
package de.ovgu.featureide.fm.core.io.manager;

import java.nio.file.Path;
import java.nio.file.Paths;

import javax.annotation.CheckForNull;

import de.ovgu.featureide.fm.core.ExtensionManager.NoSuchExtensionException;
import de.ovgu.featureide.fm.core.FeatureModelAnalyzer;
import de.ovgu.featureide.fm.core.analysis.cnf.FeatureModelFormula;
import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.base.event.FeatureIDEEvent;
import de.ovgu.featureide.fm.core.base.event.FeatureIDEEvent.EventType;
import de.ovgu.featureide.fm.core.base.event.IEventListener;
import de.ovgu.featureide.fm.core.base.impl.FMFactoryManager;
import de.ovgu.featureide.fm.core.base.impl.FMFormatManager;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.ovgu.featureide.fm.core.configuration.ConfigurationPropagator;
import de.ovgu.featureide.fm.core.io.IFeatureModelFormat;
import de.ovgu.featureide.fm.core.io.IPersistentFormat;
import de.ovgu.featureide.fm.core.io.InternalFeatureModelFormat;

/**
 * Responsible to load and save all information for a feature model instance.
 * 
 * @author Sebastian Krieter
 */
public class FeatureModelManager extends AFileManager<IFeatureModel> {

	public class FeatureModelChangeListner implements IEventListener {

		public void propertyChange(FeatureIDEEvent evt) {
			final EventType eventType = evt.getEventType();
			switch (eventType) {
//			case FEATURE_NAME_CHANGED:
//				String oldName = (String) evt.getOldValue();
//				String newName = (String) evt.getNewValue();
//				FeatureModelManager.this.renameFeature((IFeatureModel) evt.getSource(), oldName, newName);
//				break;
			case MODEL_DATA_LOADED:
				status = initStatus();
//				for (IFileManager<Configuration> iFileManager : configurationManagerList) {
//					iFileManager.setObject(new Configuration(iFileManager.getObject(), status.getFeatureModel()));
//				}
			default:
				break;
			}
		}
	}

	public static class FeatureModelSnapshot {
		private final FeatureModelFormula formula;
		private final IFeatureModel featureModel;
		private final FeatureModelAnalyzer analyzer;

		//		private List<Configuration> configurationList = Collections.emptyList();

		public FeatureModelSnapshot(IFeatureModel featureModel) {
			this.featureModel = featureModel;

			formula = new FeatureModelFormula(featureModel);
			analyzer = new FeatureModelAnalyzer(formula);
		}

		public FeatureModelFormula getFormula() {
			return formula;
		}

		public IFeatureModel getFeatureModel() {
			return featureModel;
		}

		public FeatureModelAnalyzer getAnalyzer() {
			return analyzer;
		}

		//		public List<Configuration> getConfigurationList() {
		//			return configurationList;
		//		}

		public ConfigurationPropagator getPropagator() {
			return new ConfigurationPropagator(formula, new Configuration(featureModel));
		}

		public ConfigurationPropagator getPropagator(Configuration configuration) {
			return new ConfigurationPropagator(formula, configuration);
		}

		public ConfigurationPropagator getPropagator(Configuration configuration, boolean includeAbstract) {
			return new ConfigurationPropagator(formula, configuration, includeAbstract);
		}

		public ConfigurationPropagator getPropagator(boolean includeAbstract) {
			return new ConfigurationPropagator(formula, new Configuration(featureModel), includeAbstract);
		}

	}

	private static final ObjectCreator<IFeatureModel> objectCreator = new ObjectCreator<IFeatureModel>(IFeatureModel.class, FeatureModelManager.class,
			FMFormatManager.getInstance()) {
		@Override
		protected IFeatureModel createObject(Path path, IPersistentFormat<IFeatureModel> format) throws NoSuchExtensionException {
			final IFeatureModel featureModel = FMFactoryManager.getFactory(path.toAbsolutePath().toString(), format).createFeatureModel();
			featureModel.setSourceFile(path);
			return featureModel;
		}
	};

	/**
	 * Returns an instance of a {@link IFileManager} for a certain file.
	 * 
	 * @param path The path pointing to the file.
	 * 
	 * @return The manager instance for the specified file, or {@code null} if no instance was created yet.
	 * 
	 * @throws ClassCastException When the found instance is no subclass of R.
	 */
	@CheckForNull
	public static FeatureModelManager getInstance(Path absolutePath) {
		return (FeatureModelManager) AFileManager.getInstance(absolutePath, objectCreator);
	}

	@CheckForNull
	public static FeatureModelManager getInstance(IFeatureModel featureModel) {
		final Path sourceFile = featureModel.getSourceFile();
		if (sourceFile != null) {
			return getInstance(sourceFile);
		}
		return null;
	}

	public static IFeatureModelFormat getFormat(String fileName) {
		return FMFormatManager.getInstance().getFormatByFileName(fileName);
	}

	public static boolean save(IFeatureModel featureModel, Path path) {
		final String pathString = path.toAbsolutePath().toString();
		final IFeatureModelFormat format = FMFormatManager.getInstance().getFormatByFileName(pathString);
		return !FileHandler.save(path, featureModel, format).containsError();
	}

	public static boolean convert(Path inPath, Path outPath) {
		IFeatureModel featureModel = load(inPath).getObject();
		if (featureModel == null) {
			return false;
		}
		return save(featureModel, outPath);
	}
	
	@Deprecated
	public static ConfigurationPropagator getPropagator(Configuration configuration, boolean includeAbstractFeatures) {
		return new ConfigurationPropagator(configuration, includeAbstractFeatures);
	}

	@Deprecated
	public static ConfigurationPropagator getPropagator(IFeatureModel featureModel, boolean includeAbstractFeatures) {
		final Configuration configuration = new Configuration(featureModel);
		return new ConfigurationPropagator(configuration, includeAbstractFeatures);
	}

//	private final HashSet<IFileManager<Configuration>> configurationManagerList = new HashSet<>();

	private FeatureModelSnapshot status;

	protected FeatureModelManager(IFeatureModel model, String absolutePath, IPersistentFormat<IFeatureModel> modelHandler) {
		super(setSourcePath(model, absolutePath), absolutePath, modelHandler);

		addListener(new FeatureModelChangeListner());

		// TODO Rename manager method save -> write
		// TODO Implement analyses for configurations
		// TODO synchronize configuration and featuremodel manger
		// TODO try to save and load everything

		// TODO synchronize with update method

		initStatus();
	}

	private static IFeatureModel setSourcePath(IFeatureModel model, String absolutePath) {
		model.setSourceFile(Paths.get(absolutePath));
		return model;
	}

	@Override
	protected boolean compareObjects(IFeatureModel o1, IFeatureModel o2) {
		final InternalFeatureModelFormat format = new InternalFeatureModelFormat();
		final String s1 = format.getInstance().write(o1);
		final String s2 = format.getInstance().write(o2);
		return s1.equals(s2);
	}

	@Override
	public void override() {
		localObject.setUndoContext(variableObject.getUndoContext());
		super.override();
	}

	@Override
	public IFeatureModelFormat getFormat() {
		return (IFeatureModelFormat) super.getFormat();
	}

	@Override
	protected IFeatureModel copyObject(IFeatureModel oldObject) {
		final IFeatureModel clone = oldObject.clone();
		clone.setUndoContext(oldObject.getUndoContext());
		return clone;
	}

	public static FileHandler<IFeatureModel> load(Path path) {
		final FileHandler<IFeatureModel> fileHandler = getFileHandler(path, objectCreator);
		fileHandler.getObject().setSourceFile(path);
		return fileHandler;
	}

	@Override
	public boolean externalSave(Runnable externalSaveMethod) {
		return true;
	}

	

	public FeatureModelSnapshot getSnapshot() {
		return status;
	}

	private FeatureModelSnapshot initStatus() {
		final IFeatureModel featureModel = getObject();
		status = new FeatureModelSnapshot(featureModel);
		return status;
	}

//	private void renameFeature(final IFeatureModel model, String oldName, String newName) {
//		for (IFileManager<Configuration> configurationManager : configurationManagerList) {
//			configurationManager.read();
//			configurationManager.save();
//		}
//	}
//
//	public IFileManager<Configuration> getConfigurationManager(Path path) {
//		IFileManager<Configuration> fileManager = ConfigurationManager.getInstance(path, new Configuration(getObject()));
//		if (fileManager != null && !configurationManagerList.contains(fileManager)) {
//			configurationManagerList.add(fileManager);
//		}
//		return fileManager;
//	}
//
//	public void addConfigurationManager(Collection<? extends IFileManager<Configuration>> managerList) {
//		configurationManagerList.addAll(managerList);
//	}
//
//	public void addConfigurationManager(IFileManager<Configuration> manager) {
//		configurationManagerList.add(manager);
//	}

	@Deprecated
	public FeatureModelAnalyzer getVarAnalyzer() {
		return new FeatureModelAnalyzer(new FeatureModelFormula(editObject()));
	}

	@Deprecated
	public static FeatureModelAnalyzer getAnalyzer(IFeatureModel featureModel) {
		final FeatureModelManager instance = getInstance(featureModel);
		return instance == null ? new FeatureModelAnalyzer(new FeatureModelFormula(featureModel)) : instance.getSnapshot().getAnalyzer();
	}

}
