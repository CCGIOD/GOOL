/* Generated By:JJTree: Do not edit this line. CPPAST_PARAMETER_DECLARATION.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=CPPAST_,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package gool.parser.cpp.nodes;

import gool.parser.cpp.*;

public
class CPPAST_PARAMETER_DECLARATION extends SimpleNode {
  public CPPAST_PARAMETER_DECLARATION(int id) {
    super(id);
  }

  public CPPAST_PARAMETER_DECLARATION(CPPParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(CPPParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=3c7c7e7c3afbc9211770f3173b2e654d (do not edit this line) */
