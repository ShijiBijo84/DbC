/**
 * <copyright>
 * </copyright>
 *
 * 
 */
package no.uio.ifi.llp.resource.llp.mopp;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.emf.common.util.URI;

import no.uio.ifi.maudecompiler.LLPAndRM2MaudeCompiler;

public class LlpBuilder implements no.uio.ifi.llp.resource.llp.ILlpBuilder {
	private LLPAndRM2MaudeCompiler compiler = new LLPAndRM2MaudeCompiler();
	
	public boolean isBuildingNeeded(URI uri) {
		// Change this to return true to enable building of all resources.
		return true;
	}
	
	public IStatus build(no.uio.ifi.llp.resource.llp.mopp.LlpResource resource, IProgressMonitor monitor) {
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
