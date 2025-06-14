package com.example.apieditor.fs;

import com.example.apieditor.model.ApiEndpoint;
import com.example.apieditor.model.RemoteProgram;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.openapi.fileTypes.FileTypeManager;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.openapi.vfs.VirtualFileSystem;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Virtual file implementation for remote programs.
 */
public class ApiEditorVirtualFile extends VirtualFile {
    private final ApiEndpoint endpoint;
    private RemoteProgram program;
    private byte[] content;
    private long modificationStamp = 0;
    private boolean isWritable = true;

    public ApiEditorVirtualFile(ApiEndpoint endpoint, RemoteProgram program) {
        this.endpoint = endpoint;
        updateProgram(program);
    }

    public void updateProgram(RemoteProgram program) {
        this.program = program;
        this.content = program.getContent() != null
                ? program.getContent().getBytes(StandardCharsets.UTF_8)
                : new byte[0];
        this.modificationStamp++;
    }

    public ApiEndpoint getEndpoint() {
        return endpoint;
    }

    public RemoteProgram getProgram() {
        return program;
    }

    @NotNull
    @Override
    public String getName() {
        return program.getFullName() + " [" + endpoint.getName() + "]";
    }

    @NotNull
    @Override
    public VirtualFileSystem getFileSystem() {
        // We're not implementing a full VirtualFileSystem, so we return null
        // This is a simplification for the example
        return ApiEditorVirtualFileSystem.getInstance();
    }

    @NotNull
    @Override
    public String getPath() {
        return "/api/" + endpoint.getName() + "/" + program.getFullName();
    }

    @Override
    public boolean isWritable() {
        return isWritable;
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public VirtualFile getParent() {
        return null;
    }

    @Override
    public VirtualFile[] getChildren() {
        return VirtualFile.EMPTY_ARRAY;
    }

    @NotNull
    @Override
    public OutputStream getOutputStream(Object requestor, long newModificationStamp, long newTimeStamp) throws IOException {
        return new ByteArrayOutputStream() {
            @Override
            public void close() throws IOException {
                super.close();
                byte[] bytes = toByteArray();
                content = bytes;
                program.setContent(new String(bytes, StandardCharsets.UTF_8));
                modificationStamp = newModificationStamp;

                // Save the program to the remote API
                ApiEditorFileSystem.getInstance().saveProgram(ApiEditorVirtualFile.this);
            }
        };
    }

    @NotNull
    @Override
    public byte[] contentsToByteArray() throws IOException {
        return content;
    }

    @Override
    public long getTimeStamp() {
        return program.getLastModified();
    }

    @Override
    public long getLength() {
        return content.length;
    }

    @Override
    public void refresh(boolean asynchronous, boolean recursive, @Nullable Runnable postRunnable) {
        // No-op for this implementation
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }

    @Override
    public long getModificationStamp() {
        return modificationStamp;
    }

    @Override
    public @NotNull FileType getFileType() {
        String extension = program.getExtension();
        if (extension == null || extension.isEmpty()) {
            return FileTypeManager.getInstance().getFileTypeByFileName(program.getName());
        }
        return FileTypeManager.getInstance().getFileTypeByExtension(extension);
    }
}
