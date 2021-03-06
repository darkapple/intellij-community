/*
 * Copyright 2000-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.ui;

import com.intellij.ide.DataManager;
import com.intellij.openapi.actionSystem.ActionManager;
import com.intellij.openapi.actionSystem.ActionPlaces;
import com.intellij.openapi.actionSystem.AnAction;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.editor.colors.EditorColors;
import com.intellij.openapi.editor.colors.EditorColorsManager;
import com.intellij.util.ui.PlatformColors;
import com.intellij.util.ui.UIUtil;
import org.jetbrains.annotations.NonNls;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;

/**
 * @author Dmitry Avdeev
 */
public class EditorNotificationPanel extends JPanel {
  protected final JLabel myLabel = new JLabel();
  protected final JPanel myLinksPanel;

  public EditorNotificationPanel() {
    super(new BorderLayout());
    setBorder(BorderFactory.createEmptyBorder(1, 15, 1, 15));

    setPreferredSize(new Dimension(-1, 24));

    add(myLabel, BorderLayout.CENTER);

    myLinksPanel = new JPanel(new FlowLayout());
    myLinksPanel.setBackground(getBackground());
    add(myLinksPanel, BorderLayout.EAST);
  }

  public void setText(String text) {
    myLabel.setText(text);
  }

  @Override
  public Color getBackground() {
    Color color = EditorColorsManager.getInstance().getGlobalScheme().getColor(EditorColors.NOTIFICATION_BACKGROUND);
    return color == null ? UIUtil.getToolTipBackground() : color;
  }

  public HyperlinkLabel createActionLabel(final String text, @NonNls final String actionId) {
    return createActionLabel(text, new Runnable() {
      @Override
      public void run() {
        executeAction(actionId);
      }
    });
  }

  public HyperlinkLabel createActionLabel(final String text, final Runnable action) {
    HyperlinkLabel label = new HyperlinkLabel(text, PlatformColors.BLUE, getBackground(), PlatformColors.BLUE);
    label.addHyperlinkListener(new HyperlinkAdapter() {
      @Override
      protected void hyperlinkActivated(HyperlinkEvent e) {
        action.run();
      }
    });
    myLinksPanel.add(label);
    return label;
  }

  protected void executeAction(final String actionId) {
    final AnAction action = ActionManager.getInstance().getAction(actionId);
    final AnActionEvent event = new AnActionEvent(null, DataManager.getInstance().getDataContext(this), ActionPlaces.UNKNOWN,
                                                  action.getTemplatePresentation(), ActionManager.getInstance(),
                                                  0);
    action.beforeActionPerformedUpdate(event);
    action.update(event);

    if (event.getPresentation().isEnabled() && event.getPresentation().isVisible()) {
      action.actionPerformed(event);
    }
  }

  @Override
  public Dimension getMinimumSize() {
    return new Dimension(0, 0);
  }
}
