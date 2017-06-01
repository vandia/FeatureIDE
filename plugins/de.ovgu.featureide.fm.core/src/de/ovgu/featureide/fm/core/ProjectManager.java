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
package de.ovgu.featureide.fm.core;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.annotation.CheckForNull;

import de.ovgu.featureide.fm.core.base.IFeatureModel;
import de.ovgu.featureide.fm.core.configuration.Configuration;
import de.ovgu.featureide.fm.core.io.ProblemList;
import de.ovgu.featureide.fm.core.io.manager.ConfigurationManager;
import de.ovgu.featureide.fm.core.io.manager.FeatureModelManager;
import de.ovgu.featureide.fm.core.io.manager.FileManagerMap;
import de.ovgu.featureide.fm.core.io.manager.IFileManager;
import de.ovgu.featureide.fm.core.io.manager.VirtualFileManager;
import de.ovgu.featureide.fm.core.io.xml.XmlFeatureModelFormat;
import de.ovgu.featureide.fm.core.job.LongRunningMethod;
import de.ovgu.featureide.fm.core.job.LongRunningWrapper;
import de.ovgu.featureide.fm.core.job.util.JobArguments;
import de.ovgu.featureide.fm.core.job.util.JobSequence;

/**
 * 
 * @author Sebastian Krieter
 */
public final class ProjectManager {

	private static final HashMap<Path, FeatureProject> pathFeatureProjectMap = new HashMap<>();
	private static final HashMap<IFeatureModel, FeatureProject> modelFeatureProjectMap = new HashMap<>();
	private static final HashMap<IFeatureModel, Path> modelPathMap = new HashMap<>();

	private ProjectManager() {
	}

	/**
	 * Creates a {@link LongRunningMethod} for every project with the given arguments.
	 * 
	 * @param projects the list of projects
	 * @param arguments the arguments for the job
	 * @param autostart if {@code true} the jobs is started automatically.
	 * @return the created job or a {@link JobSequence} if more than one project is given.
	 *         Returns {@code null} if {@code projects} is empty.
	 */
	public static LongRunningMethod<?> startJobs(List<JobArguments<?>> projects, boolean autostart) {
		LongRunningMethod<?> ret;
		switch (projects.size()) {
		case 0:
			return null;
		case 1:
			LongRunningMethod<?> newJob = projects.get(0).createJob();
			ret = newJob;
			break;
		default:
			final JobSequence jobSequence = new JobSequence();
			jobSequence.setIgnorePreviousJobFail(true);
			for (JobArguments<?> p : projects) {
				LongRunningMethod<?> newSequenceJob = p.createJob();
				jobSequence.addJob(newSequenceJob);
			}
			ret = jobSequence;
		}
		if (autostart) {
			LongRunningWrapper.getRunner(ret).schedule();
		}
		return ret;
	}

	public static void addProject(Path root, Path featureModelFile, Path configurations) {
		synchronized (pathFeatureProjectMap) {
			if (pathFeatureProjectMap.containsKey(root)) {
				return;
			}
			final IFileManager<IFeatureModel> featureModelManager = FeatureModelManager.getInstance(featureModelFile);
			final ArrayList<IFileManager<Configuration>> configurationManagerList = new ArrayList<>();
			final IFeatureModel featureModel = featureModelManager.getObject();
			try {
				Files.walkFileTree(configurations, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
						final Configuration c = new Configuration(featureModel);
						ConfigurationManager configurationManager = FileManagerMap.<Configuration, ConfigurationManager> getInstance(file.toString());
						if (configurationManager != null) {
							configurationManager.setConfiguration(c);
							configurationManager.read();
						} else {
							configurationManager = ConfigurationManager.getInstance(c, file.toString());
						}

						final ProblemList lastProblems = configurationManager.getLastProblems();
						if (lastProblems.containsError()) {
							FileManagerMap.remove(file.toString());
						} else {
							configurationManagerList.add(configurationManager);
						}
						return FileVisitResult.CONTINUE;
					}
				});
			} catch (IOException e) {
				Logger.logError(e);
			}
			FeatureProject data = new FeatureProject(featureModelManager, configurationManagerList);
			pathFeatureProjectMap.put(root, data);
			modelFeatureProjectMap.put(featureModel, data);
			modelPathMap.put(featureModel, root);
		}
	}

	public static void addProject(IFeatureModel featureModel) {
		synchronized (pathFeatureProjectMap) {
			if (modelFeatureProjectMap.containsKey(featureModel)) {
				return;
			}
			modelFeatureProjectMap.put(featureModel, createVirtualFeatureProject(featureModel));
		}
	}

	private static FeatureProject createVirtualFeatureProject(IFeatureModel featureModel) {
		final IFileManager<IFeatureModel> featureModelManager = new VirtualFileManager<>(featureModel, new XmlFeatureModelFormat());
		final ArrayList<IFileManager<Configuration>> configurationManagerList = new ArrayList<>();
		FeatureProject data = new FeatureProject(featureModelManager, configurationManagerList);
		return data;
	}

	@CheckForNull
	public static FeatureProject removeProject(Path root) {
		synchronized (pathFeatureProjectMap) {
			final FeatureProject project = pathFeatureProjectMap.remove(root);
			if (project != null) {
				modelPathMap.remove(root);
				modelFeatureProjectMap.remove(project.getFeatureModel());
			}
			return project;
		}
	}

	@CheckForNull
	public static FeatureProject removeProject(IFeatureModel featureModel) {
		synchronized (pathFeatureProjectMap) {
			final FeatureProject project = modelFeatureProjectMap.remove(featureModel);
			if (project != null) {
				final Path path = modelPathMap.get(featureModel);
				if (path != null) {
					pathFeatureProjectMap.remove(path);
				}
			}
			return project;
		}
	}

	/**
	 * returns an unmodifiable Collection of all ProjectData items, or <code>null</code> if plugin is not loaded
	 * 
	 * @return
	 */
	public static Collection<FeatureProject> getFeatureProjects() {
		synchronized (pathFeatureProjectMap) {
			return Collections.unmodifiableCollection(modelFeatureProjectMap.values());
		}
	}

	/**
	 * returns the ProjectData object associated with the given resource
	 * 
	 * @param res
	 * @return <code>null</code> if there is no associated project, no active
	 *         instance of this plug-in or resource is the workspace root
	 */
	@CheckForNull
	public static FeatureProject getProject(Path path) {
		synchronized (pathFeatureProjectMap) {
			return pathFeatureProjectMap.get(path);
		}
	}

	@CheckForNull
	public static FeatureProject getProject(IFeatureModel model) {
		synchronized (pathFeatureProjectMap) {
			return modelFeatureProjectMap.get(model);
		}
	}

	public static boolean hasProjectData(Path path) {
		synchronized (pathFeatureProjectMap) {
			return pathFeatureProjectMap.containsKey(path);
		}
	}

	public static boolean hasProjectData(IFeatureModel model) {
		synchronized (pathFeatureProjectMap) {
			return pathFeatureProjectMap.containsKey(model);
		}
	}

	public static FeatureModelAnalyzer getAnalyzer(IFeatureModel featureModel) {
		FeatureProject project = getProject(featureModel);
		if (project == null) {
			project = createVirtualFeatureProject(featureModel);
		}
		return project.getAnalyzer();
	}

}
