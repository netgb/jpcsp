/*
This file is part of jpcsp.

Jpcsp is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

Jpcsp is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with Jpcsp.  If not, see <http://www.gnu.org/licenses/>.
 */
package jpcsp.GUI;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.SwingConstants;

import jpcsp.Emulator;
import jpcsp.HLE.Modules;
import jpcsp.HLE.modules.sceUtility;
import jpcsp.network.INetworkAdapter;
import jpcsp.settings.Settings;

import org.apache.log4j.Logger;

/**
 * @author gid15
 *
 * Network Chat User Interface
 */
public class ChatGUI extends JFrame {
	private static final long serialVersionUID = 5376146560681704272L;
	private Logger log = Emulator.log;
	private JScrollPane scrollPane;
	private JLabel chatMessagesLabel;
	private JTextField chatMessage;
	private JButton sendButton;
	private List<String> chatMessages = new LinkedList<String>();
	private static final String settingsName = "chat";
	private static final String chatMessageHeader = "<html>";
	private static final String chatMessageFooter = "</html>";
	private HashMap<String, Color> nickNameColors = new HashMap<String, Color>();
	private int allColorsIndex = 0;
	// Always assign the GRAY color to me.
	private static final Color colorForMe = Color.GRAY;
	// The Nicknames will be assigned these colors (first come, first served)
	private static final Color[] allColors = new Color[] {
		Color.BLUE,
		Color.RED,
		Color.CYAN,
		Color.GREEN,
		Color.MAGENTA,
		Color.ORANGE,
		Color.PINK,
		Color.YELLOW,
		Color.BLACK
	};

	public ChatGUI() {
		initComponents();

		setSize(Settings.getInstance().readWindowSize(settingsName, getWidth(), getHeight()));
		setLocation(Settings.getInstance().readWindowPos(settingsName));
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);

		nickNameColors.put(sceUtility.getSystemParamNickname(), colorForMe);
	}

	private void initComponents() {
		scrollPane = new JScrollPane();
		chatMessagesLabel = new JLabel();
		chatMessage = new JTextField();
		sendButton = new JButton();

		setTitle("Chat");
		setResizable(true);

		sendButton.setText("Send");
		sendButton.setDefaultCapable(true);
		getRootPane().setDefaultButton(sendButton);
		sendButton.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent arg0) {
				onSend();
			}
		});

		// Start displaying the chat message from the bottom
		chatMessagesLabel.setVerticalAlignment(SwingConstants.BOTTOM);
		chatMessagesLabel.setPreferredSize(new Dimension(500, 300));

		scrollPane.setViewportView(chatMessagesLabel);

		chatMessage.setEditable(true);

		GroupLayout layout = new GroupLayout(getContentPane());
		getContentPane().setLayout(layout);
		layout.setHorizontalGroup(layout.createParallelGroup()
				.addComponent(scrollPane)
				.addGroup(layout.createSequentialGroup()
						.addComponent(chatMessage)
						.addComponent(sendButton)
						));
		layout.setVerticalGroup(layout.createSequentialGroup()
				.addComponent(scrollPane)
				.addGroup(layout.createParallelGroup()
						.addComponent(chatMessage)
						.addComponent(sendButton))
				);
		pack();
	}

	private void onSend() {
		String message = chatMessage.getText();
		if (log.isDebugEnabled()) {
			log.debug(String.format("Sending chat message '%s'", message));
		}

		// Send the chat message to the network adapter
		INetworkAdapter networkAdapter = Modules.sceNetModule.getNetworkAdapter();
		if (networkAdapter != null) {
			networkAdapter.sendChatMessage(message);
			chatMessage.setText("");

			// Add my own chat to the messages
			addChatMessage(sceUtility.getSystemParamNickname(), message, true);
		}
	}

    private void addChatLine(String line) {
    	chatMessages.add(line);

    	StringBuilder formattedText = new StringBuilder();
    	formattedText.append(chatMessageHeader);
    	for (String chatMessage : chatMessages) {
    		formattedText.append(String.format("<br>%s</br>\n", chatMessage));
    	}
    	formattedText.append(chatMessageFooter);

    	chatMessagesLabel.setText(formattedText.toString());
    }

    private Color getNewColor() {
    	if (allColorsIndex >= allColors.length) {
    		allColorsIndex = 0;
    	}
    	return allColors[allColorsIndex++];
    }

    private Color getNickNameColor(String nickName) {
    	Color color = nickNameColors.get(nickName);
    	if (color == null) {
    		// No color yet assigned to the nickName, assign a new one
        	color = getNewColor();
        	nickNameColors.put(nickName, color);
    	}

    	return color;
    }

    public void addChatMessage(String nickName, String message) {
    	addChatMessage(nickName, message, false);
    }

    private void addChatMessage(String nickName, String message, boolean isMe) {
		String line;

		if (nickName == null) {
			line = message;
		} else {
			Color nickNameColor = getNickNameColor(nickName);
			String nickNameSuffix = isMe ? " (me)" : "";
			line = String.format("<font color='#%06X'>%s%s</font> - %s", nickNameColor.getRGB() & 0x00FFFFFF, nickName, nickNameSuffix, message);
		}

		addChatLine(line);
	}

	@Override
	public void dispose() {
		Settings.getInstance().writeWindowPos(settingsName, getLocation());
		Settings.getInstance().writeWindowSize(settingsName, getSize());

		Emulator.getMainGUI().endWindowDialog();
		super.dispose();
	}
}
