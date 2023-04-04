/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.linkis.storage.utils;

import org.apache.linkis.common.io.FsPath;
import org.apache.linkis.storage.FSFactory;
import org.apache.linkis.storage.fs.FileSystem;
import org.apache.linkis.storage.fs.impl.LocalFileSystem;

import java.io.IOException;
import java.util.Stack;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FileSystemUtils {
  private static final Logger logger = LoggerFactory.getLogger(FileSystemUtils.class);

  public static void copyFile(FsPath filePath, FsPath origin, String user) throws Exception {
    FileSystem fileSystem = (FileSystem) FSFactory.getFsByProxyUser(filePath, user);
    try {
      fileSystem.init(null);
      if (!fileSystem.exists(filePath)) {
        if (!fileSystem.exists(filePath.getParent())) {
          fileSystem.mkdirs(filePath.getParent());
        }
        fileSystem.createNewFile(filePath);
      }
      fileSystem.copyFile(origin, filePath);
    } finally {
      fileSystem.close();
    }
  }

  /**
   * Create a new file
   *
   * @param filePath
   * @param createParentWhenNotExists Whether to recursively create a directory
   */
  public static void createNewFile(FsPath filePath, boolean createParentWhenNotExists)
      throws Exception {
    createNewFile(filePath, StorageUtils.getJvmUser(), createParentWhenNotExists);
  }

  public static void createNewFile(FsPath filePath, String user, boolean createParentWhenNotExists) {
    FileSystem fileSystem = (FileSystem) FSFactory.getFsByProxyUser(filePath, user);
    try {
      fileSystem.init(null);
      createNewFileWithFileSystem(fileSystem, filePath, user, createParentWhenNotExists);
    } catch (IOException e) {
      logger.warn("FileSystemUtils createNewFile failed", e);
    } catch (Exception e) {
      logger.warn("FileSystemUtils createNewFile failed", e);
    } finally {
      try {
        fileSystem.close();
      } catch (IOException e) {
        logger.warn("FileSystemUtils close failed", e);
      }
    }
  }

  public static void createNewFileWithFileSystem(
      FileSystem fileSystem, FsPath filePath, String user, boolean createParentWhenNotExists)
      throws Exception {
    if (!fileSystem.exists(filePath)) {
      if (!fileSystem.exists(filePath.getParent())) {
        if (!createParentWhenNotExists) {
          throw new IOException(
              "parent dir " + filePath.getParent().getPath() + " dose not exists.");
        }
        mkdirs(fileSystem, filePath.getParent(), user);
      }
      fileSystem.createNewFile(filePath);
      if (fileSystem instanceof LocalFileSystem) {
        fileSystem.setOwner(filePath, user);
      } else {
        logger.info("doesn't need to call setOwner");
      }
    }
  }

  /**
   * Recursively create a directory
   *
   * @param fileSystem
   * @param dest
   * @param user
   * @throws IOException
   * @return
   */
  public static boolean mkdirs(FileSystem fileSystem, FsPath dest, String user) throws IOException {
    FsPath parentPath = dest.getParent();
    Stack<FsPath> dirsToMake = new Stack<>();
    dirsToMake.push(dest);
    while (!fileSystem.exists(parentPath)) {
      dirsToMake.push(parentPath);
      parentPath = parentPath.getParent();
    }
    if (!fileSystem.canExecute(parentPath)) {
      throw new IOException("You have not permission to access path " + dest.getPath());
    }
    while (!dirsToMake.empty()) {
      FsPath path = dirsToMake.pop();
      fileSystem.mkdir(path);
      if (fileSystem instanceof LocalFileSystem) {
        fileSystem.setOwner(path, user);
      } else {
        logger.info("doesn't need to call setOwner");
      }
    }
    return true;
  }
}
