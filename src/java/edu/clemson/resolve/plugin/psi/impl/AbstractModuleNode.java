package edu.clemson.resolve.plugin.psi.impl;

import com.intellij.extapi.psi.ASTWrapperPsiElement;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.psi.PsiElement;
import com.intellij.usageView.UsageViewUtil;
import com.intellij.util.IncorrectOperationException;
import edu.clemson.resolve.plugin.RESOLVETokenTypes;
import edu.clemson.resolve.plugin.parser.ResolveLexer;
import edu.clemson.resolve.plugin.psi.Module;
import org.antlr.intellij.adaptor.parser.PsiElementFactory;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;

public abstract class AbstractModuleNode
        extends
            ResolveCompositeElementImpl implements Module {

    public AbstractModuleNode(@NotNull ASTNode node) {
        super(node);
    }

    @Override @NotNull public PsiElement getIdentifier() {
        return findNotNullChildByType(RESOLVETokenTypes.TOKEN_ELEMENT_TYPES
                .get(ResolveLexer.ID));
    }

    @Nullable @Override public PsiElement getNameIdentifier() {
        return getIdentifier();
    }

    @Override public boolean isPublic() {
        return true;
    }

    @NotNull @Override public PsiElement setName(
            @NonNls @NotNull String newName)
            throws IncorrectOperationException {
        PsiElement identifier = getIdentifier();
        identifier.replace(ResolveElementFactory
                .createIdentifierFromText(getProject(), newName));
        return this;
    }

}
