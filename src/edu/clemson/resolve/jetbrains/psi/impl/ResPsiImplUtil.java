package edu.clemson.resolve.jetbrains.psi.impl;

import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.*;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.FileReferenceOwner;
import com.intellij.psi.impl.source.resolve.reference.impl.providers.PsiFileReference;
import com.intellij.psi.impl.source.tree.LeafElement;
import com.intellij.psi.scope.PsiScopeProcessor;
import com.intellij.psi.util.CachedValueProvider;
import com.intellij.psi.util.CachedValuesManager;
import com.intellij.psi.util.PsiModificationTracker;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.containers.ContainerUtil;
import edu.clemson.resolve.jetbrains.ResTypes;
import edu.clemson.resolve.jetbrains.psi.*;
import edu.clemson.resolve.jetbrains.psi.impl.imports.ResUsesReferenceSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/** Gathered from the language
 *  <a href="https://confluence.jetbrains.com/display/IntelliJIDEA/PSI+Helpers+and+Utilities">tutorial</a>.
 *  This util class is used internally by grammarkit as a repository for
 *  "mixin" methods, which are essentially hand coded methods that can be stuck
 *  into autogenerated {@link PsiElement}s.
 *  <p>
 *  Note that there's nothing inherently special going on here, this class just
 *  needs to be specified in the header of a .bnf with the {@code psiImplUtilClass}
 *  property to be usable by grammarkit.</p>
 */
public class ResPsiImplUtil {

    @NotNull
    public static TextRange getModuleSpecTextRange(@NotNull ResModuleSpec moduleSpec) {
        String text = moduleSpec.getText();
        return !text.isEmpty() ? TextRange.create(0, text.length() - 1) :
                TextRange.EMPTY_RANGE;
    }

    @NotNull
    public static TextRange getPathTextRange(@NotNull ResUsesString usesString) {
        String text = usesString.getText();
        return !text.isEmpty() && isQuote(text.charAt(0)) ?
                TextRange.create(1, text.length() - 1) : TextRange.EMPTY_RANGE;
    }

    @NotNull
    public static String getPath(@NotNull ResUsesSpec usesSpec) {
        return usesSpec.getUsesString().getPath();
    }

    @Nullable
    public static String getName(@NotNull ResUsesSpec usesSpec) {
        return getAlias(usesSpec);
    }

    @Nullable
    public static String getAlias(@NotNull ResUsesSpec usesSpec) {
        PsiElement identifier = usesSpec.getIdentifier();
        if (identifier != null) {
            return identifier.getText();
        }
        return null;
    }

    public static boolean shouldGoDeeper(@SuppressWarnings("UnusedParameters") ResUsesSpec o) {
        return false;
    }

    private static boolean isQuote(char q) {
        return q == '"';
    }

    @Nullable
    public static PsiElement resolve(@NotNull ResUsesString importString) {
        PsiReference[] references = importString.getReferences();
        for (PsiReference reference : references) {
            if (reference instanceof FileReferenceOwner) {
                PsiFileReference lastFileReference =
                        ((FileReferenceOwner) reference).getLastFileReference();
                PsiElement result = lastFileReference != null ?
                        lastFileReference.resolve() : null;

                return (result instanceof PsiDirectory) || (result instanceof ResFile) ? result : null;
            }
        }
        return null;
    }

    @NotNull
    public static PsiReference[] getReferences(@NotNull ResUsesString o) {
        if (o.getTextLength() < 2) return PsiReference.EMPTY_ARRAY;
        return new ResUsesReferenceSet(o).getAllReferences();
    }

    @NotNull
    public static String getPath(@NotNull ResUsesString o) {
        return unquote(o.getText());
    }

    @NotNull
    private static String unquote(@Nullable String s) {
        if (StringUtil.isEmpty(s)) return "";
        char quote = s.charAt(0);
        int startOffset = isQuote(quote) ? 1 : 0;
        int endOffset = s.length();
        if (s.length() > 1) {
            char lastChar = s.charAt(s.length() - 1);
            if (isQuote(quote) && lastChar == quote) {
                endOffset = s.length() - 1;
            }
            if (!isQuote(quote) && isQuote(lastChar)) {
                endOffset = s.length() - 1;
            }
        }
        return s.substring(startOffset, endOffset);
    }

    @Nullable
    public static ResFile getSpecification(ResFacilityDecl o) {
        if (o.getModuleSpecList().isEmpty()) return null;
        ResModuleSpec specification = o.getModuleSpecList().get(0);
        PsiFile specFile = specification.resolve();
        if (!(specFile instanceof ResFile)) return null;
        return (ResFile) specFile;
    }

    @NotNull
    public static PsiElement getIdentifier(ResMathReferenceExp o) {
        return PsiTreeUtil.getChildOfType(o, ResMathSymbolName.class);
    }

    @Nullable
    public static ResMathReferenceExp getQualifier(@NotNull ResMathReferenceExp o) {
        return PsiTreeUtil.getChildOfType(o, ResMathReferenceExp.class);
    }

    @Nullable
    public static ResTypeReferenceExp getQualifier(@NotNull ResTypeReferenceExp o) {
        return PsiTreeUtil.getChildOfType(o, ResTypeReferenceExp.class);
    }

    @Nullable
    public static ResReferenceExp getQualifier(@NotNull ResReferenceExp o) {
        ResReferenceExp child =
                PsiTreeUtil.getChildOfType(o, ResReferenceExp.class);
        return child;
    }

    @Nullable
    public static PsiElement resolve(@NotNull ResTypeReferenceExp o) { // todo: replace with default method in GoReferenceExpressionBase
        return o.getReference().resolve();
    }

    @NotNull
    public static PsiReference getReference(@NotNull ResTypeReferenceExp o) {
        return new ResTypeReference(o);
    }

    @NotNull
    public static ResReference getReference(@NotNull ResReferenceExp o) {
        return new ResReference(o);
    }

    @NotNull
    public static ResMathVarLikeReference getReference(@NotNull ResMathReferenceExp o) {
        return new ResMathVarLikeReference(o);
    }

    @Nullable
    public static PsiReference getReference(@NotNull ResVarDef o) {
        return new ResVarReference(o);
    }

    @NotNull
    public static PsiReference[] getReferences(@NotNull ResModuleSpec o) {
        if (o.getTextLength() == 0) return PsiReference.EMPTY_ARRAY;
        return new ResUsesReferenceSet(o).getAllReferences();
    }

    @NotNull
    public static String getText(@Nullable ResType o) {
        if (o == null) return "";
        String text = o.getText();
        if (text == null) return "";
        return text.replaceAll("\\s+", " ");
    }

    @Nullable
    public static ResType getResTypeInner(@NotNull ResTypeReprDecl o,
                                          @SuppressWarnings("UnusedParameters")
                                          @Nullable ResolveState context) {
        return o.getType();
    }

    @Nullable
    public static ResType getResType(@NotNull final ResExp o, @Nullable final ResolveState context) {
        return RecursionManager.doPreventingRecursion(o, true, new Computable<ResType>() {
            @Override
            public ResType compute() {
                if (context != null) return getResTypeInner(o, context);
                return CachedValuesManager.getCachedValue(o, new CachedValueProvider<ResType>() {
                    @Nullable
                    @Override
                    public Result<ResType> compute() {
                        return Result.create(getResTypeInner(o, null),
                                PsiModificationTracker.MODIFICATION_COUNT);
                    }
                });
            }
        });
    }

    @Nullable
    public static ResType getResTypeInner(@NotNull final ResExp o, @Nullable ResolveState context) {
        if (o instanceof ResReferenceExp) {
            PsiReference reference = o.getReference();
            PsiElement resolve = reference != null ? reference.resolve() : null;

            //TODO: Look closer at this line, I really don't think we need TypeOwner... wait... no. just Make
            //sure ResNamedElement doesn't extend ResTypeOwner. Do it only for Exp and ResTypeRepr, paramDef, varDef, OperationDecl, OpProcedureDecl, etc.
            //some code dup but I kinda like it better. Let me think about it more.
            if (resolve instanceof ResTypeOwner) return ((ResTypeOwner) resolve).getResType(context);
        }
        else if (o instanceof ResSelectorExp) {
            ResExp item = ContainerUtil.getLastItem(((ResSelectorExp) o).getExpList());
            return item != null ? item.getResType(context) : null;
        }
        return null;
    }

    @Nullable
    public static ResType getResTypeInner(@NotNull ResVarDef o, @Nullable ResolveState context) {
        PsiElement parent = o.getParent();
        if (parent instanceof ResVarSpec) {
            return ((ResVarSpec) parent).getType();
        }
        return null;
    }

    /**
     * ok, in the go jetbrains don't be fooled by the seeming lack of connection between
     * {@code UsesReferenceHelper} and the {@link FileContextProvider} -- these are responsible
     * for setting {@link }getDefaultContext to "resolve/src/" etc...
     */
    @Nullable
    public static PsiFile resolve(@NotNull ResModuleSpec moduleSpec) {
        PsiReference[] references = moduleSpec.getReferences();
        for (PsiReference reference : references) {
            if (reference instanceof FileReferenceOwner) {
                PsiFileReference lastFileReference =
                        ((FileReferenceOwner) reference).getLastFileReference();
                PsiElement result = lastFileReference != null ?
                        lastFileReference.resolve() : null;
                return result instanceof ResFile ? ((ResFile) result) : null;
            }
        }
        return null;
    }

    public static boolean processDeclarations(@NotNull ResCompositeElement o,
                                              @NotNull PsiScopeProcessor processor,
                                              @NotNull ResolveState state,
                                              PsiElement lastParent,
                                              @NotNull PsiElement place) {
        boolean isAncestor = PsiTreeUtil.isAncestor(o, place, false);
        if (o instanceof ResVarSpec) {
            return isAncestor || ResCompositeElementImpl.processDeclarationsDefault(o, processor, state, lastParent, place);
        }
        if (isAncestor) {
            return ResCompositeElementImpl.processDeclarationsDefault(o, processor, state, lastParent, place);
        }
        if (o instanceof ResBlock) { //||
            // o instanceof ResIfStatement ||
            // o instanceof ResWhileStatement  {
            return processor.execute(o, state);
        }
        return ResCompositeElementImpl.processDeclarationsDefault(
                o, processor, state, lastParent, place);
    }

    public static boolean prevDot(@Nullable PsiElement parent) {
        PsiElement prev = parent == null ? null : PsiTreeUtil.prevVisibleLeaf(parent);
        return prev instanceof LeafElement && ((LeafElement) prev).getElementType() == ResTypes.DOT;
    }

    /** An expression describing the type of a mathematical expression
     *  {@code o} written in terms of another mathematical expression.
     */
    @Nullable
    public static ResMathExp getResMathMetaTypeExp(@NotNull final ResMathExp o,
                                                   @Nullable final ResolveState context) {
        return RecursionManager.doPreventingRecursion(o, true,
                new Computable<ResMathExp>() {
                    @Override
                    public ResMathExp compute() {
                        if (context != null)
                            return getResMathTypeMetaExpInner(o, context);
                        return CachedValuesManager.getCachedValue(o,
                                new CachedValueProvider<ResMathExp>() {
                                    @Nullable
                                    @Override
                                    public Result<ResMathExp> compute() {
                                        return Result.create(getResMathTypeMetaExpInner(o, null),
                                                PsiModificationTracker.MODIFICATION_COUNT);
                                    }
                                });
                    }
                });
    }

    @Nullable
    public static ResMathExp getResMathMetaTypeExpInner(@NotNull ResExemplarDecl o,
                                                        @SuppressWarnings("UnusedParameters")
                                                        @Nullable ResolveState context) {
        ResTypeModelDecl model = PsiTreeUtil.getParentOfType(o, ResTypeModelDecl.class);
        return model == null ? null : model.getMathExp();
    }

    @Nullable
    public static ResMathExp getResMathTypeMetaExpInner(@NotNull final ResMathExp o,
                                                        @Nullable ResolveState context) {
        if (o instanceof ResMathReferenceExp) {
            PsiReference reference = o.getReference();
            PsiElement resolve = reference != null ? reference.resolve() : null;
            if (resolve instanceof ResMathMetaTypeExpOwner) {
                return ((ResMathMetaTypeExpOwner) resolve).getResMathMetaTypeExp(context);
            }
        }
        else if (o instanceof ResMathSelectorExp) {
            ResMathExp item = ContainerUtil.getLastItem(
                    ((ResMathSelectorExp) o).getMathExpList());
            return item != null ? item.getResMathMetaTypeExp(context) : null;
        }
        return null;
    }
}