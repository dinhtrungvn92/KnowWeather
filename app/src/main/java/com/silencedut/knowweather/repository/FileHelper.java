/**
 * Copyright (C) 2015 Fernando Cejas Open Source Project
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.silencedut.knowweather.repository;

import android.content.Context;
import android.os.Environment;

import com.silencedut.knowweather.scheduler.TaskCallback;
import com.silencedut.knowweather.scheduler.TaskScheduler;
import com.silencedut.knowweather.scheduler.exception.ErrorBundle;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;

/**
 * Created by SilenceDut on 17/04/19.
 * Helper class to do operations on regular files/directories.
 */

public class FileHelper {

    private static Context mContext;
    private static final String DEFAULT_FILE_NAME = "werewolf_cache";

    private FileHelper() {

    }

    public static void init() {
        getAppFolder();
    }

    /**
     * SDCard root
     */
    public static String sdcardRoot() {
        return Environment.getExternalStorageDirectory().toString();
    }

    public static String getAppFolder() {
        // Create the application workspace
        File mainDir = new File(sdcardRoot() + File.separator + DEFAULT_FILE_NAME + File.separator);
        makeDir(mainDir);
        return mainDir.getPath();
    }

    public static boolean makeDir(File dir) {
        if (!dir.exists()) {
            dir.mkdirs();
        }
        return (dir.exists() && dir.isDirectory());
    }

    /**
     * Build a file, used to be inserted in the disk cache.
     *
     * @param fileId The name build the file.
     * @return A valid file.
     */
    public static File buildFile(String fileId) {
        String fileNameBuilder =sdcardRoot() + File.separator + DEFAULT_FILE_NAME + fileId;

        return new File(fileNameBuilder);
    }

    /**
     *
     * @param fileId The fileId to write to Disk.
     */
    public static void writeToFile(String fileId, final String fileContent) {
        final File file = buildFile(fileId);
        if (!file.exists()) {
            TaskScheduler.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        FileWriter writer = new FileWriter(file);
                        writer.write(fileContent);
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     *
     * @param fileId The fileId to write to Disk.
     */
    public static void writeToFile(String fileId, final Object fileObj) {
        final File file = buildFile(fileId);
        if (!file.exists()) {
            TaskScheduler.execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        String content = GsonHelper.toJson(fileObj);
                        FileWriter writer = new FileWriter(file);
                        writer.write(content);
                        writer.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            });
        }
    }

    /**
     * 同步读取一个文件，will be block
     */
    public static String readFileContent(String fileId) {
        final File file = buildFile(fileId);
        StringBuilder fileContentBuilder = new StringBuilder();
        if (file.exists()) {
            String stringLine;
            try {
                FileReader fileReader = new FileReader(file);
                BufferedReader bufferedReader = new BufferedReader(fileReader);
                while ((stringLine = bufferedReader.readLine()) != null) {
                    fileContentBuilder.append(stringLine).append("\n");
                }
                bufferedReader.close();
                fileReader.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return fileContentBuilder.toString();
    }

    /**
     * 同步读取一个文件，根据类型返回
     */
    public static <T> T readFileContent(String fileId ,Class<T> ype) {
        String fileContent = readFileContent(fileId);
        return GsonHelper.fromJson(fileContent,ype);
    }

    /**
     * 异步读取一个文件，根据类型返回结果，内容可能为空
     */
    public static <T> void readFileContent(final String fileId,final TaskCallback.Callback<T> fileCallBack) {

        TaskScheduler.execute(new Runnable() {
            @Override
            public void run() {
                String fileContent = readFileContent(fileId);

                T result = GsonHelper.fromJson(fileContent,fileCallBack.rType);
                if(result!=null) {
                    TaskScheduler.notifySuccessToUI(result, fileCallBack);
                }else {
                    TaskScheduler.notifyErrorToUI(new ErrorBundle("文件不存在"), fileCallBack);
                }
            }
        });
    }


    public static boolean isCached(String fileId) {
        final File entityFile = buildFile(fileId);
        return exists(entityFile);
    }

    public static boolean exists(File file) {
        return file.exists();
    }

    /**
     * Warning: Deletes the content of a directory.
     * This is an I/O operation and this method executes in the main thread, so it is recommended to
     * perform the operation using another thread.
     *
     * @param directory The directory which its content will be deleted.
     */
    void clearDirectory(final File directory) {
        TaskScheduler.execute(new Runnable() {
            @Override
            public void run() {
                boolean result = false;
                if (directory.exists()) {
                    for (File file : directory.listFiles()) {
                        result = file.delete();
                    }
                }
            }
        });

    }

    public static class ParameterizedTypeImpl implements ParameterizedType {
        private final Class raw;
        private final Type[] args;
        public ParameterizedTypeImpl(Class raw, Type[] args) {
            this.raw = raw;
            this.args = args != null ? args : new Type[0];
        }
        @Override
        public Type[] getActualTypeArguments() {
            return args;
        }
        @Override
        public Type getRawType() {
            return raw;
        }
        @Override
        public Type getOwnerType() {return null;}
    }
}
