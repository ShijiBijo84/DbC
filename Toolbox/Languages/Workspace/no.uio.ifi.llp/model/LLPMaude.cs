SYNTAXDEF llpm
FOR <http://uio.no/ifi/llp/1.0> <LLP.genmodel>
START LowLevelProgram

OPTIONS {
	reloadGeneratorModel = "true";
	defaultTokenName = "IDENTIFIER_TOKEN";
	
	disableNewProjectWizard = "true";
	disableDebugSupport = "true";
	disableLaunchSupport = "true";
	
	newFileWizardName = "Low-Level Program in Maude Syntax (*.llpm)";
	newFileWizardCategory = "no.uio.ifi.newfilewizard.category";
}

TOKENS {
	//NOTE: Identifiers start with a ' in the Maude implementations (for whatever reason).
	DEFINE IDENTIFIER_TOKEN $'\''('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9')*$;
	DEFINE INTEGER_TOKEN $('-')?('0'..'9')+$;
	
	DEFINE SL_COMMENT $'//'(~('\n'|'\r'|'\uffff'))* $;
	DEFINE ML_COMMENT $'/*'.*'*/'$;
}

TOKENSTYLES {
	"IDENTIFIER_TOKEN" COLOR #6A3E3E;
	"INTEGER_TOKEN" COLOR #0000C0;
	"SL_COMMENT", "ML_COMMENT" COLOR #3F7F5F;
}

//Example of the syntax:
//'main |-> (Spawn('T1) ; Spawn('T2) ; Spawn('T3)),
//	'T1 |-> (read(1) ; write(2)) * 20,
//	'T2 |-> (write(3) ; write(1)) * 30,
//	'T3 |-> (write(3) ; write(1)) * 30
             					  
RULES {
	LowLevelProgram ::= "'main" #1 "|->" #1 "(" mainBlock ")" (#0 "," !1 tasks)*;
	
	Task ::= name[] #1 "|->" #1 "(" block ")";
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
	
	//NOTE: Use ^ for number of repetitions.
	RepetitionInstruction ::= "(" block ")" #1 "*" #1 numberOfRepetitions[INTEGER_TOKEN];

	SkipInstruction ::= "skip";
	
	ParenthesisInstruction ::= "(" block ")";

	//NOTE: Spawn starts with a capital letter in the Maude implementation (as only keyword).
	SpawnInstruction ::= "Spawn" "(" task[] ")";
	
	//TODO: Can Maude handle memory addresses as string identifiers?
	MemoryReference ::= address[];
}
