SYNTAXDEF llp
FOR <http://uio.no/ifi/llp/1.0>
START LowLevelProgram

OPTIONS {
	reloadGeneratorModel = "true";
	defaultTokenName = "IDENTIFIER_TOKEN";
	overrideBuilder = "false";
	
	disableNewProjectWizard = "true";
	disableDebugSupport = "true";
	disableLaunchSupport = "true";
	
	newFileWizardName = "Low-Level Program (*.llp)";
	newFileWizardCategory = "no.uio.ifi.newfilewizard.category";
	
	additionalDependencies = "no.uio.ifi.maudecompiler";
}

TOKENS {
	DEFINE IDENTIFIER_TOKEN $('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9')*$;
	DEFINE INTEGER_TOKEN $('-')?('0'..'9')+$;
	
	DEFINE SL_COMMENT $'//'(~('\n'|'\r'|'\uffff'))* $;
	DEFINE ML_COMMENT $'/*'.*'*/'$;
}

TOKENSTYLES {
	"IDENTIFIER_TOKEN" COLOR #6A3E3E;
	"INTEGER_TOKEN" COLOR #0000C0;
	"SL_COMMENT", "ML_COMMENT" COLOR #3F7F5F;
}

RULES {
	LowLevelProgram ::= (tasks !0!0)* "main" #1 "{" !1 mainBlock !0 "}";
	
	Task ::= "task" #1 name[] #1 "{" !1 block !0 "}";
	Block ::= (dataAccessPatterns (";" #1 dataAccessPatterns)*)?;
	
	//IO
	ReadInstruction ::= "read" "(" memoryReference ")";
	WriteInstruction ::= "write" "(" memoryReference ")";
	
	//Cache
	CommitInstruction ::= "commit" ("(" memoryReference ")")?;
	
	//Synchronization
	LockInstruction ::= "lock" "(" memoryReference ")";
	UnlockInstruction ::= "unlock" "(" memoryReference ")";
	
	//Control Flow
	ControlFlowBranchingInstruction ::= "(" leftHandSideBlock ")" #1 "||" #1 "(" rightHandSideBlock ")";
	RepetitionInstruction ::= "(" block ")" #1 "*" #1 numberOfRepetitions[INTEGER_TOKEN];
	SkipInstruction ::= "skip";
	
	ParenthesisInstruction ::= "(" block ")";

	SpawnInstruction ::= "spawn" "(" task[] ")";
	
	MemoryReference ::= address[];
}
