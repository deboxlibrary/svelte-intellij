// This is a generated file. Not intended for manual editing.
package dev.blachut.svelte.lang.psi;

import java.util.List;
import org.jetbrains.annotations.*;
import com.intellij.psi.PsiElement;

public interface SvelteAwaitBlock extends PsiElement {

  @Nullable
  SvelteAwaitBlockClosing getAwaitBlockClosing();

  @Nullable
  SvelteAwaitBlockOpening getAwaitBlockOpening();

  @Nullable
  SvelteAwaitThenBlockOpening getAwaitThenBlockOpening();

  @Nullable
  SvelteCatchContinuation getCatchContinuation();

  @Nullable
  SvelteThenContinuation getThenContinuation();

}
