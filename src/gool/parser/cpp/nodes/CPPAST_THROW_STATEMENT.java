/* Generated By:JJTree: Do not edit this line. CPPAST_THROW_STATEMENT.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=CPPAST_,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package gool.parser.cpp.nodes;

import gool.parser.cpp.*;

public
class CPPAST_THROW_STATEMENT extends SimpleNode {
  public CPPAST_THROW_STATEMENT(int id) {
    super(id);
  }

  public CPPAST_THROW_STATEMENT(CPPParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(CPPParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=ee4276bab41f55e2c79b0afb967a3670 (do not edit this line) */