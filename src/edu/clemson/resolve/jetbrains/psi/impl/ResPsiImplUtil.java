package edu.clemson.resolve.jetbrains.psi.impl;

import com.intellij.openapi.util.Computable;
import com.intellij.openapi.util.RecursionManager;
import com.intellij.openapi.util.TextRange;
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
import com.intellij.util.containers.Predicate;
import edu.clemson.resolve.jetbrains.ResTypes;
import edu.clemson.resolve.jetbrains.psi.*;
import edu.clemson.resolve.jetbrains.psi.impl.imports.ResModuleLibraryReference;
import edu.clemson.resolve.jetbrains.psi.impl.imports.ResModuleReference;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Gathered from the language
 * <a href="https://confluence.jetbrains.com/display/IntelliJIDEA/PSI+Helpers+and+Utilities">tutorial</a>.
 * This util class is used internally by the grammarkit tool as a repository for "mixin" methods, which are essentially
 * handcoded methods that can be stuck into autogenerated {@link PsiElement}s.
 * <p>
 * Note that there's nothing inherently special going on here, this class just needs to be specified in the header of
 * a .bnf with the {@code psiImplUtilClass} property to be usable by grammarkit.</p>
 */
public class ResPsiImplUtil {

    @Nullable
    public static ResModuleLibraryIdentifier getFromLibraryIdentifier(@NotNull ResModuleIdentifierSpec moduleIdentifierSpec) {
        return moduleIdentifierSpec.getModuleLibraryIdentifier();
    }

    @Nullable
    public static PsiElement getAlias(@NotNull ResModuleIdentifierSpec moduleIdentifierSpec) {
        return moduleIdentifierSpec.getIdentifier();
    }

    @NotNull
    public static TextRange getModuleIdentiferTextRange(@NotNull ResModuleIdentifier moduleIdentifier) {
        String text = moduleIdentifier.getText();
        return !text.isEmpty() ? TextRange.create(0, text.length() - 1) : TextRange.EMPTY_RANGE;
    }

    @NotNull
    public static TextRange getModuleLibraryIdentiferTextRange(@NotNull ResModuleLibraryIdentifier libraryIdentifier) {
        String text = libraryIdentifier.getText();
        return !text.isEmpty() ? TextRange.create(0, text.length() - 1) : TextRange.EMPTY_RANGE;
    }

    @NotNull
    public static TextRange getModuleInlineIdentiferTextRange(@NotNull ResModuleInlineIdentifier libraryIdentifier) {
        String text = libraryIdentifier.getText();
        return !text.isEmpty() ? TextRange.create(0, text.length() - 1) : TextRange.EMPTY_RANGE;
    }

    /**
     * Note that we don't extend {@link PsiPolyVariantReference} for module references (like we do for
     * {@link ResTypeReference}, and {@link ResReference}), instead we simply represent them in the PSI as a
     * {@link ResReferenceExp}.
     * <p>
     * And since {@link ResReference} is the reference type/resolver for all reference exps, we just need a method to
     * indicate situations where we only expect to resolve to other modules. Accordingly, the only place I can see
     * where we actually use ordinary PSI reference exps to reference a <em>module</em> is within facility decl nodes --
     * so this should really only be needed in
     * {@link ResReference#processUsesImports(ResFile, ResScopeProcessor, ResolveState)}.</p>
     *
     * @param o an arbitrary reference expression.
     *
     * @return whether or not {@code o} should resolve back to some {@link PsiFile} containing a module.
     */
    public static boolean shouldReferenceModule(ResReferenceExp o) {
        return PsiTreeUtil.getParentOfType(o, ResFacilityDecl.class) != null &&
                PsiTreeUtil.getParentOfType(o, ResModuleArgList.class) == null;
    }

    @Nullable
    private static PsiElement resolveModuleOrLibraryIdentifier(@NotNull PsiReference[] references,
                                                               @NotNull Predicate<PsiElement> p) {
        for (PsiReference reference : references) {
            if (reference instanceof FileReferenceOwner) {
                PsiFileReference lastFileReference = ((FileReferenceOwner) reference).getLastFileReference();
                PsiElement result = lastFileReference != null ? lastFileReference.resolve() : null;
                return p.apply(result) ? result : null;
            }
        }
        return null;
    }

    @NotNull
    public static String getName(@NotNull ResModuleIdentifierSpec moduleIdentifierSpec) {
        return moduleIdentifierSpec.getAlias() != null ? moduleIdentifierSpec.getAlias().getText() :
                moduleIdentifierSpec.getModuleIdentifier().getText();
    }

    @NotNull
    public static PsiReference[] getReferences(@NotNull ResModuleIdentifier o) {
        if (o.getTextLength() < 1) return PsiReference.EMPTY_ARRAY;
        return new ResModuleReference.ResModuleReferenceSet(o).getAllReferences();
    }

    @NotNull
    public static PsiReference[] getReferences(@NotNull ResModuleLibraryIdentifier o) {
        if (o.getTextLength() < 1) return PsiReference.EMPTY_ARRAY;
        return new ResModuleLibraryReference.ResModuleLibraryReferenceSet(o).getAllReferences();
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
        return PsiTreeUtil.getChildOfType(o, ResReferenceExp.class);
    }

    //TODO: replace with default method in ResReferenceExpBase
    @Nullable
    public static PsiElement resolve(@NotNull ResReferenceExp o) {
        return o.getReference().resolve();
    }

    @Nullable
    public static PsiElement resolve(@NotNull ResTypeReferenceExp o) { // todo: replace with default method in GoReferenceExpressionBase
        return o.getReference().resolve();
    }

    @Nullable
    public static PsiElement resolve(@NotNull ResModuleIdentifier moduleIdentifier) {
        return resolveModuleOrLibraryIdentifier(moduleIdentifier.getReferences(), e -> e instanceof ResFile);
    }

    @Nullable
    public static PsiElement resolve(@NotNull ResModuleLibraryIdentifier libraryIdentifier) {
        return resolveModuleOrLibraryIdentifier(libraryIdentifier.getReferences(), e -> e instanceof PsiDirectory);
    }

    @Nullable
    public static PsiElement resolve(@NotNull ResModuleInlineIdentifier moduleInlineIdentifier) {
        return resolveModuleOrLibraryIdentifier(moduleInlineIdentifier.getReferences(), e -> e instanceof ResFile);
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
/*    @Nullable
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
*/
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

    @Nullable
    public static ResFile resolveSpecification(ResFacilityDecl o) {
        //if (o.getReferenceExpList().isEmpty()) return null;
        //ResReferenceExp specification = o.getReferenceExpList().get(0);
        //PsiElement result = specification.resolve();
        return null; //result instanceof ResFile ? (ResFile) result : null;
    }


    public static boolean prevDot(@Nullable PsiElement parent) {
        PsiElement prev = parent == null ? null : PsiTreeUtil.prevVisibleLeaf(parent);
        return prev instanceof LeafElement && ((LeafElement) prev).getElementType() == ResTypes.DOT;
    }

    /**
     * An expression describing the type of a mathematical expression
     * {@code o} written in terms of another mathematical expression.
     */
    @Nullable
    public static ResMathExp getResMathMetaTypeExp(@NotNull final ResMathExp o,
                                                   @Nullable final ResolveState context) {
        return RecursionManager.doPreventingRecursion(o, true,
                new Computable<ResMathExp>() {
                    @Override
                    public ResMathExp compute() {
                        if (context != null) return getResMathTypeMetaExpInner(o, context);
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
    public static ResMathExp getResMathTypeMetaExpInner(@NotNull final ResMathExp o, @Nullable ResolveState context) {
        if (o instanceof ResMathReferenceExp) {
            PsiReference reference = o.getReference();
            PsiElement resolve = reference != null ? reference.resolve() : null;
            if (resolve instanceof ResMathMetaTypeExpOwner) {
                return ((ResMathMetaTypeExpOwner) resolve).getResMathMetaTypeExp(context);
            }
        }
        else if (o instanceof ResMathSelectorExp) {
            ResMathExp item = ContainerUtil.getLastItem(((ResMathSelectorExp) o).getMathExpList());
            return item != null ? item.getResMathMetaTypeExp(context) : null;
        }
        return null;
    }
}