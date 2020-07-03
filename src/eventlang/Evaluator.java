package eventlang;
import static eventlang.AST.*;
import static eventlang.Value.*;
import static eventlang.Heap.*;

import eventlang.AST.AnnounceExp;
import eventlang.AST.EventExp;
import eventlang.AST.Exp;
import eventlang.AST.ProgramError;
import eventlang.AST.WhenExp;
import eventlang.Env.*;
import eventlang.Value.EventVal;
import eventlang.Value.UnitVal;

import java.util.List;
import java.util.ArrayList;
import java.io.File;
import java.io.IOException;

public class Evaluator implements Visitor<Value, Value> {
	
	Printer.Formatter ts = new Printer.Formatter();

	private volatile Env<Value> initEnv = initialEnv(); 
	
    private volatile Heap heap = new Heap16Bit(); 
    
    
    Value valueOf(Program p) throws ProgramError {
		return (Value) p.accept(this, initEnv);
	}
	
	@Override
	public Value visit(AddExp e, Env<Value> env) throws ProgramError {
		List<Exp> operands = e.all();
		double result = 0;
		for(Exp exp: operands) {
			NumVal intermediate = (NumVal) exp.accept(this, env); // Dynamic type-checking
			result += intermediate.v(); //Semantics of AddExp in terms of the target language.
		}
		return new NumVal(result);
	}
	
	@Override
	public Value visit(UnitExp e, Env<Value> env) throws ProgramError {
		return new UnitVal();
	}

	@Override
	public Value visit(NumExp e, Env<Value> env) throws ProgramError {
		return new NumVal(e.v());
	}

	@Override
	public Value visit(StrExp e, Env<Value> env) throws ProgramError {
		return new StringVal(e.v());
	}

	@Override
	public Value visit(BoolExp e, Env<Value> env) throws ProgramError {
		return new BoolVal(e.v());
	}

	@Override
	public Value visit(DivExp e, Env<Value> env) throws ProgramError {
		List<Exp> operands = e.all();
		NumVal lVal = (NumVal) operands.get(0).accept(this, env);
		double result = lVal.v(); 
		for(int i=1; i<operands.size(); i++) {
			NumVal rVal = (NumVal) operands.get(i).accept(this, env);
			result = result / rVal.v();
		}
		return new NumVal(result);
	}

	@Override
	public Value visit(ErrorExp e, Env<Value> env) throws ProgramError {
		return new Value.DynamicError("Encountered an error expression");
	}

	@Override
	public Value visit(MultExp e, Env<Value> env) throws ProgramError {
		List<Exp> operands = e.all();
		double result = 1;
		for(Exp exp: operands) {
			NumVal intermediate = (NumVal) exp.accept(this, env); // Dynamic type-checking
			result *= intermediate.v(); //Semantics of MultExp.
		}
		return new NumVal(result);
	}

	@Override
	public Value visit(Program p, Env<Value> env) throws ProgramError {
		for(DefineDecl d: p.decls())
			d.accept(this, initEnv);
		return (Value) p.e().accept(this, initEnv);
	}

	@Override
	public Value visit(SubExp e, Env<Value> env) throws ProgramError {
		List<Exp> operands = e.all();
		NumVal lVal = (NumVal) operands.get(0).accept(this, env);
		double result = lVal.v();
		for(int i=1; i<operands.size(); i++) {
			NumVal rVal = (NumVal) operands.get(i).accept(this, env);
			result = result - rVal.v();
		}
		return new NumVal(result);
	}

	@Override
	public Value visit(VarExp e, Env<Value> env) throws ProgramError {
		// Previously, all variables had value 42. New semantics.
		return env.get(e.name());
	}	

	@Override
	public Value visit(LetExp e, Env<Value> env) throws ProgramError { // New for varlang.
		List<String> names = e.names();
		List<Exp> value_exps = e.value_exps();
		List<Value> values = new ArrayList<Value>(value_exps.size());
		
		for(Exp exp : value_exps){
			Value value = (Value)exp.accept(this, env);
			if(! (value instanceof DynamicError))
				values.add(value);
			else 
				return value;
		}
		
		Env<Value> new_env = env;
		for (int index = 0; index < names.size(); index++)
			new_env = new ExtendEnv<>(new_env, names.get(index), values.get(index));

		return (Value) e.body().accept(this, new_env);		
	}	
	
	@Override
	public Value visit(DefineDecl e, Env<Value> env) throws ProgramError { // New for definelang.
		String name = e.name();
		Exp value_exp = e.value_exp();
		Value value = (Value) value_exp.accept(this, env);
		((GlobalEnv<Value>) initEnv).extend(name, value);
		return new Value.UnitVal();		
	}	

	@Override
	public Value visit(LambdaExp e, Env<Value> env) throws ProgramError {
		return new Value.FunVal(env, e.formals(), e.body());
	}
	
	@Override
	public Value visit(CallExp e, Env<Value> env) throws ProgramError {
		Object result = e.operator().accept(this, env);
		if(!(result instanceof Value.FunVal))
			return new Value.DynamicError("Operator not a function in call " +  ts.visit(e, null));
		Value.FunVal operator =  (Value.FunVal) result; 
		List<Exp> operands = e.operands();

		// Call-by-value semantics
		List<Value> actuals = new ArrayList<Value>(operands.size());
		for(Exp exp : operands) 
			actuals.add((Value)exp.accept(this, env));
		
		List<String> formals = operator.formals();
		
		Env<Value> fun_env = operator.env();

		if(operator.formals().contains("..."))
		{
			ListExp varargs = new ListExp(operands);			
			fun_env = new ExtendEnv<>(fun_env, formals.get(0), (Value) varargs.accept(this, fun_env));
			return (Value) operator.body().accept(this, fun_env);
		}
		else if (formals.size()!=actuals.size())
			return new Value.DynamicError("Argument mismatch in call " + ts.visit(e, null));

		for (int index = 0; index < formals.size(); index++)
			fun_env = new ExtendEnv<>(fun_env, formals.get(index), actuals.get(index));
		
		return (Value) operator.body().accept(this, fun_env);
	}
		
	@Override
	public Value visit(IfExp e, Env<Value> env) throws ProgramError { // New for funclang.
		Object result = e.conditional().accept(this, env);
		if(!(result instanceof Value.BoolVal))
			return new Value.DynamicError("Condition not a boolean in expression " +  ts.visit(e, null));
		Value.BoolVal condition =  (Value.BoolVal) result; //Dynamic checking
		
		if(condition.v())
			return (Value) e.then_exp().accept(this, env);
		else return (Value) e.else_exp().accept(this, env);
	}

	@Override
	public Value visit(LessExp e, Env<Value> env) throws ProgramError { // New for funclang.
		Value.NumVal first = (Value.NumVal) e.first_exp().accept(this, env);
		Value.NumVal second = (Value.NumVal) e.second_exp().accept(this, env);
		return new Value.BoolVal(first.v() < second.v());
	}
	
	@Override
	public Value visit(EqualExp e, Env<Value> env) throws ProgramError { // New for funclang.
		Value.NumVal first = (Value.NumVal) e.first_exp().accept(this, env);
		Value.NumVal second = (Value.NumVal) e.second_exp().accept(this, env);
		return new Value.BoolVal(first.v() == second.v());
	}

	@Override
	public Value visit(GreaterExp e, Env<Value> env) throws ProgramError { // New for funclang.
		Value.NumVal first = (Value.NumVal) e.first_exp().accept(this, env);
		Value.NumVal second = (Value.NumVal) e.second_exp().accept(this, env);
		return new Value.BoolVal(first.v() > second.v());
	}
	
	@Override
	public Value visit(CarExp e, Env<Value> env) throws ProgramError { 
		Value.PairVal pair = (Value.PairVal) e.arg().accept(this, env);
		return pair.fst();
	}
	
	@Override
	public Value visit(CdrExp e, Env<Value> env) throws ProgramError { 
		Value.PairVal pair = (Value.PairVal) e.arg().accept(this, env);
		return pair.snd();
	}
	
	@Override
	public Value visit(ConsExp e, Env<Value> env) throws ProgramError { 
		Value first = (Value) e.fst().accept(this, env);
		Value second = (Value) e.snd().accept(this, env);
		return new Value.PairVal(first, second);
	}

	@Override
	public Value visit(ListExp e, Env<Value> env) throws ProgramError {
		List<Exp> elemExps = e.elems();
		int length = elemExps.size();
		if(length == 0)
			return new Value.Null();
		
		//Order of evaluation: left to right e.g. (list (+ 3 4) (+ 5 4)) 
		Value[] elems = new Value[length];
		for(int i=0; i<length; i++)
			elems[i] = (Value) elemExps.get(i).accept(this, env);
		
		Value result = new Value.Null();
		for(int i=length-1; i>=0; i--) 
			result = new PairVal(elems[i], result);
		return result;
	}
	
	@Override
	public Value visit(NullExp e, Env<Value> env) throws ProgramError {
		Value val = (Value) e.arg().accept(this, env);
		return new BoolVal(val instanceof Value.Null);
	}

	@Override
	public Value visit(IsListExp e, Env<Value> env) throws ProgramError {
		Value val = (Value) e.exp().accept(this, env);
		return new BoolVal(val instanceof Value.PairVal &&
				((Value.PairVal) val).isList() ||
				val instanceof Value.Null);
	}

	@Override
	public Value visit(IsPairExp e, Env<Value> env) throws ProgramError {
		Value val = (Value) e.exp().accept(this, env);
		return new BoolVal(val instanceof Value.PairVal);
	}

	@Override
	public Value visit(IsUnitExp e, Env<Value> env) throws ProgramError {
		Value val = (Value) e.exp().accept(this, env);
		return new BoolVal(val instanceof Value.UnitVal);
	}

	@Override
	public Value visit(IsProcedureExp e, Env<Value> env) throws ProgramError {
		Value val = (Value) e.exp().accept(this, env);
		return new BoolVal(val instanceof Value.FunVal);
	}

	@Override
	public Value visit(IsStringExp e, Env<Value> env) throws ProgramError {
		Value val = (Value) e.exp().accept(this, env);
		return new BoolVal(val instanceof Value.StringVal);
	}

	@Override
	public Value visit(IsNumberExp e, Env<Value> env) throws ProgramError {
		Value val = (Value) e.exp().accept(this, env);
		return new BoolVal(val instanceof Value.NumVal);
	}

	@Override
	public Value visit(IsBooleanExp e, Env<Value> env) throws ProgramError {
		Value val = (Value) e.exp().accept(this, env);
		return new BoolVal(val instanceof Value.BoolVal);
	}

	@Override
	public Value visit(IsNullExp e, Env<Value> env) throws ProgramError {
		Value val = (Value) e.exp().accept(this, env);
		return new BoolVal(val instanceof Value.Null);
	}

	public Value visit(EvalExp e, Env<Value> env) throws ProgramError {
		StringVal programText = (StringVal) e.code().accept(this, env);
		Program p = _reader.parse(programText.v());
		return (Value) p.accept(this, env);
	}

	public Value visit(ReadExp e, Env<Value> env) throws ProgramError {
		StringVal fileName = (StringVal) e.file().accept(this, env);
		try {
			String text = Reader.readFile("" + System.getProperty("user.dir") + File.separator + fileName.v());
			return new StringVal(text);
		} catch (IOException exception) {
			return new Value.DynamicError(exception.getMessage());
		}
	}
	
	@Override
	public Value visit(LetrecExp e, Env<Value> env) throws ProgramError { // New for reclang.
		List<String> names = e.names();
		List<Exp> fun_exps = e.fun_exps();
		List<Value.FunVal> funs = new ArrayList<Value.FunVal>(fun_exps.size());
		
		for(Exp exp : fun_exps) 
			funs.add((Value.FunVal)exp.accept(this, env));

		Env<Value> new_env = new ExtendEnvRec(env, names, funs);
		return (Value) e.body().accept(this, new_env);		
	}	
    
	@Override
	public Value visit(RefExp e, Env<Value> env) throws ProgramError { // New for reflang.
		Exp value_exp = e.value_exp();
		Value value = (Value) value_exp.accept(this, env);
		return heap.ref(value);
	}

	@Override
	public Value visit(DerefExp e, Env<Value> env) throws ProgramError { // New for reflang.
		Exp loc_exp = e.loc_exp();
		Value.RefVal loc = (Value.RefVal) loc_exp.accept(this, env);
		return heap.deref(loc);
	}

	@Override
	public Value visit(AssignExp e, Env<Value> env) throws ProgramError { // New for reflang.
		Exp rhs = e.rhs_exp();
		Exp lhs = e.lhs_exp();
		//Note the order of evaluation below.
		Value rhs_val = (Value) rhs.accept(this, env);
		Value.RefVal loc = (Value.RefVal) lhs.accept(this, env);
		Value assign_val = heap.setref(loc, rhs_val);
		return assign_val;
	}

	@Override
	public Value visit(FreeExp e, Env<Value> env) throws ProgramError { // New for reflang.
		Exp value_exp = e.value_exp();
		Value.RefVal loc = (Value.RefVal) value_exp.accept(this, env);
		heap.free(loc);
		return new Value.UnitVal();
	}

	private Env<Value> initialEnv() {
		GlobalEnv<Value> initEnv = new GlobalEnv<>();
		
		/* Procedure: (read <filename>). Following is same as (define read (lambda (file) (read file))) */
		List<String> formals = new ArrayList<>();
		formals.add("file");
		Exp body = new AST.ReadExp(new VarExp("file"));
		Value.FunVal readFun = new Value.FunVal(initEnv, formals, body);
		initEnv.extend("read", readFun);

		/* Procedure: (require <filename>). Following is same as (define require (lambda (file) (eval (read file)))) */
		formals = new ArrayList<>();
		formals.add("file");
		body = new EvalExp(new AST.ReadExp(new VarExp("file")));
		Value.FunVal requireFun = new Value.FunVal(initEnv, formals, body);
		initEnv.extend("require", requireFun);
		
		/* Add new built-in procedures here */ 
		
		return initEnv;
	}
	
	Reader _reader; 
	public Evaluator(Reader reader) {
		_reader = reader;
	}


	static class EvalThread extends Thread {
		Env<Value> env;
		Exp exp;
		Evaluator evaluator;
		private volatile Value value;

		protected EvalThread(Env<Value> env, Exp exp, Evaluator evaluator){
			this.env = env;
			this.exp = exp;
			this.evaluator = evaluator;
		}
		
		public void run(){
			try {
				value = (Value) exp.accept(evaluator, env);
			} catch (ProgramError e) {
				e.printStackTrace();
			}
		}
		
		public Value value(){
			try {
				this.join();
			} catch (InterruptedException e) {
				return new Value.DynamicError(e.getMessage());
			}
			return value;
		}
	}
	
	@Override
	public Value visit(EventExp e, Env<Value> env) throws ProgramError {
		return new Value.EventVal(e.contexts());
	}

	@Override
	public Value visit(AnnounceExp e, Env<Value> env) throws ProgramError {
        Exp event_exp = e.event();
        Object result = event_exp.accept(this, env);
		if(!(result instanceof Value.EventVal))
			return new Value.DynamicError("Non-event value cannot be announced in expression " +  ts.visit(e, null));
        Value.EventVal event = (Value.EventVal) result;
		if(!(e.actuals().size()== event.contexts().size()))
			return new Value.DynamicError("Number of context varaibles do not match in announce expression " +  ts.visit(e, null));
		
		List<String> contexts = event.contexts();		
		List<Value> actuals = new ArrayList<Value>(contexts.size());
		
		for(Exp exp : e.actuals()) 
			actuals.add((Value)exp.accept(this, env));

		Env<Value> ann_env = this.initEnv;
		for (int index = 0; index < actuals.size(); index++)
			ann_env = new ExtendEnv<>(ann_env, contexts.get(index), actuals.get(index));

		Value lastVal = new UnitVal();
		for(Exp exp:event.observers()){
			lastVal = (Value)exp.accept(this, ann_env);
        }
		return lastVal;
	}

	@Override
	public Value visit(WhenExp e, Env<Value> env) throws ProgramError {
        Exp event_exp = e.event();
        Object result = event_exp.accept(this, env);
		if(!(result instanceof Value.EventVal))
			return new Value.DynamicError("Non-event value cannot be used in when expression " +  ts.visit(e, null));
		EventVal event = (EventVal) result;
		Exp observer = e.body();
		event.register(observer);
		return new UnitVal();
	}

	@Override
	public Value visit(PrintExp e, Env<Value> env) throws ProgramError {
		Exp value_exp = e.value_exp();
		Object result = value_exp.accept(this, env);
		new Printer().print((Value)result);
		return new Value.UnitVal();
	}
	
	@Override
	public Value visit(SeqExp e, Env<Value> env) throws ProgramError {
		List<Exp> expressions = e.expressions();

		Value result = new Value.UnitVal();
		
		for(Exp exp : expressions) 
			result = (Value) exp.accept(this, env);
				
		return result;
	}

}
