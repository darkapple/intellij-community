package org.jetbrains.plugins.groovy.lang.psi.impl.statements;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.plugins.groovy.lang.psi.api.statements.GrWhileStatement;
import org.jetbrains.plugins.groovy.lang.psi.impl.GroovyPsiElementImpl;

/**
 * @autor: Dmitry.Krasilschikov
 * @date: 18.03.2007
 */
public class GrWhileStatementImpl extends GroovyPsiElementImpl implements GrWhileStatement {
  public GrWhileStatementImpl(@NotNull ASTNode node) {
    super(node);
  }

   public String toString() {
    return "While statement";
  }
}
