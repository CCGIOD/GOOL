/* Generated By:JJTree: Do not edit this line. CPPAST_JUMP_STATEMENT.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=CPPAST_,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package gool.parser.cpp.nodes;

import gool.parser.cpp.*;

public
class CPPAST_JUMP_STATEMENT extends SimpleNode {
  public CPPAST_JUMP_STATEMENT(int id) {
    super(id);
  }

  public CPPAST_JUMP_STATEMENT(CPPParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(CPPParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=2457e2c1900fb3313b5a2848d41379b1 (do not edit this line) */
