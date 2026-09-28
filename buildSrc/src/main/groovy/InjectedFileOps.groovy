import javax.inject.Inject
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.model.ObjectFactory

/**
 * Helper used by build scripts to perform file-system operations from doLast/doFirst blocks.
 * Project.copy/Project.delete/Project.fileTree/Project.zipTree (and the implicit
 * `copy`/`delete`/`fileTree`/`zipTree` in a build script) reference the Project, a Gradle script
 * object, at execution time, which is not supported with the configuration cache. The services
 * injected via this interface are safe to use from task actions:
 * <ul>
 *   <li>{@link FileSystemOperations} ({@code fs}) for copy/delete/sync;
 *   <li>{@link ObjectFactory} ({@code objects}) for creating file collections and trees
 *       (objects.fileTree(), objects.fileCollection());
 *   <li>{@link ArchiveOperations} ({@code archives}) for zipTree/tarTree.
 * </ul>
 *
 * This is the file-operation analogue of {@link InjectedExecOps}.
 *
 * Usage:
 *   def injectedFiles = project.objects.newInstance(InjectedFileOps)
 *   def srcDir = file('some/dir')           // resolve script-object values at configuration time
 *   doFirst {
 *       def tree = injectedFiles.objects.fileTree().setDir(srcDir)
 *       injectedFiles.fs.copy { from(srcDir); into(dest) }
 *   }
 */
interface InjectedFileOps {
  @Inject
  FileSystemOperations getFs()

  @Inject
  ObjectFactory getObjects()

  @Inject
  ArchiveOperations getArchives()
}
