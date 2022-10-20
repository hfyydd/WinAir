package gst;

import com.formdev.flatlaf.FlatDarkLaf;
import com.github.serezhka.jap2lib.rtsp.AudioStreamInfo;
import com.github.serezhka.jap2lib.rtsp.VideoStreamInfo;
import com.github.serezhka.jap2server.AirplayDataConsumer;
import org.freedesktop.gstreamer.*;
import org.freedesktop.gstreamer.elements.AppSink;
import org.freedesktop.gstreamer.elements.AppSrc;
import org.freedesktop.gstreamer.swing.GstVideoComponent;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;


public class GstPlayerJ implements AirplayDataConsumer {

    private static Pipeline pipeline;
    private final Pipeline h264Pipeline = null;
    private Pipeline alacPipeline;
    private Pipeline aacEldPipeline;

    private AppSrc h264Src;
    private AppSrc alacSrc;
    private AppSrc aacEldSrc;

    private AppSink appSink;

    private AudioStreamInfo.CompressionType audioCompressionType;

    private Bin bin;

    //public GstVideoComponent vc;
    public MyNewVideoComponent vc;
    public JFrame f;

    public boolean isVisible = false;

    public GstPlayerJ() {

        Utils.configurePaths();
        Gst.init(Version.BASELINE, "GstPlayer", "");
        FlatDarkLaf.setup();
//        h264Pipeline = (Pipeline) Gst.parseLaunch("appsrc name=h264-src ! h264parse ! avdec_h264 ! videoconvert ! autovideosink sync=false");
//
//        h264Src = (AppSrc) h264Pipeline.getElementByName("h264-src");
//        h264Src.setStreamType(AppSrc.StreamType.STREAM);
//        h264Src.setCaps(Caps.fromString("video/x-h264,colorimetry=bt709,stream-format=(string)byte-stream,alignment=(string)au"));
//        h264Src.set("is-live", true);
//        h264Src.set("format", Format.TIME);
//
//        h264Pipeline.play();
        //EventQueue.invokeLater(() -> {
        //SwingUtilities.invokeLater(new Runnable() {

        //    public void run() {
            pipeline = new Pipeline();

            //bin = (Bin) Gst.parseLaunch("appsrc name=h264-src ! h264parse ! avdec_h264 ! videoconvert ! videoscale ! appsink name=sink");
            bin = (Bin) Gst.parseLaunch("appsrc name=h264-src ! h264parse ! avdec_h264 ! videoconvert ! videoscale ! appsink name=sink");
            h264Src = (AppSrc) bin.getElementByName("h264-src");
            h264Src.setStreamType(AppSrc.StreamType.STREAM);
            h264Src.setCaps(Caps.fromString("video/x-h264,colorimetry=bt709,stream-format=(string)byte-stream,alignment=(string)au"));
            h264Src.set("is-live", true);
            h264Src.set("format", Format.TIME);


            appSink = (AppSink) bin.getElementByName("sink");
            vc = new MyNewVideoComponent(appSink);
            vc.setPreferredSize(new Dimension(300, 650));
            pipeline.addMany(bin,vc.getElement());
            Element.linkMany(bin,vc.getElement());

            //////////////////////////////////////////////////
//            appSink = (AppSink) bin.getElementByName("sink");
//            GstVideoComponent vc = new GstVideoComponent(appSink);
//
//            pipeline.addMany(bin,vc.getElement());
//            Element.linkMany(bin,vc.getElement());
        /////////////////
            f = new JFrame("iOS");
            f.add(vc);
            f.pack();
            f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
//            pipeline.play();
//



            alacPipeline = (Pipeline) Gst.parseLaunch("appsrc name=alac-src ! avdec_alac ! audioconvert !audioresample ! autoaudiosink sync=false"); // +

            alacSrc = (AppSrc) alacPipeline.getElementByName("alac-src");
            alacSrc.setStreamType(AppSrc.StreamType.STREAM);
            alacSrc.setCaps(Caps.fromString("audio/x-alac,mpegversion=(int)4,channels=(int)2,rate=(int)44100,stream-format=raw,codec_data=(buffer)00000024616c616300000000000001600010280a0e0200ff00000000000000000000ac44"));
            alacSrc.set("is-live", true);
            alacSrc.set("format", Format.TIME);

            alacPipeline.play();


            aacEldPipeline = (Pipeline) Gst.parseLaunch("appsrc name=aac-eld-src ! avdec_aac ! audioconvert ! audioresample ! autoaudiosink sync=false"); // +

            aacEldSrc = (AppSrc) aacEldPipeline.getElementByName("aac-eld-src");
            aacEldSrc.setStreamType(AppSrc.StreamType.STREAM);
            aacEldSrc.setCaps(Caps.fromString("audio/mpeg,mpegversion=(int)4,channnels=(int)2,rate=(int)44100,stream-format=raw,codec_data=(buffer)f8e85000"));
            aacEldSrc.set("is-live", true);
            aacEldSrc.set("format", Format.TIME);

            aacEldPipeline.play();

        //    }
        //});

    }

    @Override
    public void onVideo(byte[] bytes) {
        f.setVisible(true);
        Buffer buf = new Buffer(bytes.length);
        buf.map(true).put(bytes);
        // buf.setFlags(EnumSet.of(BufferFlags.LIVE));
        h264Src.pushBuffer(buf);
    }

    @Override
    public void onAudio(byte[] bytes) {
        Buffer buf = new Buffer(bytes.length);
        buf.map(true).put(bytes);
        // buf.setFlags(EnumSet.of(BufferFlags.LIVE));
        switch (audioCompressionType) {
            case ALAC:
                alacSrc.pushBuffer(buf);
                break;
            case AAC_ELD:
                aacEldSrc.pushBuffer(buf);
                break;
            default:
                break;
        }
    }

    @Override
    public void onVideoFormat(VideoStreamInfo videoStreamInfo) {
        //log.info("onVideoFormat");
    }

    @Override
    public void onAudioFormat(AudioStreamInfo audioStreamInfo) {
        //log.info("onAudioFormat: {}", audioStreamInfo.getAudioFormat());
        this.audioCompressionType = audioStreamInfo.getCompressionType();
    }

    public MyNewVideoComponent getVc(){
        return vc;
    }
    public void start(){
        //
        pipeline.play();
    }


}
