/*
 *
 * # Copyright 2016 Intellisis Inc.  All rights reserved.
 * #
 * # Use of this source code is governed by a BSD-style
 * # license that can be found in the LICENSE file
 */

package com.knurld.alphabank.com.knurld.alphabank.request.util;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v1.DbxClientV1;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.UploadErrorException;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

/**
 * Created by ndarade on 5/5/16.
 */
public class DropBoxUtil {
    private static DropBoxUtil dBInstance = new DropBoxUtil();


    DbxRequestConfig config = new DbxRequestConfig("dropbox/java-tutorial", "en_US");

    public DbxClientV2 getDbxClientV2() {
        return dbxClientV2;
    }

    public void setDbxClientV2(DbxClientV2 dbxClientV2) {
        this.dbxClientV2 = dbxClientV2;
    }

    public DbxClientV1 getDbxClientV1() {
        return dbxClientV1;
    }

    public void setDbxClientV1(DbxClientV1 dbxClientV1) {
        this.dbxClientV1 = dbxClientV1;
    }

    private DbxClientV2 dbxClientV2;
    private DbxClientV1 dbxClientV1;

    private DropBoxUtil() {
        dbxClientV2 = new DbxClientV2(config, KnurldRequestHelper.dropbox_access_token);
        dbxClientV1 = new DbxClientV1(config, KnurldRequestHelper.dropbox_access_token);
    }

    public static DropBoxUtil getInstance() {
        return dBInstance;
    }

    public static String uploadFile(InputStream inputStream) {
        DropBoxUtil instance = DropBoxUtil.getInstance();
        String s = null;
        try {
            String fileName = UUID.randomUUID().toString() + ".wav";
            instance.getDbxClientV2().files().uploadBuilder("/" + fileName)
                    .uploadAndFinish(inputStream);
            s = instance.getDbxClientV1().createShareableUrl("/" + fileName);
        } catch (UploadErrorException e) {
            e.printStackTrace();
        } catch (DbxException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return s;
    }
}
