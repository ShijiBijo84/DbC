/**
 * <copyright>
 * </copyright>
 *
 * 
 */
package no.uio.ifi.llp.resource.llpm.analysis;

import java.util.Map;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;

public class LlpmIDENTIFIER_TOKENTokenResolver implements no.uio.ifi.llp.resource.llpm.ILlpmTokenResolver {
	
	private no.uio.ifi.llp.resource.llpm.analysis.LlpmDefaultTokenResolver defaultTokenResolver = new no.uio.ifi.llp.resource.llpm.analysis.LlpmDefaultTokenResolver(true);
	
	public String deResolve(Object value, EStructuralFeature feature, EObject container) {
		// By default token de-resolving is delegated to the DefaultTokenResolver.
//		String result = defaultTokenResolver.deResolve(value, feature, container, null, null, null);
//		return result;
		
		String stringValue = (String) value;

		return "'" + stringValue;
	}
	
	public void resolve(String lexem, EStructuralFeature feature, no.uio.ifi.llp.resource.llpm.ILlpmTokenResolveResult result) {
		if (lexem != null && lexem.startsWith("'")) {
			result.setResolvedToken(lexem.substring(1));
		}
		
		// By default token resolving is delegated to the DefaultTokenResolver.
//		defaultTokenResolver.resolve(lexem, feature, result, null, null, null);
	}
	
	public void setOptions(Map<?,?> options) {
		defaultTokenResolver.setOptions(options);
	}
	
}
