/*
 * Copyright 2015 The Friedland Group, Inc.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 */
package com.imagesleuth.imagesleuthclient2;

import akka.actor.ActorRef;
import akka.actor.UntypedActor;
import com.thefriedlandgroup.XMLTools2.Test;
import java.util.Date;

/**
 *
 * @author Noah Friedland at The Friedland Group, Inc
 */
public class ProcessImage extends UntypedActor {
    public static final int sleeptime = 300;
    public static final int niter = 100;

    private final Poster poster;
    private final Getter getter;

    public ProcessImage(String url, String key, String token) {
        Test.testNull(url);
        Test.testNull(key);
        Test.testNull(token);
        System.out.println("initializing ProcessImage for url: " + url);
        poster = new Poster(url, key, token);
        getter = new Getter(url, key, token);
    }

    @Override
    public void onReceive(Object message) {
        if (message instanceof PIMessages.Start) {
            PIMessages.Start msg = (PIMessages.Start) message;
            final ActorRef replyTo = getSender();

            // posting the image
            try {
                long start = new Date().getTime();
                String id = poster.Post(msg.img);
                int count = 0;
                if (id != null) {
                    while (!msg.result.exists() && count++ < niter) {
                        getter.get(id, msg.result);
                        Thread.sleep(sleeptime);
                    }
                }
                long time = new Date().getTime() - start;
                if (count > 0 && count < niter) {
                    replyTo.tell(
                            new PIMessages.Finished(PIMessages.Finished.SUCCESS,
                            "", time), getSelf());
                } else {
                    replyTo.tell(
                            new PIMessages.Finished(PIMessages.Finished.FAILURE,
                            "", 0), getSelf());                    
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                    replyTo.tell(
                            new PIMessages.Finished(PIMessages.Finished.FAILURE,
                            ex.toString(), 0), getSelf());
            }

        } else {
            unhandled(message);
        }
    }

}
