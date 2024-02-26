package fr.crazycat256.subclassrenamer;

import software.coley.recaf.info.ClassInfo;
import software.coley.recaf.workspace.model.Workspace;

import static fr.crazycat256.subclassrenamer.SubclassRenamer.INDEX_PLACEHOLDER;
import static fr.crazycat256.subclassrenamer.SubclassRenamer.NAME_PLACEHOLDER;


/**
 * Name generator that creates names based on a pattern.
 * Copied from Recaf's <a href="https://github.com/Recaf-Plugins/Auto-Renamer/blob/master/src/main/java/me/coley/recaf/plugin/rename/NameGenerator.java">AutoRename Plugin</a>
 *
 * @author Matt Coley
 */
public class NameGenerator {
	private final Workspace workspace;
	private final ClassInfo superClass;
	private final String pattern;
	private int idx = 0;

	/**
	 * @param superClass
	 * 		The super class to base names off of.
	 * @param pattern
	 * 		The pattern to use for naming.
	 */
	public NameGenerator(Workspace workspace, ClassInfo superClass, String pattern) {
		this.workspace = workspace;
		this.superClass = superClass;
		this.pattern = pattern;
	}

	/**
	 * @return {@code true} when the name generation implementation is safe to be executed with multiple threads.
	 */
	public boolean allowMultiThread() {
		return !pattern.contains(INDEX_PLACEHOLDER);
	}

	/**
	 * @param info
	 * 		Class to rename.
	 *
	 * @return New internal name, or {@code null} if the naming scope does not apply to the class.
	 */
	public String createClassName(ClassInfo info) {

		// Increment the index for the class.
		idx++;

		// Create the new name for the class.
		String nodeSimpleName = info.getName().substring(info.getName().lastIndexOf('/') + 1);
		String newName = pattern.replace(NAME_PLACEHOLDER, nodeSimpleName).replace(INDEX_PLACEHOLDER, String.valueOf(idx));

		// Check if the new name already exists.
		if (workspace.findClass(newName) != null) {
			return null;
		}

		// Return the new name
        return newName;
	}
}
