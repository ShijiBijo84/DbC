package no.uio.ifi.hlp.compiler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IFile;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.resource.Resource;

import de.christophseidl.util.eclipse.ResourceUtil;
import de.christophseidl.util.ecore.EcoreIOUtil;
import de.christophseidl.util.java.StringUtil;
import no.uio.ifi.hlp.AddExpression;
import no.uio.ifi.hlp.Assignment;
import no.uio.ifi.hlp.BinaryExpression;
import no.uio.ifi.hlp.Block;
import no.uio.ifi.hlp.Condition;
import no.uio.ifi.hlp.DivideExpression;
import no.uio.ifi.hlp.Expression;
import no.uio.ifi.hlp.ExpressionStatement;
import no.uio.ifi.hlp.ForLoop;
import no.uio.ifi.hlp.HighLevelProgram;
import no.uio.ifi.hlp.IfStatement;
import no.uio.ifi.hlp.LiteralValue;
import no.uio.ifi.hlp.MultiplyExpression;
import no.uio.ifi.hlp.ParenthesisExpression;
import no.uio.ifi.hlp.ScheduleInstruction;
import no.uio.ifi.hlp.Statement;
import no.uio.ifi.hlp.SubtractExpression;
import no.uio.ifi.hlp.SynchronizedStatement;
import no.uio.ifi.hlp.Task;
import no.uio.ifi.hlp.Variable;
import no.uio.ifi.hlp.VariableDeclaration;
import no.uio.ifi.hlp.VariableDeclarationScope;
import no.uio.ifi.hlp.VariableReference;
import no.uio.ifi.llp.ControlFlowBranchingInstruction;
import no.uio.ifi.llp.DataAccessPattern;
import no.uio.ifi.llp.LLPFactory;
import no.uio.ifi.llp.LockInstruction;
import no.uio.ifi.llp.LowLevelProgram;
import no.uio.ifi.llp.MemoryReference;
import no.uio.ifi.llp.ParenthesisInstruction;
import no.uio.ifi.llp.ReadInstruction;
import no.uio.ifi.llp.RepetitionInstruction;
import no.uio.ifi.llp.SkipInstruction;
import no.uio.ifi.llp.SpawnInstruction;
import no.uio.ifi.llp.UnlockInstruction;
import no.uio.ifi.llp.WriteInstruction;

public class HLP2LLPCompiler {
	private static final LLPFactory factory = LLPFactory.eINSTANCE;
	
	private Map<List<String>, String> variableSetsToLockNamesMap = new LinkedHashMap<List<String>, String>();
	
	public LowLevelProgram compile(Resource programResource) {
		List<EObject> contents = programResource.getContents();
		
		if (contents == null || contents.isEmpty()) {
			throw new UnsupportedOperationException();
		}
		
		HighLevelProgram highLevelProgram = (HighLevelProgram) contents.get(0);
		IFile highLevelProgramFile = EcoreIOUtil.getFile(programResource);
		
		LowLevelProgram lowLevelProgram = compile(highLevelProgram);
		
		IFile intermediateLanguageFile = ResourceUtil.deriveFile(highLevelProgramFile, "llp");
		EcoreIOUtil.saveModelAs(lowLevelProgram, intermediateLanguageFile);
		
		return lowLevelProgram;
	}
	
	public LowLevelProgram compile(HighLevelProgram highLevelProgram) {
		initialize();
		LowLevelProgram lowLevelProgram = compileHighLevelProgram(highLevelProgram);
		
		//Debug
		dumpLockMap();
		
		return lowLevelProgram;
	}
	
	protected void initialize() {
		variableSetsToLockNamesMap.clear();
	}
	
	protected LowLevelProgram compileHighLevelProgram(HighLevelProgram highLevelProgram) {
		LowLevelProgram lowLevelProgram = factory.createLowLevelProgram();
		
		//Assemble main block
		no.uio.ifi.llp.Block mainBlock = factory.createBlock();
		lowLevelProgram.setMainBlock(mainBlock);
		
		List<DataAccessPattern> dataAccessPatterns = mainBlock.getDataAccessPatterns();

		//Add global variable declarations
		dataAccessPatterns.addAll(compileVariableDeclarationScope(highLevelProgram));
		
		
		//Assemble program tasks
		List<Task> hlpTasks = highLevelProgram.getTasks();
		List<no.uio.ifi.llp.Task> llpTasks = lowLevelProgram.getTasks();
		Map<Task, no.uio.ifi.llp.Task> taskMap = new HashMap<Task, no.uio.ifi.llp.Task>();
		
		for (Task hlpTask : hlpTasks) {
			no.uio.ifi.llp.Task llpTask = compileTask(hlpTask);
			
			//Add each task to the task declarations.
			llpTasks.add(llpTask);
			
			//Save mapping between HLP and LLP tasks for later
			taskMap.put(hlpTask, llpTask);
		}
		
		
		ScheduleInstruction scheduleInstruction = highLevelProgram.getScheduleInstruction();
		
		if (scheduleInstruction != null) {
			List<Task> scheduledHLPTasks = scheduleInstruction.getTasks();
			
			//Spawn each task in the schedule instruction.
			for (Task scheduleHLPTask : scheduledHLPTasks) {
				//Resolve LLP task from HLP task.
				no.uio.ifi.llp.Task scheduledLLPTask = taskMap.get(scheduleHLPTask);
				
				SpawnInstruction spawnInstruction = factory.createSpawnInstruction();
				spawnInstruction.setTask(scheduledLLPTask);
				dataAccessPatterns.add(spawnInstruction);
			}
		}
		
		return lowLevelProgram;
	}
	
	protected no.uio.ifi.llp.Task compileTask(Task programFragment) {
		String name = programFragment.getName();
		
		no.uio.ifi.llp.Task task = factory.createTask();
		task.setName(name);
		
		no.uio.ifi.llp.Block taskBlock = factory.createBlock(); 
		task.setBlock(taskBlock);
		List<DataAccessPattern> dataAccessPatterns = taskBlock.getDataAccessPatterns();
		
		dataAccessPatterns.addAll(compileVariableDeclarationScope(programFragment));
		
		Block block = programFragment.getBlock();
		dataAccessPatterns.addAll(unwrapBlock(compileBlock(block)));
		
		return task;
	}
	
	
	protected List<DataAccessPattern> compileVariableDeclarationScope(VariableDeclarationScope variableDeclarationScope) {
		List<DataAccessPattern> result = createDefaultList();
		
		List<VariableDeclaration> variableDeclarations = variableDeclarationScope.getVariableDeclarations();
		
		for (VariableDeclaration variableDeclaration : variableDeclarations) {
			result.addAll(compileVariableDeclaration(variableDeclaration));
		}

		return result;
	}
	
	protected List<DataAccessPattern> compileVariableDeclaration(VariableDeclaration variableDeclaration) {
		List<DataAccessPattern> result = createDefaultList();
		
		Variable variable = variableDeclaration.getVariable();

		Expression initialValue = variableDeclaration.getInitialValue();

		if (initialValue != null) {
			result.addAll(compileExpression(initialValue));
		}
		
		//Either it gets an explicit value or it would be initialized to zero.
		result.add(createWriteInstruction(variable));
		
		return result;
	}

	protected no.uio.ifi.llp.Block compileBlock(Block block) {
		return compileBlock(block, false);
	}
	
	protected no.uio.ifi.llp.Block compileBlock(Block block, boolean addSkipIfBlockMissing) {
		no.uio.ifi.llp.Block llpBlock = factory.createBlock();
		List<DataAccessPattern> dataAccessPatterns = llpBlock.getDataAccessPatterns();
		
		if (block != null) {
			List<Statement> statements = block.getStatements();
			
			for (Statement statement : statements) {
				dataAccessPatterns.addAll(compileStatement(statement));
			}
		} else if (addSkipIfBlockMissing) {
			dataAccessPatterns.add(createSkipInstruction());
		}
		
		return llpBlock;
	}
	
	protected List<DataAccessPattern> compileStatement(Statement statement) {
		List<DataAccessPattern> result = createDefaultList();
		
		if (statement instanceof Assignment) {
			Assignment assignment = (Assignment) statement;
			result.addAll(compileAssignment(assignment));
			return result;
		}
		
		if (statement instanceof IfStatement) {
			IfStatement ifStatement = (IfStatement) statement;
			result.addAll(compileIfStatement(ifStatement));
			return result;
		}
		
		//Cannot determine number of repetitions. Disabled for now.
//		if (statement instanceof WhileLoop) {
//			WhileLoop whileLoop = (WhileLoop) statement;
//			result.addAll(compileWhileLoop(whileLoop));
//			return result;
//		}
		
		if (statement instanceof ForLoop) {
			ForLoop forLoop = (ForLoop) statement;
			result.addAll(compileForLoop(forLoop));
			return result;
		}
		
		if (statement instanceof SynchronizedStatement) {
			SynchronizedStatement synchronizedStatement = (SynchronizedStatement) statement;
			result.addAll(compileSynchronizedStatement(synchronizedStatement));
			return result;
		}
		
		if (statement instanceof ExpressionStatement) {
			ExpressionStatement expressionStatement = (ExpressionStatement) statement;
			Expression expression = expressionStatement.getExpression();
			
			result.addAll(compileExpression(expression));
			return result;
		}
		
		throw new UnsupportedOperationException();
	}
	
	protected List<DataAccessPattern> compileAssignment(Assignment assignment) {
		List<DataAccessPattern> result = createDefaultList();
		
		VariableReference leftHandSide = assignment.getLeftHandSide();
		Expression rightHandSide = assignment.getRightHandSide();
		
		result.addAll(compileExpression(rightHandSide));
		result.add(createWriteInstruction(leftHandSide));
		
		return result;
	}
	
	protected List<DataAccessPattern> compileIfStatement(IfStatement ifStatement) {
		List<DataAccessPattern> result = createDefaultList();
		
		Condition condition = ifStatement.getCondition();
		Block thenBlock = ifStatement.getThenBlock();
		Block elseBlock = ifStatement.getElseBlock();

		result.addAll(compileCondition(condition));
		
		ControlFlowBranchingInstruction controlFlowBranchingInstruction = factory.createControlFlowBranchingInstruction();
		
		no.uio.ifi.llp.Block llpLeftHandSideBlock = compileBlock(thenBlock);
		controlFlowBranchingInstruction.setLeftHandSideBlock(llpLeftHandSideBlock);
		
		//Automatically adds a skip instruction if else block in HLP was missing.
		no.uio.ifi.llp.Block llpRightHandSideBlock = compileBlock(elseBlock, true);
		controlFlowBranchingInstruction.setRightHandSideBlock(llpRightHandSideBlock);
		
		result.add(controlFlowBranchingInstruction);
		
		return result;
	}
	
	protected List<DataAccessPattern> compileCondition(Condition condition) {
		List<DataAccessPattern> result = createDefaultList();
		
		Expression leftHandSide = condition.getLeftHandSide();
		Expression rightHandSide = condition.getRightHandSide();
		
		//Just acre about the expressions here, not the actual comparison.
		result.addAll(compileExpression(leftHandSide));
		result.addAll(compileExpression(rightHandSide));
		
		return result;
	}
	
//	protected List<DataAccessPattern> compileWhileLoop(WhileLoop whileLoop) {
//		List<DataAccessPattern> result = createDefaultList();
//		
//		Condition condition = whileLoop.getCondition();
//		Block block = whileLoop.getBlock();
//
//		//Condition as part of repetition because it is being checked each loop
//		List<DataAccessPattern> intermediateResult = createDefaultList();
//		intermediateResult.addAll(compileCondition(condition));
//		
//		RepetitionInstruction repetitionInstruction = factory.createRepetitionInstruction();
//		no.uio.ifi.llp.Block loopBlock = compileBlock(block);
//		no.uio.ifi.llp.Block repetitionBlock = loopBlock;
//		repetitionBlock.getDataAccessPatterns().addAll(0, intermediateResult);
//		repetitionInstruction.setBlock(repetitionBlock);
//		
//		result.add(repetitionInstruction);
//		
//		return result;
//	}

	protected List<DataAccessPattern> compileForLoop(ForLoop forLoop) {
		List<DataAccessPattern> result = createDefaultList();
		
		VariableReference variableReference = forLoop.getVariableReference();
		Variable variable = variableReference.getVariable();
		
		Expression bound1 = forLoop.getBound1();
		Expression bound2 = forLoop.getBound2();
		boolean incrementing = forLoop.isIncrementing();
		Block block = forLoop.getBlock();

		//Initialization of variable.
		result.addAll(compileExpression(bound1));
		result.add(createWriteInstruction(variable));
		
		//Check for loop variable (first thing in repetition).
		List<DataAccessPattern> checkOfLoopVariable = createDefaultList();
		checkOfLoopVariable.add(createReadInstruction(variable));
		checkOfLoopVariable.addAll(compileExpression(bound2));
		
		//Loop body
		no.uio.ifi.llp.Block loopBodyBlock = compileBlock(block);
		
		//Increment/decrement of loop variable (last thing in repetition).
		List<DataAccessPattern> changeOfLoopVariable = createDefaultList();
		changeOfLoopVariable.add(createReadInstruction(variable));
		changeOfLoopVariable.add(createWriteInstruction(variable));
		
		no.uio.ifi.llp.Block repetitionBlock = loopBodyBlock;
		List<DataAccessPattern> dataAccessPatterns = repetitionBlock.getDataAccessPatterns();
		dataAccessPatterns.addAll(0, checkOfLoopVariable);
		dataAccessPatterns.addAll(changeOfLoopVariable);
		
		RepetitionInstruction repetitionInstruction = factory.createRepetitionInstruction();
		repetitionInstruction.setBlock(repetitionBlock);
		
		//NOTE: Based on the assumption that bound can only be literals (which is true for now but not in general).
		LiteralValue literalBound1 = (LiteralValue) bound1;
		LiteralValue literalBound2 = (LiteralValue) bound2;
		
		int numberOfRepetitions = Math.max(0, incrementing ? literalBound2.getRawValue() - literalBound1.getRawValue() + 1 : literalBound1.getRawValue() - literalBound2.getRawValue() + 1);
		repetitionInstruction.setNumberOfRepetitions(numberOfRepetitions);
		
		result.add(repetitionInstruction);
		
		return result;
	}
	
	protected List<DataAccessPattern> compileSynchronizedStatement(SynchronizedStatement synchronizedStatement) {
		List<DataAccessPattern> result = createDefaultList();
		
		List<VariableReference> variableReferences = synchronizedStatement.getVariableReferences();
		Block block = synchronizedStatement.getBlock();
		
		//Lock in order of declaration
//		for (VariableReference variableReference : variableReferences) {
//			result.add(createLockInstruction(variableReference));
//		}
		
		result.add(createLockInstruction(variableReferences));
		
		result.addAll(unwrapBlock(compileBlock(block)));
		
		//Unlock in inverse order of locking.
//		ListIterator<VariableReference> iterator = variableReferences.listIterator(variableReferences.size());
//		
//		while(iterator.hasPrevious()) {
//			VariableReference variableReference = iterator.previous();
//			
//			result.add(createUnlockInstruction(variableReference));
//		}
		
		result.add(createUnlockInstruction(variableReferences));
		
		return result;
	}
	
	protected List<DataAccessPattern> compileExpression(Expression expression) {
		List<DataAccessPattern> result = createDefaultList();
		
		if (expression instanceof VariableReference) {
			VariableReference variableReference = (VariableReference) expression;

			//Only getting to this point for reads.
			result.add(createReadInstruction(variableReference));
			return result;
		}
		
		if (expression instanceof LiteralValue) {
			//LiteralValue literalValue = (LiteralValue) expression;
			//Integer rawValue = literalValue.getRawValue();

			//Discard. Not interested in this.
			return result;
		}
		
		if (expression instanceof ParenthesisExpression) {
			ParenthesisExpression parenthesisExpression = (ParenthesisExpression) expression;
			
			//Create LLP parenthesis instruction
			ParenthesisInstruction parenthesisInstruction = factory.createParenthesisInstruction();
			no.uio.ifi.llp.Block llpBlock = factory.createBlock();
			parenthesisInstruction.setBlock(llpBlock);
			List<DataAccessPattern> dataAccessPatterns = llpBlock.getDataAccessPatterns();
			
			Expression operand = parenthesisExpression.getOperand();
			dataAccessPatterns.addAll(compileExpression(operand));
			
			result.add(parenthesisInstruction);
			
			return result;
		}
		
		if (expression instanceof BinaryExpression) {
			BinaryExpression binaryExpression = (BinaryExpression) expression;
			result.addAll(compileBinaryExpression(binaryExpression));
			return result;
		}
		
		throw new UnsupportedOperationException();
	}
	
	protected List<DataAccessPattern> compileBinaryExpression(BinaryExpression binaryExpression) {
		List<DataAccessPattern> result = createDefaultList();
		
		Expression leftHandSide = binaryExpression.getLeftHandSide();
		Expression rightHandSide = binaryExpression.getRightHandSide();
		
		result.addAll(compileExpression(leftHandSide));
		result.addAll(compileExpression(rightHandSide));
		
		//No more specifics here!
		if (binaryExpression instanceof AddExpression) {
			return result;
		}
		
		if (binaryExpression instanceof SubtractExpression) {
			return result;
		}
		
		if (binaryExpression instanceof MultiplyExpression) {
			return result;
		}
		
		if (binaryExpression instanceof DivideExpression) {
			return result;
		}
		
		throw new UnsupportedOperationException();
	}
	
	protected static List<DataAccessPattern> createDefaultList() {
		return new LinkedList<DataAccessPattern>();
	}
	
	
	//Helpers
	
	protected List<DataAccessPattern> unwrapBlock(no.uio.ifi.llp.Block block) {
		List<DataAccessPattern> result = createDefaultList();
		List<DataAccessPattern> dataAccessPatterns = block.getDataAccessPatterns();
		
		result.addAll(dataAccessPatterns);
		dataAccessPatterns.clear();
		
		return result;
	}
	
	protected String createLockName(List<VariableReference> variableReferences) {
		List<String> variableNames = getVariableNamesFromVariableReferences(variableReferences);
		
		for (Entry<List<String>, String> entry : variableSetsToLockNamesMap.entrySet()) {
			List<String> comparisonVariableNames = entry.getKey();
			
			if (comparisonVariableNames.size() == variableReferences.size()) {
				if (comparisonVariableNames.containsAll(variableNames)) {
					return entry.getValue();
				}
			}
		}
		
		String lockName = "l" + (variableSetsToLockNamesMap.size() + 1);
		variableSetsToLockNamesMap.put(variableNames, lockName);
		return lockName;
	}
	
//	protected String createLockName(List<VariableReference> variableReferences) {
//		List<String> variableNames = new ArrayList<String>();
//		
//		for (VariableReference variableReference : variableReferences) {
//			Variable variable = variableReference.getVariable();
//			String variableName = variable.getName();
//			variableNames.add(variableName);
//		}
//		
//		Collections.sort(variableNames);
//		return StringUtil.implode(variableNames, "");
//	}
	
	private List<String> getVariableNamesFromVariableReferences(Collection<VariableReference> variableReferences) {
		List<String> variableNames = new ArrayList<String>();
		
		for (VariableReference variableReference : variableReferences) {
			Variable variable = variableReference.getVariable();
			String variableName = variable.getName();
			variableNames.add(variableName);
		}
		
		Collections.sort(variableNames);
		
		return variableNames;
	}
	
	private void dumpLockMap() {
		for (Entry<List<String>, String> entry : variableSetsToLockNamesMap.entrySet()) {
			List<String> variableNames = entry.getKey();
			String lockName = entry.getValue();
			
			System.out.println(lockName + " = " + StringUtil.implode(variableNames, ", "));
		}
		
		System.out.println();
	}
	
	//Helpers for LLP creation

	protected ReadInstruction createReadInstruction(VariableReference variableReference) {
		Variable variable = variableReference.getVariable();
		return createReadInstruction(variable);
	}
	
	protected ReadInstruction createReadInstruction(Variable variable) {
		ReadInstruction readInstruction = factory.createReadInstruction();
		MemoryReference memoryReference = createMemoryReference(variable);
		readInstruction.setMemoryReference(memoryReference);
		return readInstruction;
	}
	
	
	protected WriteInstruction createWriteInstruction(VariableReference variableReference) {
		Variable variable = variableReference.getVariable();
		return createWriteInstruction(variable);
	}
	
	protected WriteInstruction createWriteInstruction(Variable variable) {
		WriteInstruction writeInstruction = factory.createWriteInstruction();
		MemoryReference memoryReference = createMemoryReference(variable);
		writeInstruction.setMemoryReference(memoryReference);
		return writeInstruction;
	}
	
	protected SkipInstruction createSkipInstruction() {
		return factory.createSkipInstruction();
	}
	
	
	
	protected LockInstruction createLockInstruction(List<VariableReference> variableReferences) {
		LockInstruction lockInstruction = factory.createLockInstruction();
		String lockName = createLockName(variableReferences);
		MemoryReference memoryReference = createMemoryReference(lockName);
		lockInstruction.setMemoryReference(memoryReference);
		return lockInstruction;
	}
	
//	protected LockInstruction createLockInstruction(VariableReference variableReference) {
//		Variable variable = variableReference.getVariable();
//		return createLockInstruction(variable);
//	}
//	
//	protected LockInstruction createLockInstruction(Variable variable) {
//		LockInstruction lockInstruction = factory.createLockInstruction();
//		MemoryReference memoryReference = createMemoryReference(variable);
//		lockInstruction.setMemoryReference(memoryReference);
//		return lockInstruction;
//	}
	
	
	protected UnlockInstruction createUnlockInstruction(List<VariableReference> variableReferences) {
		UnlockInstruction unlockInstruction = factory.createUnlockInstruction();
		String lockName = createLockName(variableReferences);
		MemoryReference memoryReference = createMemoryReference(lockName);
		unlockInstruction.setMemoryReference(memoryReference);
		return unlockInstruction;
	}
	
//	protected UnlockInstruction createUnlockInstruction(VariableReference variableReference) {
//		Variable variable = variableReference.getVariable();
//		return createUnlockInstruction(variable);
//	}
//	
//	protected UnlockInstruction createUnlockInstruction(Variable variable) {
//		UnlockInstruction unlockInstruction = factory.createUnlockInstruction();
//		MemoryReference memoryReference = createMemoryReference(variable);
//		unlockInstruction.setMemoryReference(memoryReference);
//		return unlockInstruction;
//	}
	
	
	private MemoryReference createMemoryReference(Variable variable) {
//		String address = getAddress(variable);
		return createMemoryReference(variable.getName());
	}
	
	private MemoryReference createMemoryReference(String name) {
		MemoryReference memoryReference = factory.createMemoryReference();
		memoryReference.setAddress(name);
		return memoryReference;
	}
}
