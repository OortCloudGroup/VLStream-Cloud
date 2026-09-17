package com.ruoyi.vlstream.test.vlstream.service;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
@Tag("dev")
class TrainingOptionsTest {
    @Test void defaultsAreExplicitAndAutoPublicationOptIn() {
        TrainingOptions options=new TrainingOptions(20,128,960,"{\"mode\":\"auto\",\"autoPublish\":true}");
        assertEquals(20,options.getEpochs()); assertEquals(16,options.getBatchSize()); assertEquals(640,options.getImgSize()); assertTrue(options.isAutoPublish());
        assertFalse(new TrainingOptions(null,null,null,null).isAutoPublish());
    }
    @Test void customValuesSurviveSerialization() {
        TrainingOptions options=new TrainingOptions(43,8,960,"{\"mode\":\"advanced\",\"autoPublish\":false}");
        assertEquals(43,options.getEpochs()); assertEquals(8,options.getBatchSize()); assertEquals(960,options.getImgSize());
        assertTrue(options.toJson().contains("\"imgSize\":960"));
    }
    @Test void rejectsUnsupportedOrUnsafeOptionsBeforeQueueing() {
        assertThrows(RuntimeException.class,()->new TrainingOptions(0,16,640,null));
        assertThrows(RuntimeException.class,()->new TrainingOptions(20,0,640,null));
        assertThrows(RuntimeException.class,()->new TrainingOptions(20,16,641,null));
        assertThrows(RuntimeException.class,()->new TrainingOptions(20,16,2048,null));
        for(String extra:new String[]{"[]","bad","{\"mode\":\"tune\"}","{\"shell\":\"touch /tmp/a\"}","{\"autoPublish\":\"yes\"}"}) assertThrows(RuntimeException.class,()->new TrainingOptions(20,16,640,extra));
    }
}
