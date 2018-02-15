package org.boon.bugs;

import org.boon.examples.model.test.movies.crud.BatchResults;
import org.boon.examples.model.test.movies.crud.RecordReadResponse;
import org.boon.examples.model.test.movies.likeable.Like;
import org.boon.examples.model.test.movies.likeable.LikeabilityUpdate;
import org.boon.examples.model.test.movies.likeable.LikesBulkUpdate;
import org.boon.examples.model.test.movies.likeable.LikingService;
import org.boon.examples.model.test.movies.wathcer.Watcher;
import org.boon.Lists;
import org.boon.Maps;
import org.boon.core.reflection.ClassMeta;
import org.boon.core.reflection.Invoker;
import org.boon.core.reflection.MethodAccess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import java.io.Serializable;
import java.util.Map;
import static org.boon.Boon.fromJson;
import static org.boon.Boon.puts;
import static org.boon.Boon.toJson;
import static org.boon.Exceptions.die;

/**
 * Created by Richard on 8/25/14.
 */
public class Bug211 {

    boolean scoreCalled;

    boolean ok;

    @BeforeEach
    public void setup() {
        scoreCalled = false;
    }

    @Test
    public void test() {
        LikingService likingService = new LikingService() {

            @Override
            public BatchResults updatePreferences(LikesBulkUpdate request) {
                return null;
            }

            @Override
            public void score(LikeabilityUpdate scoreUpdate) {
                puts(scoreUpdate);
                scoreCalled = true;
            }

            @Override
            public RecordReadResponse<Watcher> loadUser(String username, long version) {
                return null;
            }
        };
        Invoker.invokeFromObject(likingService, "score", Lists.list(Lists.list("bob", "123", "LIKE", 10)));
        ok = scoreCalled || die("Score was not called");
    }

    @Test
    public void testWithSingleList() {
        LikingService likingService = new LikingService() {

            @Override
            public BatchResults updatePreferences(LikesBulkUpdate request) {
                return null;
            }

            @Override
            public void score(LikeabilityUpdate scoreUpdate) {
                puts(scoreUpdate);
                scoreCalled = true;
            }

            @Override
            public RecordReadResponse<Watcher> loadUser(String username, long version) {
                return null;
            }
        };
        Invoker.invokeFromObject(likingService, "score", Lists.list("bob", "123", "LIKE", 10));
        ok = scoreCalled || die("Score was not called");
    }

    @Test
    public void testWithSingleMap() {
        LikingService likingService = new LikingService() {

            @Override
            public BatchResults updatePreferences(LikesBulkUpdate request) {
                return null;
            }

            @Override
            public void score(LikeabilityUpdate scoreUpdate) {
                puts(scoreUpdate);
                scoreCalled = true;
            }

            @Override
            public RecordReadResponse<Watcher> loadUser(String username, long version) {
                return null;
            }
        };
        //        private String username;
        //        private String video;
        //        private Like like;
        //        private int spot;
        Invoker.invokeFromObject(likingService, "score", Maps.map("username", "bob", "video", "123", "like", "LIKE", "spot", 10));
        ok = scoreCalled || die("Score was not called");
    }

    @Test
    public void testWithSingleMap2() {
        LikingService likingService = new LikingService() {

            @Override
            public BatchResults updatePreferences(LikesBulkUpdate request) {
                return null;
            }

            @Override
            public void score(LikeabilityUpdate scoreUpdate) {
                puts(scoreUpdate);
                scoreCalled = true;
            }

            @Override
            public RecordReadResponse<Watcher> loadUser(String username, long version) {
                return null;
            }
        };
        //        private String username;
        //        private String video;
        //        private Like like;
        //        private int spot;
        Invoker.invokeFromObject(likingService, "score", Maps.map("username", "bob", "video", "123", "like", "LIKE", "spot", 0));
        ok = scoreCalled || die("Score was not called");
    }

    @Test
    public void listofMaps() {
        LikingService likingService = new LikingService() {

            @Override
            public BatchResults updatePreferences(LikesBulkUpdate request) {
                return null;
            }

            @Override
            public void score(LikeabilityUpdate scoreUpdate) {
                puts(scoreUpdate);
                scoreCalled = true;
            }

            @Override
            public RecordReadResponse<Watcher> loadUser(String username, long version) {
                return null;
            }
        };
        //        private String username;
        //        private String video;
        //        private Like like;
        //        private int spot;
        Invoker.invokeFromObject(likingService, "score", Lists.list(Maps.map("username", "bob", "video", "123", "like", "LIKE", "spot", 0)));
        ok = scoreCalled || die("Score was not called");
    }

    @Test
    public void testWithSingleMapWithMethod() {
        LikingService likingService = new LikingService() {

            @Override
            public BatchResults updatePreferences(LikesBulkUpdate request) {
                return null;
            }

            @Override
            public void score(LikeabilityUpdate scoreUpdate) {
                puts(scoreUpdate);
                scoreCalled = true;
            }

            @Override
            public RecordReadResponse<Watcher> loadUser(String username, long version) {
                return null;
            }
        };
        final MethodAccess score = ClassMeta.classMeta(LikingService.class).method("score");
        //        private String username;
        //        private String video;
        //        private Like like;
        //        private int spot;
        Invoker.invokeMethodFromObjectArg(likingService, score, Maps.map("username", "bob", "video", "123", "like", "LIKE", "spot", 0));
        ok = scoreCalled || die("Score was not called");
    }

    @Test
    public void testWithSingleMapWithMethodJson() {
        LikingService likingService = new LikingService() {

            @Override
            public BatchResults updatePreferences(LikesBulkUpdate request) {
                return null;
            }

            @Override
            public void score(LikeabilityUpdate scoreUpdate) {
                puts(scoreUpdate);
                scoreCalled = true;
            }

            @Override
            public RecordReadResponse<Watcher> loadUser(String username, long version) {
                return null;
            }
        };
        final MethodAccess score = ClassMeta.classMeta(LikingService.class).method("score");
        //        private String username;
        //        private String video;
        //        private Like like;
        //        private int spot;
        final Map<String, ? extends Serializable> map = Maps.map("username", "bob", "video", "123", "like", "LIKE", "spot", 0);
        String json = toJson(map);
        final Object o = fromJson(json);
        Invoker.invokeMethodFromObjectArg(likingService, score, o);
        ok = scoreCalled || die("Score was not called");
    }
}
