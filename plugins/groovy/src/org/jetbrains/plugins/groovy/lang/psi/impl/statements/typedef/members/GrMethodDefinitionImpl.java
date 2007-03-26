package org.jetbrains.plugins.groovy.lang.psi.impl.statements.typedef.members;

import org.jetbrains.plugins.groovy.lang.psi.api.statements.typedef.members.GrMethod;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyPsiElementImpl;
import org.jetbrains.annotations.NotNull;
import com.intellij.lang.ASTNode;

/**
 * @author: Dmitry.Krasilschikov
 * @date: 26.03.2007
 */
public class GrMethodDefinitionImpl extends GroovyPsiElementImpl implements GrMethod {
  public GrMethodDefinitionImpl(@NotNull ASTNode node) {
    super(node);
  }

  public String toString (){
    return "Method";
  }
}