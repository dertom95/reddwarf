/*
 * Copyright (c) 2007-2008, Sun Microsystems, Inc.
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

package com.sun.sgs.tutorial.client.lesson2;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.nio.ByteBuffer;
import java.util.concurrent.atomic.AtomicInteger;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JPanel;

import com.sun.sgs.tutorial.client.lesson1.HelloUserClient;

/**
 * A simple GUI client that interacts with an SGS server-side app using
 * both direct messaging and channel broadcasts.
 * <p>
 * It presents a basic chat interface with an output area and input
 * field, and adds a channel selector to allow the user to choose which
 * method is used for sending data.
 *
 * @see HelloUserClient for a description of the properties understood
 *      by this client.
 */
public class HelloChannelClient extends HelloUserClient
{
    /** The version of the serialized form of this class. */
    private static final long serialVersionUID = 1L;

    /**
     * Default channel names at login.
     * 
     * <b>Note:</b> these must correspond to the channel names used
     * by the lesson 6 server tutorial.
     */
    private static final String[] startingChannelNames = new String[] {
        "Foo", "Bar"
    };

    /** The UI selector among direct messaging and different channels. */
    protected JComboBox channelSelector;

    /** The data model for the channel selector. */
    protected DefaultComboBoxModel channelSelectorModel;

    /** Sequence generator for counting channels. */
    protected final AtomicInteger channelNumberSequence =
        new AtomicInteger(1);

    // Main

    /**
     * Runs an instance of this client.
     *
     * @param args the command-line arguments (unused)
     */
    public static void main(String[] args) {
        new HelloChannelClient().login();
    }

    // HelloChannelClient methods

    /**
     * Creates a new client UI.
     */
    public HelloChannelClient() {
        super(HelloChannelClient.class.getSimpleName());
    }

    /**
     * {@inheritDoc}
     * <p>
     * This implementation adds a channel selector component next
     * to the input text field to allow users to choose between
     * direct-to-server messages and channel broadcasts.
     */
    @Override
    protected void populateInputPanel(JPanel panel) {
        super.populateInputPanel(panel);

        channelSelectorModel = new DefaultComboBoxModel();
        channelSelectorModel.addElement("<DIRECT>");

        for (String channelName : startingChannelNames) {
            channelSelectorModel.addElement(channelName);
        }

        channelSelector = new JComboBox(channelSelectorModel);
        channelSelector.setFocusable(false);
        panel.add(channelSelector, BorderLayout.WEST);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void receivedMessage(ByteBuffer message) {
        super.receivedMessage(message);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void actionPerformed(ActionEvent event) {
        if (! simpleClient.isConnected())
            return;

        String text = getInputText();
        String channelName =
            (String) channelSelector.getSelectedItem();
        if (channelName.equalsIgnoreCase("<DIRECT>")) {
            send("* " + text);
        } else {
            send(channelName + " " + text);
        }
    }
}
