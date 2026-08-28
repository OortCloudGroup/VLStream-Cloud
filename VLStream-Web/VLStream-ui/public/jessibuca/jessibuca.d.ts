/*
 * SPDX-FileCopyrightText: 2026 OortCloud (https://vls.oortcloudsmart.com/en/)
 * SPDX-License-Identifier: MIT
 * Created by: ChaoQun Lei
 * Updated by: ChaoQun Lei
 */

declare namespace Jessibuca {

    /* * info */
    enum TIMEOUT {
        /* * play() , if data */
        loadingTimeout = 'loadingTimeout',
        /* * in , if timeout after data */
        delayTimeout = 'delayTimeout',
    }

    /* * info */
    enum ERROR {
        /* * , url is empty , play method */
        playError = 'playError',
        /* * http failed */
        fetchError = 'fetchError',
        /* * websocket failed */
        websocketError = 'websocketError',
        /* * webcodecs h265 failed */
        webcodecsH265NotSupport = 'webcodecsH265NotSupport',
        /* * mediaSource h265 failed */
        mediaSourceH265NotSupport = 'mediaSourceH265NotSupport',
        /* * wasm failed */
        wasmDecodeError = 'wasmDecodeError',
    }

    interface Config {
        /**
         * 
         * * to string , layer is document.getElementById('id')
         * */
        container: HTMLElement | string;
        /**
         * Set , , will
         */
        videoBuffer?: number;
        /**
         * worker
         * * is decoder.js , decoder.js and decoder.wasm is in . */
        decoder?: string;
        /**
         * whether ( can )
         */
        forceNoOffscreen?: boolean;
        /**
         * whether page 'visibilityState' to 'hidden' , .
         */
        hiddenAutoPause?: boolean;
        /**
         * whether , if Set `false`, data , can .
         */
        hasAudio?: boolean;
        /**
         * Set , only , 0( ), 180, 270 value
         */
        rotate?: boolean;
        /**
         * 1. to `true` : etc. after, canvas , , . etc. `setScaleMode(1)`
         * 2. to `false` : full fill canvas , will . etc. `setScaleMode(0)`
         */
        isResize?: boolean;
        /**
         * 1. to `true` : etc. after, full fill canvas , , , full . etc. `setScaleMode(2)`
         */
        isFullResize?: boolean;
        /**
         * 1. to `true` : ws whether .flv to , Parse .
         */
        isFlv?: boolean;
        /**
         * whether control
         */
        debug?: boolean;
        /**
         * 1. Set ,
         * 2. in successfully before(loading) and in (heart),if data , timeoutevent
         */
        timeout?: number;
        /**
         * 1. Set ,
         * 2. in successfully before,if data , timeoutevent
         */
        heartTimeout?: number;
        /**
         * 1. Set ,
         * 2. in successfully before,if data , timeoutevent
         */
        loadingTimeout?: number;
        /**
         * whether event, full , full event
         */
        supportDblclickFullscreen?: boolean;
        /**
         * whether
         */
        showBandwidth?: boolean;
        /**
         * configurationoperationbutton
         */
        operateBtns?: {
            /* * whether full button */
            fullscreen?: boolean;
            /* * whether snapshotbutton */
            screenshot?: boolean;
            /* * whether button */
            play?: boolean;
            /* * whether button */
            audio?: boolean;
            /* * whether */
            record?: boolean;
        };
        /**
         * , in , canvas will video
         */
        keepScreenOn?: boolean;
        /**
         * whether , is
         */
        isNotMute?: boolean;
        /**
         * Load in
         */
        loadingText?: string;
        /**
         * 
         */
        background?: string;
        /**
         * whether MediaSource
         * * only H.264 (Safari on iOS )
         * * forceNoOffscreen to false ( )
         */
        useMSE?: boolean;
        /**
         * whether Webcodecs
         * * only H.264 ( in chrome 94 , need to https localhost )
         * * forceNoOffscreen to false ( )
         * */
        useWCS?: boolean;
        /**
         * whether
         * before : esc -> exit full ; arrowUp -> ; arrowDown -> ;
         */
        hotKey?: boolean;
        /**
         * in MSE Webcodecs H265 , whether wasm .
         * Set to false , Error , Set to true will wasm .
         */
        autoWasm?: boolean;
        /**
         * heartTimeout after , , new .
         */
        heartTimeoutReplay?: boolean,
        /**
         * heartTimeoutReplay from , after,
         */
        heartTimeoutReplayTimes?: number,
        /**
         * loadingTimeout loading after , , new .
         */
        loadingTimeoutReplay?: boolean,
        /**
         * heartTimeoutReplay from , after,
         */
        loadingTimeoutReplayTimes?: number
        /**
         * wasm after, , is new .
         */
        wasmDecodeErrorReplay?: boolean,
        /**
         * https://github.com/langhuihui/jessibuca/issues/152
         * : WebGL Process 4 data, is 540x960 U、V is 540/2=270 can 4 , .
         */
        openWebglAlignment?: boolean
    }
}


declare class Jessibuca {

    constructor(config?: Jessibuca.Config);

    /**
     * whether control
     @example
     // 
     jessibuca.setDebug(true)
     // 
     jessibuca.setDebug(false)
     */
    setDebug(flag: boolean): void;

    /**
     * 
     @example
     jessibuca.mute()
     */
    mute(): void;

    /**
     * 
     @example
     jessibuca.cancelMute()
     */
    cancelMute(): void;

    /**
     * layer useroperation method .
     *
     * iPhone, chrome etc. need to , , need to user operation , can .
     *
     * https://developers.google.com/web/updates/2017/09/autoplay-policy-changes
     */
    audioResume(): void;

    /**
     *
     * Set ,
     * in successfully before and in ,if data , timeoutevent

     @example
     jessibuca.setTimeout(10)

     jessibuca.on('timeout',function(){
        //
    });
     */
    setTimeout(): void;

    /**
     * @param mode
     * 0 full fill canvas , will etc. parameter `isResize` to false
     *
     * 1 etc. after, canvas , , etc. parameter `isResize` to true
     *
     * 2 etc. after, full fill canvas , , , full etc. parameter `isFullResize` to true
     @example
     jessibuca.setScaleMode(0)

     jessibuca.setScaleMode(1)

     jessibuca.setScaleMode(2)
     */
    setScaleMode(mode: number): void;

    /**
     * 
     *
     * in pause after, `play()` method then before .
     @example
     jessibuca.pause().then(()=>{
        console.log('pause success')

        jessibuca.play().then(()=>{

        }).catch((e)=>{

        })

    }).catch((e)=>{
        console.log('pause error',e);
    })
     */
    pause(): Promise<void>;

    /**
     * , layer
     @example
     jessibuca.close();
     */
    close(): void;

    /**
     * , layer
     @example
     jessibuca.destroy()
     */
    destroy(): void;

    /**
     * to
     @example
     jessibuca.clearView()
     */
    clearView(): void;

    /**
     * 
     @example

     jessibuca.play('url').then(()=>{
        console.log('play success')
    }).catch((e)=>{
        console.log('play error',e)
    })
     //
     jessibuca.play()
     */
    play(url?: string): Promise<void>;

    /**
     * new
     */
    resize(): void;

    /**
     * Set , , will .
     *
     * etc. `videoBuffer` parameter.
     *
     @example
     // Set 200ms
     jessibuca.setBufferTime(0.2)
     */
    setBufferTime(time: number): void;

    /**
     * Set , only , 0( ) , 180, 270 value .
     *
     * > and full , iOS full API, method page full . *
     @example
     jessibuca.setRotate(0)

     jessibuca.setRotate(90)

     jessibuca.setRotate(270)
     */
    setRotate(deg: number): void;

    /**
     *
     * Set , value 0 — 1
     *
     * > mute and cancelMute method , Set setVolume(0) also can mute method , is mute method is layer , can can . setVolume(0)only is Set to 0 , .
     * @param volume to 0 , full ; to 1 , , value
     @example
     jessibuca.setVolume(0.2)

     jessibuca.setVolume(0)

     jessibuca.setVolume(1)
     */
    setVolume(volume: number): void;

    /**
     * whether Load
     @example
     var result = jessibuca.hasLoaded()
     console.log(result) // true
     */
    hasLoaded(): boolean;

    /**
     * , in , canvas will video .
     * H5 before in chrome\edge 84, android chrome 84 API, need to is httpspage
     * to , to , all
     @example
     jessibuca.setKeepScreenOn()
     */
    setKeepScreenOn(): boolean;

    /**
     * full ( full )
     @example
     jessibuca.setFullscreen(true)
     //
     jessibuca.setFullscreen(false)
     */
    setFullscreen(flag: boolean): void;

    /**
     *
     * snapshot, after snapshot
     * @param filename parameter, , ` `
     * @param format parameter, snapshot , png jpeg webp , `png`
     * @param quality parameter, is jpeg webp , , value 0 ~ 1 , `0.92`
     * @param type parameter, download base64 blob, `download`

     @example

     jessibuca.screenshot("test","png",0.5)

     const base64 = jessibuca.screenshot("test","png",0.5,'base64')

     const fileBlob = jessibuca.screenshot("test",'blob')
     */
    screenshot(filename?: string, format?: string, quality?: number, type?: string): void;

    /**
     * start .
     * @param fileName ,
     * @param fileType , webm, webm and mp4

     @example
     jessibuca.startRecord('xxx','webm')
     */
    startRecord(fileName: string, fileType: string): void;

    /**
     * .
     @example
     jessibuca.stopRecordAndSave()
     */
    stopRecordAndSave(): void;

    /**
     * whether in in .
     @example
     var result = jessibuca.isPlaying()
     console.log(result) // true
     */
    isPlaying(): boolean;

    /**
     * whether .
     @example
     var result = jessibuca.isMute()
     console.log(result) // true
     */
    isMute(): boolean;

    /**
     * whether in .
     @example
     var result = jessibuca.isRecording()
     console.log(result) // true
     */
    isRecording(): boolean;


    /**
     * jessibuca Initialize event
     * @example
     * jessibuca.on("load",function(){console.log('load')})
     */
    on(event: 'load', callback: () => void): void;

    /**
     * , ms
     * @example
     * jessibuca.on('timeUpdate',function (ts) {console.log('timeUpdate',ts);})
     */
    on(event: 'timeUpdate', callback: () => void): void;

    /**
     * Parse info , 2 parameter
     * @example
     * jessibuca.on("videoInfo",function(data){console.log('width:',data.width,'height:',data.width)})
     */
    on(event: 'videoInfo', callback: (data: {
        /* * */
        width: number;
        /* * */
        height: number;
    }) => void): void;

    /**
     * Parse info , 2 parameter
     * @example
     * jessibuca.on("audioInfo",function(data){console.log('numOfChannels:',data.numOfChannels,'sampleRate',data.sampleRate)})
     */
    on(event: 'audioInfo', callback: (data: {
        /* * channel */
        numOfChannels: number;
        /* * */
        sampleRate: number;
    }) => void): void;

    /**
     * info, info
     * @example
     * jessibuca.on("log",function(data){console.log('data:',data)})
     */
    on(event: 'log', callback: () => void): void;

    /**
     * info
     * @example
     * jessibuca.on("error",function(error){
        if(error === Jessibuca.ERROR.fetchError){
            //
        }
        else if(error === Jessibuca.ERROR.webcodecsH265NotSupport){
            //
        }
        console.log('error:',error)
    })
     */
    on(event: 'error', callback: (err: Jessibuca.ERROR) => void): void;

    /**
     * current , KB 1 ,
     * @example
     * jessibuca.on("kBps",function(data){console.log('kBps:',data)})
     */
    on(event: 'kBps', callback: (value: number) => void): void;

    /**
     * start
     * @example
     * jessibuca.on("start",function(){console.log('start render')})
     */
    on(event: 'start', callback: () => void): void;

    /**
     * data ,
     * @example
     * jessibuca.on("timeout",function(error){console.log('timeout:',error)})
     */
    on(event: 'timeout', callback: (error: Jessibuca.TIMEOUT) => void): void;

    /**
     * play() , if data ,
     * @example
     * jessibuca.on("loadingTimeout",function(){console.log('timeout')})
     */
    on(event: 'loadingTimeout', callback: () => void): void;

    /**
     * in , if timeout after data , .
     * @example
     * jessibuca.on("delayTimeout",function(){console.log('timeout')})
     */
    on(event: 'delayTimeout', callback: () => void): void;

    /**
     * current whether full
     * @example
     * jessibuca.on("fullscreen",function(flag){console.log('is fullscreen',flag)})
     */
    on(event: 'fullscreen', callback: () => void): void;

    /**
     * event
     * @example
     * jessibuca.on("play",function(flag){console.log('play')})
     */
    on(event: 'play', callback: () => void): void;

    /**
     * event
     * @example
     * jessibuca.on("pause",function(flag){console.log('pause')})
     */
    on(event: 'pause', callback: () => void): void;

    /**
     * event, boolean value
     * @example
     * jessibuca.on("mute",function(flag){console.log('is mute',flag)})
     */
    on(event: 'mute', callback: () => void): void;

    /**
     * , start after , 1 .
     * @example
     * jessibuca.on("stats",function(s){console.log("stats is",s)})
     */
    on(event: 'stats', callback: (stats: {
        /* * current , */
        buf: number;
        /* * current */
        fps: number;
        /* * current , byte */
        abps: number;
        /* * current , byte */
        vbps: number;
        /* * current pts, */
        ts: number;
    }) => void): void;

    /**
     * can , start after , 1 .
     * @param performance 0: ,1: ,2: non- workflow
     * @example
     * jessibuca.on("performance",function(performance){console.log("performance is",performance)})
     */
    on(event: 'performance', callback: (performance: 0 | 1 | 2) => void): void;

    /**
     * start event

     * @example
     * jessibuca.on("recordStart",function(){console.log("record start")})
     */
    on(event: 'recordStart', callback: () => void): void;

    /**
     * finish event

     * @example
     * jessibuca.on("recordEnd",function(){console.log("record end")})
     */
    on(event: 'recordEnd', callback: () => void): void;

    /**
     * , , 1s

     * @example
     * jessibuca.on("recordingTimestamp",function(timestamp){console.log("recordingTimestamp is",timestamp)})
     */
    on(event: 'recordingTimestamp', callback: (timestamp: number) => void): void;

    /**
     * play method Initialize -> -> -> ->
     * @param event
     * @param callback
     */
    on(event: 'playToRenderTimes', callback: (times: {
        playInitStart: number, // 1 Initialize
        playStart: number, // 2 Initialize
        streamStart: number, // 3
        streamResponse: number, // 4
        demuxStart: number, // 5
        decodeStart: number, // 6
        videoStart: number, // 7
        playTimestamp: number,// playStart- playInitStart
        streamTimestamp: number,// streamStart - playStart
        streamResponseTimestamp: number,// streamResponse - streamStart
        demuxTimestamp: number, // demuxStart - streamResponse
        decodeTimestamp: number, // decodeStart - demuxStart
        videoTimestamp: number,// videoStart - decodeStart
        allTimestamp: number // videoStart - playInitStart
    }) => void): void

    /**
     * method
     *
     @example

     jessibuca.on("load",function(){console.log('load')})
     */
    on(event: string, callback: Function): void;

}

export default Jessibuca;
