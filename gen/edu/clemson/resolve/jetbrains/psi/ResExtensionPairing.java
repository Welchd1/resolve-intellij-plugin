// This is a generated file. Not intended for manual editing.
package edu.clemson.resolve.jetbrains.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface ResExtensionPairing extends ResCompositeElement {

  @NotNull
  List<ResModuleArgList> getModuleArgListList();

  @NotNull
  List<ResModuleIdentifier> getModuleIdentifierList();

  @NotNull
  List<ResModuleLibraryIdentifier> getModuleLibraryIdentifierList();

  @NotNull
  PsiElement getExtended();

  @Nullable
  PsiElement getExternally();

  @Nullable
  PsiElement getRealized();

  @Nullable
  ResFile resolveSpecification();

  @Nullable
  ResFile resolveImplementation();

}
