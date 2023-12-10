package com.zyq.chirp.common.redis.command;

import io.lettuce.core.protocol.ProtocolKeyword;

import java.nio.charset.StandardCharsets;

public enum BloomCommand implements ProtocolKeyword {
    BF_ADD("BF.ADD"),
    BF_EXISTS("BF.EXISTS"),
    BF_MADD("BF.MADD"),
    BF_INSERT("BF.INSERT"),
    BF_RESERVE("BF.RESERVE"),
    BF_MEXISTS("BF.MEXISTS"),
    BF_SCANDUMP("BF.SCANDUMP"),
    BF_LOADCHUNK("BF.LOADCHUNK"),
    BF_INFO("BF.INFO");
    public final byte[] bytes;

    BloomCommand(String command) {
        bytes = command.getBytes(StandardCharsets.US_ASCII);
    }


    @Override
    public byte[] getBytes() {
        return bytes;
    }
}
