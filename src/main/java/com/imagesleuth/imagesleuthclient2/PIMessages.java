/*
 * Copyright 2015 The Friedland Group, Inc.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package com.imagesleuth.imagesleuthclient2;

import com.thefriedlandgroup.XMLTools2.Test;
import java.io.File;
import java.io.Serializable;

/**
 *
 * @author Noah Friedland at The Friedland Group, Inc
 */
public interface PIMessages {
    
    public class Launch implements Serializable {
        public final String url;
        public final String user;
        public final String password;
        public final File imgDir;
        public final int nworkers;
        
        public Launch(String url, String user, String password, File imgDir, int nworkers) {
            Test.testNull(url);
            Test.testNull(user);
            Test.testNull(password);
            Test.testNull(imgDir);
            if (nworkers <= 0) {
                throw new IllegalArgumentException("invalid nworkers:" + nworkers);
            }
            this.url = url;
            this.user = user;
            this.password = password;
            this.imgDir = imgDir;
            this.nworkers = nworkers;
        }
    }
    

    public class Start implements Serializable {

        public final File img;
        public final File result;

        public Start(File img, File result) {
            Test.testNull(img);
            Test.testNull(result);
            this.img = img;
            this.result = result;
        }

    }

    public class Finished implements Serializable {
        public static final int SUCCESS = 0;
        public static final int FAILURE = 1;
        
        public final int mode;
        public final String msg;
        public final long time;

        public Finished(int mode, String msg, long time) {
            Test.testNull(msg);
            this.mode = mode;
            this.msg = msg;
            this.time = time;
        }
    }

}
