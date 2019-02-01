package no.uio.ifi.eclipse.commands;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;

import de.christophseidl.util.eclipse.ResourceUtil;
import de.christophseidl.util.eclipse.ui.JFaceUtil;
import de.christophseidl.util.eclipse.ui.SelectionUtil;
import de.christophseidl.util.ecore.EcoreIOUtil;
import no.uio.ifi.llp.LowLevelProgram;
import no.uio.ifi.maudecompiler.LLPAndRM2MaudeCompiler;
import no.uio.ifi.rm.ResourceModel;

public class GenerateMaudeImplementationCommandHandler extends AbstractHandler {
	private LLPAndRM2MaudeCompiler compiler = new LLPAndRM2MaudeCompiler();
	
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		//Get files from selection.
		IFile lowLevelProgramFile = SelectionUtil.getFirstSelectedIFileWithExtension("llp");
		
		if (lowLevelProgramFile == null) {
			lowLevelProgramFile = SelectionUtil.getFirstSelectedIFileWithExtension("llpm");
		}
		
		IFile resourceModelFile = SelectionUtil.getFirstSelectedIFileWithExtension("rm");
		
		//Check input files.
		if (lowLevelProgramFile == null || !lowLevelProgramFile.exists()) {
			JFaceUtil.alertError("You have to select a low levle program file (*.llp or *.llpm).");
			return null;
		}
		
		LowLevelProgram lowLevelProgram = EcoreIOUtil.loadModel(lowLevelProgramFile);
		ResourceModel resourceModel = EcoreIOUtil.loadModel(resourceModelFile);
		
		//Live with defaults when no resource model is loaded.
		String maudeCode = resourceModel == null ? compiler.compile(lowLevelProgram) : compiler.compile(lowLevelProgram, resourceModel);
		
		//Synthesize file name and path
		IContainer container = lowLevelProgramFile.getParent();
		String name = ResourceUtil.getBaseFilename(lowLevelProgramFile) + (resourceModelFile != null ? "_" + ResourceUtil.getBaseFilename(resourceModelFile) : "") + ".maude";
		IFile maudeFile = container.getFile(new Path(name));

		
		ResourceUtil.writeToFile(maudeCode, maudeFile);

		JFaceUtil.alertInformation("Maude code generated to \"" + maudeFile.getFullPath() + "\".");
		
		return null;
	}

}
