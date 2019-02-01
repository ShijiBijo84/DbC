/**
 * <copyright>
 * </copyright>
 *
 * 
 */
package no.uio.ifi.hlp.resource.hlp.mopp;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;

import no.uio.ifi.hlp.compiler.HLP2LLPCompiler;

public class HlpBuilder implements no.uio.ifi.hlp.resource.hlp.IHlpBuilder {
	private HLP2LLPCompiler compiler = new HLP2LLPCompiler();
	
	public boolean isBuildingNeeded(URI uri) {
		// Change this to return true to enable building of all resources.
		return true;
	}
	
	public IStatus build(no.uio.ifi.hlp.resource.hlp.mopp.HlpResource resource, IProgressMonitor monitor) {
		compiler.compile(resource);
		return Status.OK_STATUS;
	}
	
	/**
	 * Handles the deletion of the given resource.
	 */
	public IStatus handleDeletion(URI uri, IProgressMonitor monitor) {
		// by default nothing is done when a resource is deleted
		return Status.OK_STATUS;
	}
	
}
