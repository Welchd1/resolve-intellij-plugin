package edu.clemson.resolve.plugin.psi.impl;

import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceOwner;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiFileReference;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.PsiTreeUtil;
import edu.clemson.resolve.plugin.ResTypes;
import edu.clemson.resolve.plugin.psi.*;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ResPsiImplUtil {

    @NotNull public static TextRange getUsesTextRange(
            @NotNull ResUsesItem importString) {
        String text = importString.getText();
        return !text.isEmpty() ? TextRange.create(0, text.length() - 1) :
                TextRange.EMPTY_RANGE;
    }

    @NotNull public static PsiElement getIdentifier(ResMathReferenceExp o) {
        return PsiTreeUtil.getChildOfType(o, ResMathNameIdentifier.class);
    }

    @Nullable public static ResMathReferenceExp getQualifier(
            @NotNull ResMathReferenceExp o) {
        return PsiTreeUtil.getChildOfType(o, ResMathReferenceExp.class);
    }

    @NotNull public static ResMathVarLikeReference getReference(
            @NotNull ResMathReferenceExp o) {
        return new ResMathVarLikeReference(o);
    }

    @NotNull public static String getText(@Nullable ResType o) {
        if (o == null) return "";
        String text = o.getText();
        if (text == null) return "";
        return text.replaceAll("\\s+", " ");
    }

    /**
     * ok, in the go plugin don't be fooled by the seeming lack of connection between
     * UsesReferenceHelper and the FileContextProvider -- these are responsible
     * for setting getDefaultContext to "resolve/src/" etc...
     */
    @Nullable public static PsiFile resolve(@NotNull ResUsesItem usesItem) {
        PsiReference[] references = usesItem.getReferences();
        for (PsiReference reference : references) {
            if (reference instanceof FileReferenceOwner) {
                PsiFileReference lastFileReference =
                        ((FileReferenceOwner)reference).getLastFileReference();
                PsiElement result = lastFileReference != null ?
                        lastFileReference.resolve() : null;
                return result instanceof ResFile ? ((ResFile)result) : null;
            }
        }
        return null;
    }

    @NotNull public static PsiReference[] getReferences(@NotNull ResUsesItem o) {
        return PsiReference.EMPTY_ARRAY;
        //if (o.getTextLength() == 0) return PsiReference.EMPTY_ARRAY;
        //return new ResUsesReferenceSet(o).getAllReferences();
    }

    public static boolean processDeclarations(@NotNull ResCompositeElement o,
                                              @NotNull PsiScopeProcessor processor,
                                              @NotNull ResolveState state,
                                              PsiElement lastParent,
                                              @NotNull PsiElement place) {
        boolean isAncestor = PsiTreeUtil.isAncestor(o, place, false);
        //if (o instanceof ResVarSpec) return isAncestor || ResCompositeElementImpl.processDeclarationsDefault(o, processor, state, lastParent, place);
        if (isAncestor) return ResCompositeElementImpl.processDeclarationsDefault(o, processor, state, lastParent, place);

        if (o instanceof ResBlock) { //||
               // o instanceof ResIfStatement ||
               // o instanceof ResWhileStatement  {
            return processor.execute(o, state);
        }
        return ResCompositeElementImpl.processDeclarationsDefault(
                o, processor, state, lastParent, place);
    }

    public static boolean prevDot(@Nullable PsiElement parent) {
        PsiElement prev = parent == null ? null :
                PsiTreeUtil.prevVisibleLeaf(parent);
        return prev instanceof LeafElement &&
                ((LeafElement)prev).getElementType() == ResTypes.DOT;
    }
}
