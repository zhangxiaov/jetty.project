//
//  ========================================================================
//  Copyright (c) 1995-2013 Mort Bay Consulting Pty. Ltd.
//  ------------------------------------------------------------------------
//  All rights reserved. This program and the accompanying materials
//  are made available under the terms of the Eclipse Public License v1.0
//  and Apache License v2.0 which accompanies this distribution.
//
//      The Eclipse Public License is available at
//      http://www.eclipse.org/legal/epl-v10.html
//
//      The Apache License v2.0 is available at
//      http://www.opensource.org/licenses/apache2.0.php
//
//  You may elect to redistribute this code under either of these licenses.
//  ========================================================================
//

package org.eclipse.jetty.websocket.common;

import static org.hamcrest.Matchers.*;

import java.nio.ByteBuffer;

import org.eclipse.jetty.io.ByteBufferPool;
import org.eclipse.jetty.io.MappedByteBufferPool;
import org.eclipse.jetty.util.BufferUtil;
import org.eclipse.jetty.websocket.api.StatusCode;
import org.eclipse.jetty.websocket.api.WebSocketPolicy;
import org.eclipse.jetty.websocket.api.extensions.Frame;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

public class WebSocketFrameTest
{
    private static Generator strictGenerator;
    private static Generator laxGenerator;

    private ByteBuffer generateWholeFrame(Generator generator, Frame frame)
    {
        ByteBuffer buf = ByteBuffer.allocate(frame.getPayloadLength() + Generator.OVERHEAD);
        generator.generateWholeFrame(frame,buf);
        BufferUtil.flipToFlush(buf,0);
        return buf;
    }

    @BeforeClass
    public static void initGenerator()
    {
        WebSocketPolicy policy = WebSocketPolicy.newServerPolicy();
        ByteBufferPool bufferPool = new MappedByteBufferPool();
        strictGenerator = new Generator(policy,bufferPool);
        laxGenerator = new Generator(policy,bufferPool,false);
    }

    private void assertEqual(String message, ByteBuffer expected, ByteBuffer actual)
    {
        BufferUtil.flipToFlush(expected,0);

        ByteBufferAssert.assertEquals(message,expected,actual);
    }

    private void assertFrameHex(String message, String expectedHex, ByteBuffer actual)
    {
        String actualHex = Hex.asHex(actual);
        Assert.assertThat("Generated Frame:" + message,actualHex,is(expectedHex));
    }

    @Test
    public void testLaxInvalidClose()
    {
        WebSocketFrame frame = new WebSocketFrame(OpCode.CLOSE).setFin(false);
        ByteBuffer actual = generateWholeFrame(laxGenerator,frame);
        String expected = "0800";
        assertFrameHex("Lax Invalid Close Frame",expected,actual);
    }

    @Test
    public void testLaxInvalidPing()
    {
        WebSocketFrame frame = new WebSocketFrame(OpCode.PING).setFin(false);
        ByteBuffer actual = generateWholeFrame(laxGenerator,frame);
        String expected = "0900";
        assertFrameHex("Lax Invalid Ping Frame",expected,actual);
    }

    @Test
    public void testStrictValidClose()
    {
        CloseInfo close = new CloseInfo(StatusCode.NORMAL);
        ByteBuffer actual = generateWholeFrame(strictGenerator,close.asFrame());
        String expected = "880203E8";
        assertFrameHex("Strict Valid Close Frame",expected,actual);
    }

    @Test
    public void testStrictValidPing()
    {
        WebSocketFrame frame = new WebSocketFrame(OpCode.PING);
        ByteBuffer actual = generateWholeFrame(strictGenerator,frame);
        String expected = "8900";
        assertFrameHex("Strict Valid Ping Frame",expected,actual);
    }
    
    @Test
    public void testRsv1()
    {
        WebSocketFrame frame = new WebSocketFrame(OpCode.TEXT);
        frame.setPayload("Hi");
        frame.setRsv1(true);
        ByteBuffer actual = generateWholeFrame(laxGenerator,frame);
        String expected = "C1024869";
        assertFrameHex("Lax Text Frame with RSV1",expected,actual);
    }
    
    @Test
    public void testRsv2()
    {
        WebSocketFrame frame = new WebSocketFrame(OpCode.TEXT);
        frame.setPayload("Hi");
        frame.setRsv2(true);
        ByteBuffer actual = generateWholeFrame(laxGenerator,frame);
        String expected = "A1024869";
        assertFrameHex("Lax Text Frame with RSV2",expected,actual);
    }
    
    @Test
    public void testRsv3()
    {
        WebSocketFrame frame = new WebSocketFrame(OpCode.TEXT);
        frame.setPayload("Hi");
        frame.setRsv3(true);
        ByteBuffer actual = generateWholeFrame(laxGenerator,frame);
        String expected = "91024869";
        assertFrameHex("Lax Text Frame with RSV3",expected,actual);
    }
}