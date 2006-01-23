package com.intellij.openapi.editor.actions.moveUpDown;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.editor.LogicalPosition;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.codeStyle.CodeEditUtil;
import com.intellij.psi.impl.source.tree.*;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.lang.StdLanguages;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.ArrayList;

class DeclarationMover extends LineMover {
  public DeclarationMover(final boolean isDown) {
    super(isDown);
  }

  protected void afterMove(final Editor editor, final PsiFile file) {
    super.afterMove(editor, file);
    final int line1 = editor.offsetToLogicalPosition(myInsertStartAfterCutOffset).line;
    final int line2 = editor.offsetToLogicalPosition(myInsertEndAfterCutOffset).line;
    Document document = editor.getDocument();
    PsiWhiteSpace whiteSpace1 = findWhitespaceNear(document.getLineStartOffset(line1), file, false);
    PsiWhiteSpace whiteSpace2 = findWhitespaceNear(document.getLineStartOffset(line2), file, false);
    PsiWhiteSpace whiteSpace = findWhitespaceNear(myDeleteStartAfterMoveOffset, file, false);
    fixupWhiteSpace(whiteSpace1);
    fixupWhiteSpace(whiteSpace2);

    fixupWhiteSpace(whiteSpace);
  }

  private static PsiWhiteSpace findWhitespaceNear(final int offset, final PsiFile file, boolean lookRight) {
    PsiElement element = file.findElementAt(offset);
    if (element instanceof PsiWhiteSpace) {
      return (PsiWhiteSpace)element;
    }
    if (element == null) return null;
    element = lookRight ? element.getNextSibling() : element.getPrevSibling();
    return element instanceof PsiWhiteSpace ? (PsiWhiteSpace)element : null;
  }

  private static void fixupWhiteSpace(final PsiWhiteSpace whitespace) {
    if (whitespace == null) return;
    PsiElement element1 = whitespace.getPrevSibling();
    PsiElement element2 = whitespace.getNextSibling();
    if (element2 == null || element1 == null) return;
    PsiDocumentManager documentManager = PsiDocumentManager.getInstance(whitespace.getProject());
    final Document document = documentManager.getDocument(whitespace.getContainingFile());
    String ws = CodeEditUtil.getStringWhiteSpaceBetweenTokens(whitespace.getNode(), element2.getNode(), StdLanguages.JAVA);
    LeafElement node = Factory.createSingleLeafElement(TokenType.WHITE_SPACE, ws.toCharArray(), 0, ws.length(), SharedImplUtil.findCharTableByTree(whitespace.getNode()), whitespace.getManager());
    whitespace.getParent().getNode().replaceChild(whitespace.getNode(), node);
    documentManager.commitDocument(document);
  }

  protected boolean checkAvailable(Editor editor, PsiFile file) {
    if (!(file instanceof PsiJavaFile)) {
      return false;
    }
    boolean available = super.checkAvailable(editor, file);
    if (!available) return false;
    LineRange oldRange = whatToMove;
    final Pair<PsiElement, PsiElement> psiRange = getElementRange(editor, file, oldRange);
    if (psiRange == null) return false;

    final PsiMember firstMember = PsiTreeUtil.getParentOfType(psiRange.getFirst(), PsiMember.class, false);
    final PsiMember lastMember = PsiTreeUtil.getParentOfType(psiRange.getSecond(), PsiMember.class, false);
    if (firstMember == null || lastMember == null) return false;
    LineRange range;
    if (firstMember == lastMember) {
      range = memberRange(firstMember, editor, oldRange);
      if (range == null) return false;
      range.firstElement = range.lastElement = firstMember;
    }
    else {
      final PsiElement parent = PsiTreeUtil.findCommonParent(firstMember, lastMember);
      if (parent == null) return false;

      final Pair<PsiElement, PsiElement> combinedRange = getElementRange(parent, firstMember, lastMember);
      if (combinedRange == null) return false;
      final LineRange lineRange1 = memberRange(combinedRange.getFirst(), editor, oldRange);
      if (lineRange1 == null) return false;
      final LineRange lineRange2 = memberRange(combinedRange.getSecond(), editor, oldRange);
      if (lineRange2 == null) return false;

      range = new LineRange(lineRange1.startLine, lineRange2.endLine);
      range.firstElement = combinedRange.getFirst();
      range.lastElement = combinedRange.getSecond();
    }
    PsiElement nextWhitespace = range.lastElement.getNextSibling();
    if (nextWhitespace instanceof PsiWhiteSpace) {
      int endLine = editor.offsetToLogicalPosition(nextWhitespace.getTextRange().getEndOffset()).line;
      Document document = editor.getDocument();
      while (true) {
        int lineStartOffset = document.getLineStartOffset(endLine);
        int lineEndOffset = document.getLineEndOffset(endLine);
        PsiElement elementAtStart = file.findElementAt(lineStartOffset);
        PsiElement elementAtEnd = file.findElementAt(lineEndOffset - 1);
        if (elementAtEnd == nextWhitespace && elementAtStart == nextWhitespace) break;
        endLine--;
        if (endLine == range.endLine) break;
      }
      LineRange newRange = new LineRange(range.startLine, endLine);
      newRange.firstElement = range.firstElement;
      newRange.lastElement = nextWhitespace;
      range = newRange;
    }


    PsiElement sibling = myIsDown ? range.lastElement.getNextSibling() : range.firstElement.getPrevSibling();
    if (sibling == null) return false;
    final boolean areWeMovingClass = range.firstElement instanceof PsiClass;
    sibling = firstNonWhiteElement(sibling, myIsDown);
    int offset = moveInsideOutsideClassOffset(editor, sibling, myIsDown, areWeMovingClass);
    if (offset == ILLEGAL_MOVE) {
      insertOffset = -1;
      return true;
    }
    if (offset != NOT_CROSSING_CLASS_BORDER) {
      whatToMove = range;
      insertOffset = offset;
      return true;
    }
    if (myIsDown) {
      sibling = sibling.getNextSibling();
      if (sibling == null) return false;
      sibling = firstNonWhiteElement(sibling, myIsDown);
      if (sibling == null) return false;
    }

    whatToMove = range;
    insertOffset = sibling.getTextRange().getStartOffset();
    return true;
  }

  private static LineRange memberRange(@NotNull PsiElement member, Editor editor, LineRange lineRange) {
    final TextRange textRange = member.getTextRange();
    if (editor.getDocument().getTextLength() < textRange.getEndOffset()) return null;
    final int startLine = editor.offsetToLogicalPosition(textRange.getStartOffset()).line;
    final int endLine = editor.offsetToLogicalPosition(textRange.getEndOffset()).line;
    if (!isInsideDeclaration(member, startLine, endLine, lineRange, editor)) return null;

    return new LineRange(startLine, endLine);
  }

  private static boolean isInsideDeclaration(@NotNull final PsiElement member,
                                             final int startLine,
                                             final int endLine,
                                             final LineRange lineRange,
                                             final Editor editor) {
    // if we positioned on member start or end we'll be able to move it
    if (startLine == lineRange.startLine || startLine == lineRange.endLine || endLine == lineRange.startLine ||
        endLine == lineRange.endLine) {
      return true;
    }
    List<PsiElement> memberSuspects = new ArrayList<PsiElement>();
    PsiModifierList modifierList = member instanceof PsiMember ? ((PsiMember)member).getModifierList() : null;
    if (modifierList != null) memberSuspects.add(modifierList);
    if (member instanceof PsiClass) {
      final PsiClass aClass = (PsiClass)member;
      if (aClass instanceof PsiAnonymousClass) return false; // move new expression instead of anon class
      PsiIdentifier nameIdentifier = aClass.getNameIdentifier();
      if (nameIdentifier != null) memberSuspects.add(nameIdentifier);
    }
    if (member instanceof PsiMethod) {
      final PsiMethod method = (PsiMethod)member;
      PsiIdentifier nameIdentifier = method.getNameIdentifier();
      if (nameIdentifier != null) memberSuspects.add(nameIdentifier);
      PsiTypeElement returnTypeElement = method.getReturnTypeElement();
      if (returnTypeElement != null) memberSuspects.add(returnTypeElement);
    }
    if (member instanceof PsiField) {
      final PsiField field = (PsiField)member;
      PsiIdentifier nameIdentifier = field.getNameIdentifier();
      if (nameIdentifier != null) memberSuspects.add(nameIdentifier);
      PsiTypeElement typeElement = field.getTypeElement();
      if (typeElement != null) memberSuspects.add(typeElement);
    }
    TextRange lineTextRange = new TextRange(editor.getDocument().getLineStartOffset(lineRange.startLine), editor.getDocument().getLineEndOffset(lineRange.endLine));
    for (PsiElement suspect : memberSuspects) {
      TextRange textRange = suspect.getTextRange();
      if (textRange != null && lineTextRange.intersects(textRange)) return true;
    }
    return false;
  }

  private static final int ILLEGAL_MOVE = -1;
  private static final int NOT_CROSSING_CLASS_BORDER = -2;
  private static int moveInsideOutsideClassOffset(Editor editor,
                                                  PsiElement sibling,
                                                  final boolean isDown,
                                                  boolean areWeMovingClass) {
    if (sibling == null) return ILLEGAL_MOVE;
    if (sibling instanceof PsiJavaToken &&
        ((PsiJavaToken)sibling).getTokenType() == (isDown ? JavaTokenType.RBRACE : JavaTokenType.LBRACE) &&
        sibling.getParent() instanceof PsiClass) {
      // moving outside class
      final PsiClass aClass = (PsiClass)sibling.getParent();
      final PsiElement parent = aClass.getParent();
      if (!areWeMovingClass && !(parent instanceof PsiClass)) return ILLEGAL_MOVE;
      return isDown ? nextLineOffset(editor, aClass.getTextRange().getEndOffset()) : aClass.getTextRange().getStartOffset();
    }
    if (sibling instanceof PsiClass) {
      // moving inside class
      return isDown
             ? nextLineOffset(editor, ((PsiClass)sibling).getLBrace().getTextOffset())
             : ((PsiClass)sibling).getRBrace().getTextOffset();
    }
    return NOT_CROSSING_CLASS_BORDER;
  }

  private static int nextLineOffset(Editor editor, final int offset) {
    final LogicalPosition position = editor.offsetToLogicalPosition(offset);
    return editor.logicalPositionToOffset(new LogicalPosition(position.line + 1, 0));
  }
}
