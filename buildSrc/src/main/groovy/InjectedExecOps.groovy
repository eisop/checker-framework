import javax.inject.Inject
import org.gradle.process.ExecOperations

/**
 * Helper used by build scripts to invoke external processes from doLast/doFirst blocks
 * (Project.exec/javaexec are not safe for configuration cache; ExecOperations injected via
 * this interface is).
 *
 * Usage:
 *   def injected = project.objects.newInstance(InjectedExecOps)
 *   doLast {
 *       injected.execOps.exec { ... }
 *   }
 */
interface InjectedExecOps {
  @Inject
  ExecOperations getExecOps()
}
