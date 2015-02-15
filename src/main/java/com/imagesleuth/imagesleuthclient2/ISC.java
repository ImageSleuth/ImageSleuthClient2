/*
 * Copyright 2014 The Friedland Group, Inc.
 * -----------------------------------------------------------------
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package com.imagesleuth.imagesleuthclient2;

import akka.actor.ActorRef;
import akka.actor.ActorSystem;
import akka.actor.PoisonPill;
import akka.actor.Props;
import akka.actor.UntypedActor;
import akka.event.Logging;
import akka.event.LoggingAdapter;
import akka.routing.RoundRobinRouter;
import com.thefriedlandgroup.XMLTools2.ImageFileFilter;
import com.thefriedlandgroup.XMLTools2.Test;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;

/**
 *
 * @author Noah Friedland at The Friedland Group, Inc
 */
public class ISC extends UntypedActor {

    LoggingAdapter logger = Logging.getLogger(getContext().system(), this);

    public String url;
    public String user;
    public String password;
    public int imgcount;
    public int successCount = 0;
    public int failureCount = 0;
    public long timesum = 0;
    public long start;
    static final ActorSystem system = ActorSystem.create("ISCSystem");
    ActorRef workerRouter;

    @Override
    public void onReceive(Object message) {
        if (message instanceof PIMessages.Launch) {
            PIMessages.Launch lmsg = (PIMessages.Launch) message;

            this.url = lmsg.url;
            if (url.endsWith("/")) {
                // removes trailing "/"
                url = url.substring(0, url.length() - 1);
            }
            this.user = lmsg.user;
            this.password = lmsg.password;
            ArrayList<File> images = getImages(lmsg.imgDir);

            this.imgcount = images.size();
            if (imgcount == 0) {
                System.out.println("No unprocessed images detected, terminating");
                System.exit(0);                
            }
            int nworkers = (imgcount < lmsg.nworkers) ? imgcount : lmsg.nworkers;
            workerRouter = getContext().actorOf(Props.create(ProcessImage.class, url, user, password)
                    .withRouter(new RoundRobinRouter(nworkers)));

            start = new Date().getTime();
            for (File img : images) {
                workerRouter.tell(new PIMessages.Start(img, getJsonFile(img)), getSelf());
            }
        } else if (message instanceof PIMessages.Finished) {
            PIMessages.Finished fmsg = (PIMessages.Finished) message;
            switch (fmsg.mode) {
                case PIMessages.Finished.SUCCESS:
                    successCount++;
                    timesum += fmsg.time;
                    break;
                case PIMessages.Finished.FAILURE:
                    failureCount++;
                    break;
            }
            int processed = successCount + failureCount;
            if (processed % 100 == 0) {
                long totaltime = new Date().getTime() - start;
                System.out.println("ISC: processed: " + processed + " success: " + successCount + " totaltime: " + totaltime);
            }
            if (processed == imgcount) {
                // shut down the workers
                workerRouter.tell(PoisonPill.getInstance(), ActorRef.noSender());
                System.out.println("ISC finished: success: " + successCount + " failure: " + failureCount);
                double avgtime = (double) timesum / (double) successCount;
                System.out.println("Avg image processing time: " + avgtime);
                long totaltime = new Date().getTime() - start;
                System.out.println("Total processing time: " + totaltime);
                System.exit(0);
            }
        } else {
            unhandled(message);
        }
    }

    public static void main(final String[] args) throws Exception {
        if (args.length != 4 && args.length != 5) {
            System.out.println("usage: <url> <user> <pw> <image dir> [nworkers]");
            System.exit(1);
        }
        final String url = args[0];        
        String user = args[1];
        String pw = args[2];
        File imgDir = new File(args[3]);
        int nworkers = (args.length == 5) ? Integer.parseInt(args[4])
                : (int)Math.round(.6*Runtime.getRuntime().availableProcessors());
        nworkers = (nworkers == 0) ? 1 : nworkers;
        System.out.println("HYDRAClient: URL: " + url + " nworkers: " + nworkers);
        ActorRef me = system.actorOf(Props.create(ISC.class));
        me.tell(new PIMessages.Launch(url, user, pw, imgDir, nworkers), me);
    }

    public static File getJsonFile(File file) {
        return new File(file.getParent() + File.separator + getBaseName(file) + ".json");
    }

    public static String getBaseName(File file) {
        if (file == null) {
            throw new IllegalArgumentException("null file");
        }
        int pos = file.getName().lastIndexOf(".");
        if (pos != -1) {
            return file.getName().substring(0, pos);
        }
        return file.getName();
    }

    public static ArrayList<File> getImages(File imgDir) {
        Test.testDir(imgDir);
        ArrayList<File> images = new ArrayList<>();
        File[] imgs = imgDir.listFiles(new ImageFileFilter());
        for (File img : imgs) {
            File jsonFile = getJsonFile(img);
            if (!jsonFile.exists()) {
                images.add(img);
            }
        }
        return images;
    }

}
