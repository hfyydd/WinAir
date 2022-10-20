package gst

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.formdev.flatlaf.FlatDarkLaf
import com.github.serezhka.jap2lib.rtsp.AudioStreamInfo
import com.github.serezhka.jap2lib.rtsp.VideoStreamInfo
import com.github.serezhka.jap2server.AirplayDataConsumer
import org.freedesktop.gstreamer.*
import org.freedesktop.gstreamer.elements.AppSink
import org.freedesktop.gstreamer.elements.AppSrc
import java.awt.Dimension
import javax.swing.JFrame


class GstPlayerK : AirplayDataConsumer {
    private val h264Pipeline: Pipeline? = null
    private val alacPipeline: Pipeline
    private val aacEldPipeline: Pipeline
    private val h264Src: AppSrc
    private val alacSrc: AppSrc
    private val aacEldSrc: AppSrc
    private val appSink: AppSink
    private var audioCompressionType: AudioStreamInfo.CompressionType? = null
    private val bin: Bin

    //public GstVideoComponent vc;
    var vc: MyNewVideoComponent
    var f: JFrame
    var isVisible:Boolean by mutableStateOf(false)
    private var pipeline: Pipeline

    init {
        Utils.configurePaths()
        Gst.init(Version.BASELINE, "GstPlayer", "")
        FlatDarkLaf.setup()
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
        pipeline = Pipeline()

        //bin = (Bin) Gst.parseLaunch("appsrc name=h264-src ! h264parse ! avdec_h264 ! videoconvert ! videoscale ! appsink name=sink");
        bin =
            Gst.parseLaunch("appsrc name=h264-src ! h264parse ! avdec_h264 ! videoconvert ! videoscale ! appsink name=sink") as Bin
        h264Src = bin.getElementByName("h264-src") as AppSrc
        h264Src.streamType = AppSrc.StreamType.STREAM
        h264Src.caps =
            Caps.fromString("video/x-h264,colorimetry=bt709,stream-format=(string)byte-stream,alignment=(string)au")
        h264Src["is-live"] = true
        h264Src["format"] = Format.TIME
        appSink = bin.getElementByName("sink") as AppSink
        vc = MyNewVideoComponent(appSink)
        vc.preferredSize = Dimension(300, 650)
        pipeline.addMany(bin, vc.element)
        Element.linkMany(bin, vc.element)

        //////////////////////////////////////////////////
//            appSink = (AppSink) bin.getElementByName("sink");
//            GstVideoComponent vc = new GstVideoComponent(appSink);
//
//            pipeline.addMany(bin,vc.getElement());
//            Element.linkMany(bin,vc.getElement());
        /////////////////
        f = JFrame("iOS")
        f.add(vc)
        f.pack()
        f.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        //            pipeline.play();
//
        alacPipeline =
            Gst.parseLaunch("appsrc name=alac-src ! avdec_alac ! audioconvert !audioresample ! autoaudiosink sync=false") as Pipeline // +
        alacSrc = alacPipeline.getElementByName("alac-src") as AppSrc
        alacSrc.streamType = AppSrc.StreamType.STREAM
        alacSrc.caps =
            Caps.fromString("audio/x-alac,mpegversion=(int)4,channels=(int)2,rate=(int)44100,stream-format=raw,codec_data=(buffer)00000024616c616300000000000001600010280a0e0200ff00000000000000000000ac44")
        alacSrc["is-live"] = true
        alacSrc["format"] = Format.TIME
        alacPipeline.play()
        aacEldPipeline =
            Gst.parseLaunch("appsrc name=aac-eld-src ! avdec_aac ! audioconvert ! audioresample ! autoaudiosink sync=false") as Pipeline // +
        aacEldSrc = aacEldPipeline.getElementByName("aac-eld-src") as AppSrc
        aacEldSrc.streamType = AppSrc.StreamType.STREAM
        aacEldSrc.caps =
            Caps.fromString("audio/mpeg,mpegversion=(int)4,channnels=(int)2,rate=(int)44100,stream-format=raw,codec_data=(buffer)f8e85000")
        aacEldSrc["is-live"] = true
        aacEldSrc["format"] = Format.TIME
        aacEldPipeline.play()

        //    }
        //});
    }

    override fun onVideo(bytes: ByteArray) {
        f.isVisible = true
        isVisible = true
        val buf = Buffer(bytes.size)
        buf.map(true).put(bytes)
        // buf.setFlags(EnumSet.of(BufferFlags.LIVE));
        h264Src.pushBuffer(buf)
    }

    override fun onAudio(bytes: ByteArray) {
        val buf = Buffer(bytes.size)
        buf.map(true).put(bytes)
        when (audioCompressionType) {
            AudioStreamInfo.CompressionType.ALAC -> alacSrc.pushBuffer(buf)
            AudioStreamInfo.CompressionType.AAC_ELD -> aacEldSrc.pushBuffer(buf)
            else -> {}
        }
    }

    override fun onVideoFormat(videoStreamInfo: VideoStreamInfo) {
        //log.info("onVideoFormat");
    }

    override fun onAudioFormat(audioStreamInfo: AudioStreamInfo) {
        //log.info("onAudioFormat: {}", audioStreamInfo.getAudioFormat());
        audioCompressionType = audioStreamInfo.compressionType
    }

    fun start() {
        //
        pipeline.play()
    }

}