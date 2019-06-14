/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */
package com.intellij.mock;

import com.intellij.openapi.module.Module;
import com.intellij.openapi.module.UnloadedModuleDescription;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.roots.FileIndexFacade;
import com.intellij.openapi.util.ModificationTracker;
import com.intellij.openapi.vfs.VfsUtilCore;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import com.intellij.openapi.vfs.impl.jar.CoreJarFileSystem;
import com.intellij.openapi.vfs.impl.jar.CoreJarVirtualFile;
import com.intellij.openapi.vfs.local.CoreLocalFileSystem;
import com.intellij.openapi.vfs.local.CoreLocalVirtualFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;

/**
 * @author yole
 */
public class MockFileIndexFacade extends FileIndexFacade {
    private final Module myModule;
    private final List<VirtualFile> myLibraryRoots = new ArrayList<>();

    public MockFileIndexFacade(final Project project) {
        super(project);
        myModule = null;  // TODO
    }

    @Override
    public boolean isInContent(@NotNull VirtualFile file) {
        return true;
    }

    @Override
    public boolean isInSource(@NotNull VirtualFile file) {
        return true;
    }

    @Override
    public boolean isInSourceContent(@NotNull VirtualFile file) {
        return true;
    }

    static {
        System.out.println("Using hacked MockFileIndexFacade");
    }

    @Override
    public boolean isInLibraryClasses(@NotNull VirtualFile file) {
        for (VirtualFile ancestor : myLibraryRoots) {
            if (!file.getFileSystem().equals(ancestor.getFileSystem())) continue;

            VirtualFileSystem vfs = file.getFileSystem();
            if (vfs instanceof CoreLocalFileSystem) {
                if (file.getPath().startsWith(ancestor.getPath())) {
                    return true;
                }
            }
            else {
                if (vfs instanceof CoreJarFileSystem) {
                    CoreJarVirtualFile jarAncestor = (CoreJarVirtualFile) ancestor;
                    CoreJarVirtualFile jarFile = (CoreJarVirtualFile) file;
                    if (jarAncestor.getHandler() != jarFile.getHandler()) continue;
                }
                if (VfsUtilCore.isAncestor(ancestor, file, false)) {
                    return true;
                }
            }
        }
        return false;
    }

    @Override
    public boolean isInLibrarySource(@NotNull VirtualFile file) {
        return false;
    }

    @Override
    public boolean isExcludedFile(@NotNull VirtualFile file) {
        return false;
    }

    @Override
    public boolean isUnderIgnored(@NotNull VirtualFile file) {
        return false;
    }

    @Override
    public Module getModuleForFile(@NotNull VirtualFile file) {
        return myModule;
    }

    @Override
    public boolean isValidAncestor(@NotNull VirtualFile baseDir, @NotNull VirtualFile child) {
        return VfsUtilCore.isAncestor(baseDir, child, false);
    }

    @NotNull
    @Override
    public ModificationTracker getRootModificationTracker() {
        return ModificationTracker.NEVER_CHANGED;
    }

    @NotNull
    @Override
    public Collection<UnloadedModuleDescription> getUnloadedModuleDescriptions() {
        return Collections.emptySet();
    }

    public void addLibraryRoot(VirtualFile file) {
        myLibraryRoots.add(file);
    }
}
