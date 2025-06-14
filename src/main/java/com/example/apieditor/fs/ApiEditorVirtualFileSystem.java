package com.example.apieditor.fs;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.components.Service;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileListener;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * Virtual file system implementation for API Editor.
 */
@Service
public final class ApiEditorVirtualFileSystem extends VirtualFileSystem {
    private static final String PROTOCOL = "apieditor";

    public static ApiEditorVirtualFileSystem getInstance() {
        return ApplicationManager.getApplication().getService(ApiEditorVirtualFileSystem.class);
    }

    @Override
    public @NotNull String getProtocol() {
        return PROTOCOL;
    }

    @Override
    public @Nullable VirtualFile findFileByPath(@NotNull @NonNls String path) {
        // This is a simplified implementation
        // In a real implementation, we would parse the path and find the corresponding file
        return null;
    }

    @Override
    public void refresh(boolean asynchronous) {
        // No-op for this implementation
    }

    @Override
    public @Nullable VirtualFile refreshAndFindFileByPath(@NotNull String path) {
        // This is a simplified implementation
        return findFileByPath(path);
    }

    @Override
    public void addVirtualFileListener(@NotNull VirtualFileListener listener) {
        // No-op for this implementation
    }

    @Override
    public void removeVirtualFileListener(@NotNull VirtualFileListener listener) {
        // No-op for this implementation
    }

    @Override
    public boolean isReadOnly() {
        return false; // Our file system allows writing to files
    }

    @Override
    protected void deleteFile(Object requestor, @NotNull VirtualFile vFile) throws IOException {
        throw new UnsupportedOperationException("Delete operation is not supported");
    }

    @Override
    protected void moveFile(Object requestor, @NotNull VirtualFile vFile, @NotNull VirtualFile newParent) throws IOException {
        throw new UnsupportedOperationException("Move operation is not supported");
    }

    @Override
    protected void renameFile(Object requestor, @NotNull VirtualFile vFile, @NotNull String newName) throws IOException {
        throw new UnsupportedOperationException("Rename operation is not supported");
    }

    @Override
    protected @NotNull VirtualFile createChildFile(Object requestor, @NotNull VirtualFile vDir, @NotNull String fileName) throws IOException {
        throw new UnsupportedOperationException("Create operation is not supported");
    }

    @Override
    protected @NotNull VirtualFile createChildDirectory(Object requestor, @NotNull VirtualFile vDir, @NotNull String dirName) throws IOException {
        throw new UnsupportedOperationException("Create directory operation is not supported");
    }

    @Override
    protected @NotNull VirtualFile copyFile(Object requestor, @NotNull VirtualFile vFile, @NotNull VirtualFile newParent, @NotNull String copyName) throws IOException {
        throw new UnsupportedOperationException("Copy operation is not supported");
    }
}
