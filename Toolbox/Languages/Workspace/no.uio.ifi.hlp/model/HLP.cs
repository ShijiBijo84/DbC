SYNTAXDEF hlp
FOR <http://uio.no/ifi/hlp/1.0>
START HighLevelProgram

OPTIONS {
	reloadGeneratorModel = "true";
	defaultTokenName = "IDENTIFIER_TOKEN";
	overrideBuilder = "false";
//	overrideLaunchConfigurationDelegate = "false";
	
	disableNewProjectWizard = "true";
	disableDebugSupport = "true";
	disableLaunchSupport = "true";
	
	newFileWizardName = "High-Level Program (*.hlp)";
	newFileWizardCategory = "no.uio.ifi.newfilewizard.category";
}

TOKENS {
	DEFINE IDENTIFIER_TOKEN $('a'..'z'|'A'..'Z'|'_') ('a'..'z'|'A'..'Z'|'_'|'0'..'9')*$;
	DEFINE INTEGER_TOKEN $('-')?('0'..'9')+$;

	DEFINE SL_COMMENT $'//'(~('\n'|'\r'|'\uffff'))* $;
	DEFINE ML_COMMENT $'/*'.*'*/'$;

//	DEFINE WHITESPACE $(' '|'\t')+$;
//	DEFINE LINEBREAK $('\r'|'\n')+$;
//	DEFINE WS $(' '|'\t'|'\r'|'\n')+$;
}

TOKENSTYLES {
	"IDENTIFIER_TOKEN" COLOR #6A3E3E;
	"INTEGER_TOKEN" COLOR #0000C0;
	"SL_COMMENT", "ML_COMMENT" COLOR #3F7F5F;
}

RULES {
	HighLevelProgram ::= "Program" #1 name[]
		(!1 "Variables" #1 variableDeclarations ("," #1 variableDeclarations)* ".")?
		(!1 tasks !1)* !0 scheduleInstruction? "End" ".";
	
	ScheduleInstruction ::= "Schedule" "(" tasks[] ("," #1 tasks[])* ")" ".";
	
	Task ::= "Task" #1 name[]
		(!1 "Variables" #1 variableDeclarations ("," #1 variableDeclarations)* ".")?
		block !0 "End" ".";
	
	VariableDeclaration ::= variable (#1 ":=" #1 initialValue)?;
	Variable ::= name[];
	
	Block ::= (!1 statements)*;
	
	Assignment ::= leftHandSide #1 ":=" #1 rightHandSide ".";
//	ExpressionStatement ::= expression ".";
	IfStatement ::= "If" #1 "(" condition ")" #1 "Then" thenBlock (!0 "Else" elseBlock)? !0 "End" ".";
	
	//NOTE: Disabled for now as number of repetitions cannot be determined easily.
//	WhileLoop ::= "While" #1 "(" condition ")" #1 "Do" block !0 "End" ".";
	
	//NOTE: Restricted bounds to literal values so that number of repetitions can be determined. 
	ForLoop ::= "For" #1 variableReference #1 ":=" #1 bound1 : LiteralValue #1 incrementing["" : "Down"] #1 "To" #1 bound2 : LiteralValue #1 "Do" block !0 "End" ".";
	SynchronizedStatement ::= "Synchronized" "(" variableReferences ("," #1 variableReferences)* ")" block "End" ".";
	
	Condition ::= leftHandSide #1 operator[EQUAL : "=", UNEQUAL : "!=", LESS_THAN : "<", LESS_THAN_OR_EQUAL : "<=", GREATER_THAN_OR_EQUAL : ">=", GREATER_THAN : ">"] #1 rightHandSide;


	@Operator(type="binary_left_associative", weight="1", superclass="Expression")
	AddExpression ::= leftHandSide #1 "+" #1 rightHandSide;
	
	@Operator(type="binary_left_associative", weight="1", superclass="Expression")
	SubtractExpression ::= leftHandSide #1 "-" #1 rightHandSide;
	
	@Operator(type="binary_left_associative", weight="3", superclass="Expression")
	MultiplyExpression ::= leftHandSide #1 "*" #1 rightHandSide;
	
	@Operator(type="binary_left_associative", weight="3", superclass="Expression")
	DivideExpression ::= leftHandSide #1 "/" #1 rightHandSide;

	//TODO: Requires a space in between because it is recognized as integer token otherwise.
	@Operator(type="unary_prefix", weight="4", superclass="Expression")
	UnaryMinusExpression ::= "-" #1 expression;
		
	@Operator(type="primitive", weight="5", superclass="Expression")
	ParenthesisExpression ::= "(" operand ")";

	@Operator(type="primitive", weight="5", superclass="Expression")
	LiteralValue ::= rawValue[INTEGER_TOKEN];
	
	@Operator(type="primitive", weight="5", superclass="Expression")
	VariableReference ::= variable[];
}

