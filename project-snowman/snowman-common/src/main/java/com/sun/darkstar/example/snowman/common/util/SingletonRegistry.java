/*
 * Copyright (c) 2008, Sun Microsystems, Inc.
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in
 *       the documentation and/or other materials provided with the
 *       distribution.
 *     * Neither the name of Sun Microsystems, Inc. nor the names of its
 *       contributors may be used to endorse or promote products derived
 *       from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 * A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 * THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.sun.darkstar.example.snowman.common.util;

import com.sun.darkstar.example.snowman.common.protocol.handlers.MessageHandler;
import com.sun.darkstar.example.snowman.common.protocol.handlers.MessageHandlerImpl;



/**
 * The SingletonRegistry is a Singleton itself that should be a single
 * point of access for all Singletons.  It is introduced to increase 
 * testability of code that depends on Singleton objects.
 * 
 * @author Owen Kellett
 */
public class SingletonRegistry 
{
    private static DataImporter dataImporter;
    private static CollisionManager collisionManager;
    private static IHPConverter hpConverter;
    private static MessageHandler messageHandler;

    public static IHPConverter getHPConverter() {
        if (hpConverter == null) {
            hpConverter = HPConverter.getInstance();
        }
        return hpConverter;
    }

    public static DataImporter getDataImporter() {
        if (dataImporter == null) {
            dataImporter = DataImporterImpl.getInstance();
        }
        return dataImporter;
    }
    public static CollisionManager getCollisionManager() {
        if (collisionManager == null) {
            collisionManager = CollisionManagerImpl.getInstance();
        }
        return collisionManager;
    }
    public static MessageHandler getMessageHandler() {
        if (messageHandler == null) {
            messageHandler = MessageHandlerImpl.getInstance();
        }
        return messageHandler;
    }

    public static void setDataImporter(DataImporter dataImporter) {
        SingletonRegistry.dataImporter = dataImporter;
    }
    public static void setCollisionManager(CollisionManager collisionManager) {
        SingletonRegistry.collisionManager = collisionManager;
    }
    public static void setHPConverter(IHPConverter hpConverter) {
        SingletonRegistry.hpConverter = hpConverter;
    }
    public static void setMessageHandler(MessageHandler messageHandler) {
        SingletonRegistry.messageHandler = messageHandler;
    }

}
