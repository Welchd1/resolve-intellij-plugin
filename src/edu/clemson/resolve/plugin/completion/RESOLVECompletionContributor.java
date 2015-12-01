package edu.clemson.resolve.plugin.completion;

import com.intellij.codeInsight.completion.CompletionContributor;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.patterns.PsiElementPattern;
import com.intellij.psi.PsiElement;
import edu.clemson.resolve.plugin.psi.ResMathIdentifierSymbol;
import edu.clemson.resolve.plugin.psi.ResReferenceExpBase;

import static com.intellij.patterns.PlatformPatterns.*;

public class RESOLVECompletionContributor extends CompletionContributor {

    public RESOLVECompletionContributor() {
        extend(CompletionType.BASIC, referenceExp(),
                new RESOLVEReferenceCompletionProvider());
        extend(CompletionType.BASIC, mathReferenceExp(),
                new RESOLVEReferenceCompletionProvider());
        //extend(CompletionType.BASIC, resReference(),
        //        new RESOLVEReferenceCompletionProvider());
    }

    private static PsiElementPattern.Capture<PsiElement> referenceExp() {
        return psiElement().withParent(ResReferenceExpBase.class);
    }

    private static PsiElementPattern.Capture<PsiElement> mathReferenceExp() {
        return psiElement().withParent(psiElement(ResMathIdentifierSymbol.class)
                .withParent(ResReferenceExpBase.class));
    }


    // private static PsiElementPattern.Capture<PsiElement> resReference() {
   //     return psiElement().withParent(psiElement()
   //             .withReference(ResCachedReference.class));
   // }

    /**
     * Allow autoPopup to appear after custom symbol
     */
   // public boolean invokeAutoPopup(@NotNull PsiElement position, char typeChar) {
   //     return typeChar != ';' && typeChar != ' ' && typeChar != ')';
   // }
}
