SYNTAXDEF rm
FOR <http://uio.no/ifi/rm/1.0>
START ResourceModel

OPTIONS {
	reloadGeneratorModel = "true";
	
//	overrideTextHover = "false";
	
	newFileWizardName = "Resource Model (*.rm)";
	newFileWizardCategory = "no.uio.ifi.newfilewizard.category";
}

TOKENS {
	DEFINE INTEGER_TOKEN $('-')?('0'..'9')+$;
	
	DEFINE SL_COMMENT $'//'(~('\n'|'\r'|'\uffff'))* $;
	DEFINE ML_COMMENT $'/*'.*'*/'$;
}

TOKENSTYLES {
	"INTEGER_TOKEN" COLOR #0000C0;
	"SL_COMMENT", "ML_COMMENT" COLOR #3F7F5F;
}

RULES {
	ResourceModel ::= (memory devices+) | (devices+ memory);
	
	Device ::= "device" #1 "{"
		!1 "cacheSize" #0 ":" #1 cacheSize[INTEGER_TOKEN] ";"
		(!1 "localMemory" #0 ":" #1 localMemoryCellReference ";")? !0
	"}";
	
	MemoryCellReference ::= startCellIndex[INTEGER_TOKEN] "-" endCellIndex[INTEGER_TOKEN];

	Memory ::= "memory" #1 "{" !1 "size" ":" #1 size[INTEGER_TOKEN] ";" !1 ("references" #1 "{" (!1 variableReferences ";")+ !0 "}" )? !0 "}";
	VariableReference ::= variable[] #1 ":" #1 memoryCellIndex[INTEGER_TOKEN];
}
