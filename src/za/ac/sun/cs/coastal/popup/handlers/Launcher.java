package za.ac.sun.cs.coastal.popup.handlers;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.jdt.core.IClasspathEntry;
import org.eclipse.jdt.core.IJavaProject;
import org.eclipse.jdt.launching.IJavaLaunchConfigurationConstants;
import org.eclipse.jdt.launching.JavaRuntime;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.handlers.HandlerUtil;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;

import za.ac.sun.cs.coastal.popup.Activator;
import za.ac.sun.cs.coastal.popup.preferences.PreferenceConstants;

import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.jface.viewers.IStructuredSelection;

public class Launcher extends AbstractHandler {

	protected static final Bundle BUNDLE = FrameworkUtil.getBundle(Launcher.class);

	protected static final ILog LOGGER = Platform.getLog(BUNDLE);

	protected static final String COASTAL_CLASS = "za.ac.sun.cs.coastal.COASTAL";

	protected static final String COASTAL_PREFS = "coastal-popup.preferences.CoastalPopupPage";

	public String getCoastalOptions() {
		return "-quiet";
	}

	public String getVmOptions() {
		return "-ea";
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		log("COASTAL pop-up: start");
		IPreferenceStore store = Activator.getDefault().getPreferenceStore();
		String coastalLibrary = store.getString(PreferenceConstants.P_COASTAL_INSTALL_DIRECTORY);
		IWorkbenchWindow window = HandlerUtil.getActiveWorkbenchWindowChecked(event);
		ISelectionService service = window.getSelectionService();
		IStructuredSelection structured = (IStructuredSelection) service.getSelection();
		if (structured.getFirstElement() instanceof IFile) {
			IFile file = (IFile) structured.getFirstElement();
			IProject project = file.getProject();
			String resourcePath = file.getLocation().toPortableString();
			log("COASTAL pop-up: project=" + project.getFullPath().toPortableString());
			log("COASTAL pop-up: resourcePath=" + resourcePath);
			try {
				ILaunchManager manager = DebugPlugin.getDefault().getLaunchManager();
				ILaunchConfigurationType type = manager
						.getLaunchConfigurationType(IJavaLaunchConfigurationConstants.ID_JAVA_APPLICATION);
				ILaunchConfigurationWorkingCopy copy = type.newInstance(null, "COASTAL-TEMP");
				copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_USE_CLASSPATH_ONLY_JAR, false);
				copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_USE_START_ON_FIRST_THREAD, true);
				List<String> classpaths = new ArrayList<String>();
				classpaths.add(String.format(
						"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><runtimeClasspathEntry id=\"org.eclipse.jdt.launching.classpathentry.variableClasspathEntry\"><memento path=\"3\" variableString=\"%s/*\"/></runtimeClasspathEntry>",
						coastalLibrary));
				classpaths.add(String.format(
						"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><runtimeClasspathEntry containerPath=\"org.eclipse.jdt.launching.JRE_CONTAINER/org.eclipse.jdt.internal.debug.ui.launcher.StandardVMType/JavaSE-1.8\" javaProject=\"%s\" path=\"1\" type=\"4\"/>",
						project.getName()));
				classpaths.add(String.format(
						"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><runtimeClasspathEntry id=\"org.eclipse.jdt.launching.classpathentry.defaultClasspath\"><memento exportedEntriesOnly=\"false\" project=\"%s\"/></runtimeClasspathEntry>",
						project.getName()));
				copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, classpaths);
				copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_EXCLUDE_TEST_CODE, false);
				copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_DEFAULT_CLASSPATH, false);
				copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_MAIN_TYPE_NAME, COASTAL_CLASS);
				copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROGRAM_ARGUMENTS,
						resourcePath + " " + getCoastalOptions());
				copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_PROJECT_NAME, project.getName());
				copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_VM_ARGUMENTS, getVmOptions());
				IJavaProject proj = JavaRuntime.getJavaProject(copy);
				for (IClasspathEntry entry : proj.getRawClasspath()) {
					if (entry.getEntryKind() == IClasspathEntry.CPE_SOURCE) {
						IPath path = entry.getOutputLocation();
						if (path != null) {
							classpaths.add(String.format(
									"<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?><runtimeClasspathEntry id=\"org.eclipse.jdt.launching.classpathentry.variableClasspathEntry\"><memento path=\"3\" variableString=\"%s\"/></runtimeClasspathEntry>",
									path.toPortableString()));
						}
					}
				}
				copy.setAttribute(IJavaLaunchConfigurationConstants.ATTR_CLASSPATH, classpaths);
				copy.doSave().launch(ILaunchManager.RUN_MODE, null);
			} catch (CoreException e) {
				log("COASTAL pop-up: CoreException", e);
			}
		}
		return null;
	}

	public static void log(String msg) {
		log(msg, null);
	}

	public static void log(String msg, Exception e) {
		LOGGER.log(new Status((e == null ? Status.INFO : Status.ERROR), BUNDLE.getSymbolicName(), msg, e));
	}
}
