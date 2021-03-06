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

package com.sun.darkstar.example.tool;

import java.awt.Component;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.JToggleButton;

@SuppressWarnings("unchecked")
public class EnumButtonBar<K extends Enum<K>> extends JPanel implements ActionListener {

	private static final long serialVersionUID = 1L;

	Class enumClassName;
	List<EnumButtonBarListener> listeners = new ArrayList<EnumButtonBarListener>();
	public EnumButtonBar(K ... buttons) {
		enumClassName = buttons[0].getClass();
		setLayout(new GridLayout(1,0));
		for(Enum e : buttons){
			addButton(e);
		}
		JToggleButton first = (JToggleButton)getComponent(0);
		first.setSelected(true);
	}

	private void addButton(Enum e) {
		JToggleButton button = new JToggleButton(e.name());
		button.addActionListener(this);
		add(button);
	}

	@Override
	public void actionPerformed(ActionEvent e) {
		JToggleButton buttonPressed = (JToggleButton) e.getSource(); 
		for (int i=0; i< getComponentCount();i++){
			Component c = getComponent(i);
			if (c instanceof JToggleButton){
				if (!buttonPressed.equals(c)){
					((JToggleButton)c).setSelected(false);
				}
			}
		}
		fireEnum(Enum.valueOf(enumClassName, buttonPressed.getText()));	
	}

	private void fireEnum(Enum actualEnum) {
		for(EnumButtonBarListener l : listeners){
			l.enumSet(actualEnum);
		}
		
	}
	
	public void addListener(EnumButtonBarListener l){
		listeners.add(l);
	}
	

}
