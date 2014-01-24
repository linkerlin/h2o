package water.exec;

import java.util.ArrayList;
import java.util.List;

import water.H2O;
import water.util.Log;

/** Parse a generic R string and build an AST, in the context of an H2O Cloud
 *  @author cliffc@0xdata.com
 */

// --------------------------------------------------------------------------
public class ASTFunc extends ASTOp {
  final AST _body;
  final String _locals[];       // including arguments and local variables.
  final int _tmps;
  Env _env;                     // Captured environment at each apply point
  Env2 _envR;                   // used in rwo-wise evaluation
  transient Env2 _caller;       // the cache of caller environment, used for detecting environment change
  ASTFunc( String vars[], String locals[], Type vtypes[], AST body, int tmps ) {
    super(vars,vtypes,OPF_PREFIX,OPP_PREFIX,OPA_RIGHT); _body = body; _locals=locals; _tmps=tmps;
  }
  @Override String opStr() { return "fun"; }
  @Override ASTOp make() { throw H2O.fail();} 
  static ASTOp parseFcn(Exec2 E ) {
    int x = E._x;
    String var = E.isID();
    if( var == null ) return null;
    if( !"function".equals(var) ) { E._x = x; return null; }
    E.xpeek('(',E._x,null);
    ArrayList<ASTId> vars = new ArrayList<ASTId>();
    if( !E.peek(')') ) {
      while( true ) {
        x = E._x;
        var = E.isID();
        if( var == null ) E.throwErr("Invalid var",x);
        for( ASTId id : vars ) if( var.equals(id._id) ) E.throwErr("Repeated argument",x);
        // Add unknown-type variable to new vars list
        vars.add(new ASTId(Type.unbound(),var,0,vars.size()));
        if( E.peek(')') ) break;
        E.xpeek(',',E._x,null);
      }
    }
    int argcnt = vars.size();   // Record current size, as body may extend
    // Parse the body
    E.xpeek('{',(x=E._x),null);
    E._env.push(vars);
    AST body = E.xpeek('}',E._x,ASTStatement.parse(E));
    if( body == null ) E.throwErr("Missing function body",x);
    List<ASTId> local_ids = E._env.pop();
    String[] local_names = new String[local_ids.size()];
    for (int i = 0; i < local_ids.size(); i++)
      local_names[i] = local_ids.get(i)._id;

    // The body should force the types.  Build a type signature.
    String xvars[] = new String[argcnt+1];
    Type   types[] = new Type  [argcnt+1];

    xvars[0] = "fun";
    types[0] = body._t;         // Return type of body
    for( int i=0; i<argcnt; i++ ) {
      ASTId id = vars.get(i);
      xvars[i+1] = id._id;
      types[i+1] = id._t;
    }
    return new ASTFunc(xvars,local_names,types,body,vars.size()-argcnt);
  }  
  
  @Override void exec(Env env) { 
    // We need to push a Closure: the ASTFunc plus captured environment.
    // Make a shallow copy (the body remains shared across all ASTFuncs).
    // Then fill in the current environment.
    ASTFunc fun = (ASTFunc)clone();
    fun._env = env.capture();
    env.push(fun);
  }
  @Override void apply(Env env, int argcnt) {
    int res_idx = env.pushScope(argcnt-1);
    env.push(_tmps);
    _body.exec(env);
    env.tos_into_slot(res_idx-1,null);
    env.popScope();
  }

  @Override void evalR(Env2 env) {
    ASTFunc copy = (ASTFunc)clone();
    copy._envR = new Env2(env,copy).copy();
    env.setFcn0(copy);
  }

  @Override double[] map(Env2 caller, double[] out, double[]... ins) {
    assert caller!=null;
    if (caller != _caller) {
      _caller = caller;
      _envR = new Env2(null,this);
    }
    assert _vars.length-1 == ins.length;
    for (int i = 0; i < ins.length; i++) _envR.setAry(i, ins[i]);
    _body.evalR(_envR);
    out = _envR.getAry0();
    if (out == null) throw H2O.unimpl();
    return out;
  }

  @Override public StringBuilder toString( StringBuilder sb, int d ) {
    indent(sb,d).append(this).append(") {\n");
    _body.toString(sb,d+1).append("\n");
    return indent(sb,d).append("}");
  }
}
