package no.uio.ifi.maudecompiler;

import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.common.util.URI;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;
import org.eclipse.emf.ecore.resource.ResourceSet;
import org.eclipse.emf.ecore.resource.impl.ResourceSetImpl;
import org.eclipse.emf.ecore.util.EcoreUtil;

import de.christophseidl.util.eclipse.ResourceUtil;
import de.christophseidl.util.ecore.EcoreIOUtil;
import no.uio.ifi.llp.LowLevelProgram;
import no.uio.ifi.llp.resource.llpm.util.LlpmResourceUtil;
import no.uio.ifi.rm.Device;
import no.uio.ifi.rm.Memory;
import no.uio.ifi.rm.MemoryCellReference;
import no.uio.ifi.rm.ResourceModel;
import no.uio.ifi.rm.VariableReference;

//Poor man's string template engine...
public class LLPAndRM2MaudeCompiler {
	private static final String NEWLINE = "\n";

	private static ResourceModel loadDefaultResourceModel() {
		URI uri = URI.createPlatformPluginURI("no.uio.ifi.maudecompiler/resources/Default.rm", true);
		return EcoreIOUtil.loadModel(uri);
	}
	
	public void compile(Resource lowLevelProgramResource) {
		List<EObject> contents = lowLevelProgramResource.getContents();
		
		if (contents == null || contents.isEmpty()) {
			throw new UnsupportedOperationException();
		}
		
		LowLevelProgram lowLevelProgram = (LowLevelProgram) contents.get(0);
		
		String maudeCode = compile(lowLevelProgram);
		
		IFile lowLevelProgramFile = EcoreIOUtil.getFile(lowLevelProgramResource);
		IFile maudeFile = ResourceUtil.deriveFile(lowLevelProgramFile, "maude");

		ResourceUtil.writeToFile(maudeCode, maudeFile);
	}
	
	public String compile(LowLevelProgram lowLevelProgram) {
		return compile(lowLevelProgram, loadDefaultResourceModel());
	}
	
	public String compile(LowLevelProgram lowLevelProgram, ResourceModel resourceModel) {
		final String moduleName = "CORE-TEST";
		final String programName = "program";
		
		Memory memory = resourceModel.getMemory();
		List<Device> devices = resourceModel.getDevices();
		
		
		String output = "";
		
		//Header
		output += "load locks-local-rules-list2.maude" + NEWLINE;
		
		output += NEWLINE;
		
		output += "mod " + moduleName + " is" + NEWLINE;
	    output += "inc RANDOM ." + NEWLINE;
		output += "inc RULES-2 ." + NEWLINE;
		
		//Create variables for the cores of all devices.
		for (int i = 0; i < devices.size(); i++) {
			output +=  "C" + (i + 1) + " ";
		}
		output += "ops Tbl M Sch Ta MT : -> Oid [ctor] ." + NEWLINE;
		
		output += "ops " + programName + " cc : -> Configuration ." + NEWLINE;
		output += "op init : Configuration Int -> Configuration ." + NEWLINE;
		
		output += NEWLINE;
		
		output += "op changeList : List{Object} Int -> List{Object} ." + NEWLINE;
		output += "op changeList : List{Object} Int Int List{Object} -> List{Object} ." + NEWLINE;
		
		output += NEWLINE;
		
		output += "var OList1 OList2 : List{Object} ." + NEWLINE;
		output += "var config0 : Configuration ." + NEWLINE;
		output += "var obj : Object ." + NEWLINE;
		output += "var x y k : Int ." + NEWLINE;
		
		output += NEWLINE;
		
	    output += "eq init(config0 OList1, k) = (config0 changeList(OList1, (random(k) rem size(OList1) + 1))) ." + NEWLINE;
		
		output += "eq  changeList((obj | OList1), x) = changeList((obj | OList1), x, 1, nil) ." + NEWLINE;
		output += "ceq changeList((obj | OList1), x, y, OList2) =  changeList(OList1, x, y + 1, (OList2 | obj)) if x > y ." + NEWLINE;
		output += "ceq changeList((obj | OList1), x, y, OList2) = obj | OList2 | OList1  if x == y ." + NEWLINE;
		output += "eq changeList(nil, x, y, OList2) = OList2 ." + NEWLINE;
		
		
		
		//Create entries for all cores.
		output += "eq " + programName + " =" + NEWLINE;
		output += "(";
		
		for (int i = 0; i < devices.size(); i++) {
			if (i > 0) {
				output += " |" + NEWLINE;
			}
			output += "< C" + (i + 1) + " : CR | Rst: nil, InCount: 0.0, HitPer: 0.0, MissPer: 0.0 >";
		}
		output += ")" + NEWLINE;
		output += NEWLINE;
		
		
		//Low level program in Maude syntax
		output += "< Ta : Task | Data: " + printLowLevelProgramInMaudeSyntax(lowLevelProgram) + ">" + NEWLINE;
		output += NEWLINE;

		
		output += "< Sch : Qu | TidSet: 'main >" + NEWLINE;
		
		//Create a cache for each core with the respective configuration.
		int i = 0;
		for (Device device : devices) {
			int cacheSize = device.getCacheSize();

			MemoryCellReference memoryCellReference = device.getLocalMemoryCellReference();
			int startCellIndex = memoryCellReference == null ? 0 : memoryCellReference.getStartCellIndex();
			int endCellIndex = memoryCellReference == null ? 0 : memoryCellReference.getEndCellIndex();
			
			output += "< CaId(C" + (i + 1) + ") : Cache | CM: empty, D: nil, CacheSz: " + cacheSize + ", Assoc: Assoc(1), Hits: 0.0, Miss: 0.0, Penalty: 0, Range: mem(" + startCellIndex + ", " + endCellIndex + ") >" + NEWLINE;
			
			i++;
		}
		output += NEWLINE;					  
		
		
		//Variable references.
		List<VariableReference> variableReferences = memory.getVariableReferences();
		output += "< Tbl : TBL | Addr: "; 
		boolean isFirst = true;
		
		for (VariableReference variableReference : variableReferences) {
			String variableName = variableReference.getVariable();
			int memoryCellIndex = variableReference.getMemoryCellIndex();
			
			if (!isFirst) {
				output += ", ";
			}
			
			output += "ref('" + variableName + ") |-> " + memoryCellIndex;
			
			isFirst = false;
		}
		
		output += ">" + NEWLINE;
		output += NEWLINE;
		
		
		//Create initial entries for memory layout (all have to be 0).
		int memorySize = memory.getSize();
		
		output += "< M : MM | M: ";
		for (int j = 0; j < memorySize; j++) {
			if (j > 0) {
				output += ", ";
			}
			
			output += j + " |-> (0, sh)";
		}
		
//		List<MemoryCell> memoryCells = memory.getMemoryCells();
//		int j = 0;
//		boolean isFirst = true;
//		
//		output += "< M : MM | M: ";
//		for (MemoryCell memoryCell : memoryCells) {
//			Integer content = memoryCell.getContent();
//			
//			if (content != null) {
//				if (!isFirst) {
//					output += ", ";
//				}
//				
//				output += j + " |-> (" + content + ", sh)";
//				
//				isFirst = false;
//			}
//			
//			j++;
//		}
		
		output += ", fetchCount: 0, Penalty: 0 > ." + NEWLINE;		     
		output += NEWLINE;
		
		output += "endm" + NEWLINE;	
	
		return output;
	}
	
	private String printLowLevelProgramInMaudeSyntax(LowLevelProgram lowLevelProgram) {
		//Have to ensure that the low level program is in the right type of resource for printing.
		//To not destroy the original model's resource association, a defensive copy is created.
		LowLevelProgram copiedLowLevelProgram = EcoreUtil.copy(lowLevelProgram);
		ResourceSet resourceSet = new ResourceSetImpl();
		Resource resource = resourceSet.createResource(URI.createFileURI("temp.llpm"));
		resource.getContents().add(copiedLowLevelProgram);
		
		return LlpmResourceUtil.getText(copiedLowLevelProgram);
	}
}
