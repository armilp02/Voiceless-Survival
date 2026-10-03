package com.armilp.ezvcsurvival.voicechat.plasmo;

import su.plo.voice.api.audio.codec.AudioDecoder;

public class DecoderHolder {

    public final AudioDecoder decoder;
    public boolean closed;

    public DecoderHolder(AudioDecoder decoder) {
        this.decoder = decoder;
    }

    public synchronized short[] decode(byte[] data) throws Exception {
        if (closed) {
            return null;
        }
        return decoder.decode(data);
    }

    public synchronized void close() {
        if (closed) {
            return;
        }
        closed = true;
        decoder.close();
    }
}
