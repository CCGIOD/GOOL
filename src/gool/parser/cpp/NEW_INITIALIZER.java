/* Generated By:JJTree: Do not edit this line. NEW_INITIALIZER.java Version 4.3 */
/* JavaCCOptions:MULTI=true,NODE_USES_PARSER=false,VISITOR=true,TRACK_TOKENS=true,NODE_PREFIX=,NODE_EXTENDS=,NODE_FACTORY=,SUPPORT_CLASS_VISIBILITY_PUBLIC=true */
package gool.parser.cpp;

public
class NEW_INITIALIZER extends SimpleNode {
  public NEW_INITIALIZER(int id) {
    super(id);
  }

  public NEW_INITIALIZER(CPPParser p, int id) {
    super(p, id);
  }


  /** Accept the visitor. **/
  public Object jjtAccept(CPPParserVisitor visitor, Object data) {
    return visitor.visit(this, data);
  }
}
/* JavaCC - OriginalChecksum=724a8c9b419999750f128cb5725e7ab1 (do not edit this line) */